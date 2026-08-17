package com.healthvault.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs before the controllers on protected routes. Reads the "Authorization: Bearer &lt;jwt&gt;"
 * header, verifies it via {@link JwtService}, and stashes the authenticated user id on the
 * request so controllers can read it with {@link CurrentUser}. No valid token, no entry: 401.
 * <p>
 * This is authentication only (who you are). Authorization (whether this user may touch this
 * specific row) is enforced per-resource in the service layer.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = "authUserId";

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "missing bearer token");
            return;
        }
        try {
            long userId = jwt.verify(header.substring("Bearer ".length()).trim());
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
        } catch (JwtService.JwtException e) {
            unauthorized(response, e.getMessage());
            return;
        }
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":{\"code\":\"unauthorized\",\"message\":\"" + reason + "\"}}");
    }
}
