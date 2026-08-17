package com.healthvault.platform.auth;

import com.healthvault.platform.error.ApiException;
import com.healthvault.platform.security.JwtService;
import com.healthvault.platform.user.OauthAccount;
import com.healthvault.platform.user.OauthAccountRepository;
import com.healthvault.platform.user.User;
import com.healthvault.platform.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * All auth business logic: password hashing, credential checks, the OAuth link-or-create flow,
 * and minting JWTs. Lifted out of the controller so it can be unit-tested without the web layer
 * and so the controller stays a thin HTTP adapter.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final OauthAccountRepository oauthAccounts;
    private final JwtService jwt;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository users, OauthAccountRepository oauthAccounts, JwtService jwt) {
        this.users = users;
        this.oauthAccounts = oauthAccounts;
        this.jwt = jwt;
    }

    @Transactional
    public User register(String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("email already registered");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        return users.save(u);
    }

    @Transactional(readOnly = true)
    public String login(String email, String rawPassword) {
        User u = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "unauthorized", "invalid credentials"));
        // OAuth-only accounts have no local password.
        if (u.getPasswordHash() == null || !encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "unauthorized", "invalid credentials");
        }
        return jwt.issue(u.getId(), u.getEmail());
    }

    /** Link-or-create against an identity the provider already verified, then mint our own JWT. */
    @Transactional
    public String oauthLogin(String provider, String providerUserId, String email) {
        User user = oauthAccounts.findByProviderAndProviderUserId(provider, providerUserId)
                .map(acc -> users.findById(acc.getUserId()).orElseThrow())
                .orElseGet(() -> {
                    User u = users.findByEmail(email).orElseGet(() -> {
                        User created = new User();
                        created.setEmail(email);
                        return users.save(created);
                    });
                    OauthAccount acc = new OauthAccount();
                    acc.setUserId(u.getId());
                    acc.setProvider(provider);
                    acc.setProviderUserId(providerUserId);
                    oauthAccounts.save(acc);
                    return u;
                });
        return jwt.issue(user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("user not found"));
    }
}
