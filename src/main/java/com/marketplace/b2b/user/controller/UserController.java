package com.marketplace.b2b.user.controller;

import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.security.AuthenticatedUser;
import com.marketplace.b2b.user.dto.UserResponse;
import com.marketplace.b2b.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_USERS)
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Mi perfil", description = "Datos del usuario dueño del token.")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(AuthenticatedUser.from(jwt).id());
    }
}