package com.healthvault.platform.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Represents an identity ALREADY verified by the provider.
// In production these fields come from a verified id_token, not from the client.
public record OAuthLoginRequest(
        @NotBlank String providerUserId,
        @Email @NotBlank String email
) {}
