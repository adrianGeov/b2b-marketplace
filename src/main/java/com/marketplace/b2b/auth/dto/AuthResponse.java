package com.marketplace.b2b.auth.dto;

import com.marketplace.b2b.user.dto.UserResponse;

/**
 * @param expiresIn segundos de vigencia del token
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}