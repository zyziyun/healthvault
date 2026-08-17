package com.healthvault.platform.auth;

import com.healthvault.platform.security.CurrentUser;
import com.healthvault.platform.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Thin HTTP layer over {@link AuthService}. Public routes: register, login, oauth, guest.
 * {@code /auth/me} sits behind the JWT filter and echoes the authenticated identity.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        User u = auth.register(req.email(), req.password());
        return Map.of("id", u.getId(), "email", u.getEmail());
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest req) {
        return Map.of("token", auth.login(req.email(), req.password()));
    }

    // Callback after the provider verified the user. In production this is wired to a Spring
    // Security OAuth2 success handler; here it takes the verified identity directly so the
    // link-or-create logic can be run and tested locally. Returns our own JWT.
    @PostMapping("/oauth/{provider}")
    public Map<String, String> oauth(@PathVariable String provider, @Valid @RequestBody OAuthLoginRequest req) {
        return Map.of("token", auth.oauthLogin(provider, req.providerUserId(), req.email()));
    }

    // Guest mode: browse before signing up. Ephemeral, not persisted.
    @PostMapping("/guest")
    public Map<String, String> guest() {
        return Map.of("token", "guest-" + UUID.randomUUID(), "mode", "guest");
    }

    // Protected: the filter validated the JWT and set the user id.
    @GetMapping("/me")
    public Map<String, Object> me(@CurrentUser Long userId) {
        User u = auth.requireUser(userId);
        return Map.of("id", u.getId(), "email", u.getEmail());
    }
}
