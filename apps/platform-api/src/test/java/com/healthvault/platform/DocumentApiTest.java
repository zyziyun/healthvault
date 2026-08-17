package com.healthvault.platform;

import com.healthvault.platform.security.JwtAuthFilter;
import com.healthvault.platform.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end checks over the real filter chain: auth, the JWT gate, upload validation, and the
 * scoped document reads. The cross-user 403 test is intentionally left as homework.
 */
@SpringBootTest
class DocumentApiTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JwtService jwt;

    MockMvc mvc() {
        // Register the JWT filter on the same protected patterns as production so it actually runs.
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilter(new JwtAuthFilter(jwt), "/auth/me", "/documents", "/documents/*")
                .build();
    }

    private static String extract(String json, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(json);
        if (!m.find()) {
            throw new AssertionError("pattern " + pattern + " not found in " + json);
        }
        return m.group(1);
    }

    private String registerAndLogin(String email) throws Exception {
        MockMvc mvc = mvc();
        String body = "{\"email\":\"" + email + "\",\"password\":\"password123\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        String res = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extract(res, "\"token\":\"([^\"]+)\"");
    }

    private static MockMultipartFile png(String field) {
        // PNG magic bytes so content sniffing accepts it regardless of the declared type.
        byte[] bytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
        return new MockMultipartFile(field, "scan.png", "image/png", bytes);
    }

    @Test
    void protectedRouteRejectsMissingToken() throws Exception {
        mvc().perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsAuthenticatedUser() throws Exception {
        String token = registerAndLogin("me@example.com");
        mvc().perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }

    @Test
    void uploadThenListAndGetOwnDocument() throws Exception {
        String token = registerAndLogin("owner@example.com");
        MockMvc mvc = mvc();

        String uploadRes = mvc.perform(multipart("/documents").file(png("file"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"))
                .andReturn().getResponse().getContentAsString();
        String id = extract(uploadRes, "\"id\":(\\d+)");

        mvc.perform(get("/documents").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Integer.parseInt(id)));

        mvc.perform(get("/documents/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void uploadRejectsDisguisedNonImage() throws Exception {
        String token = registerAndLogin("baddata@example.com");
        // Declares image/png but the bytes are plain text: content sniffing must reject it.
        MockMultipartFile fake = new MockMultipartFile(
                "file", "evil.png", "image/png", "#!/bin/sh\nrm -rf /".getBytes());
        mvc().perform(multipart("/documents").file(fake).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("bad_request"));
    }
}
