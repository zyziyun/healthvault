package com.healthvault.platform.auth;

import com.healthvault.platform.user.OauthAccount;
import com.healthvault.platform.user.OauthAccountRepository;
import com.healthvault.platform.user.User;
import com.healthvault.platform.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final OauthAccountRepository oauthAccounts;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository users, OauthAccountRepository oauthAccounts) {
        this.users = users;
        this.oauthAccounts = oauthAccounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        User u = new User();
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);
        return Map.of("id", u.getId(), "email", u.getEmail());
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        // OAuth-only accounts have no local password.
        if (u.getPasswordHash() == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        return Map.of("token", "session-for-user-" + u.getId());
    }

    // Callback after the provider verified the user. In production this is wired to a
    // Spring Security OAuth2 success handler; here it takes the verified identity directly
    // so the account link-or-create logic can be run and tested locally.
    @PostMapping("/oauth/{provider}")
    public Map<String, Object> oauth(@PathVariable String provider, @Valid @RequestBody OAuthLoginRequest req) {
        // 1. known external identity -> return its user
        User user = oauthAccounts.findByProviderAndProviderUserId(provider, req.providerUserId())
                .map(acc -> users.findById(acc.getUserId()).orElseThrow())
                // 2. same email already registered -> link this provider to it
                .orElseGet(() -> {
                    User u = users.findByEmail(req.email()).orElseGet(() -> {
                        // 3. brand new user, no local password
                        User created = new User();
                        created.setEmail(req.email());
                        return users.save(created);
                    });
                    OauthAccount acc = new OauthAccount();
                    acc.setUserId(u.getId());
                    acc.setProvider(provider);
                    acc.setProviderUserId(req.providerUserId());
                    oauthAccounts.save(acc);
                    return u;
                });
        return Map.of("token", "session-for-user-" + user.getId(), "email", user.getEmail());
    }

    // Guest mode: browse before signing up. Ephemeral, not persisted.
    @PostMapping("/guest")
    public Map<String, String> guest() {
        return Map.of("token", "guest-" + UUID.randomUUID(), "mode", "guest");
    }
}
