package com.marketplace.b2b.user.controller;

import com.marketplace.b2b.security.AuthenticatedUser;
import com.marketplace.b2b.user.dto.UserResponse;
import com.marketplace.b2b.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Perfil del usuario autenticado (lo usará el front para mostrar nombre y empresa). */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(AuthenticatedUser.from(jwt).id());
    }
}