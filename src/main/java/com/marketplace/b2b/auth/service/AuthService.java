package com.marketplace.b2b.auth.service;

import com.marketplace.b2b.auth.dto.AuthResponse;
import com.marketplace.b2b.auth.dto.LoginRequest;
import com.marketplace.b2b.auth.dto.RegisterRequest;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.integration.event.IntegrationEventPublisher;
import com.marketplace.b2b.security.JwtTokenService;
import com.marketplace.b2b.security.JwtTokenService.IssuedToken;
import com.marketplace.b2b.user.entity.User;
import com.marketplace.b2b.user.mapper.UserMapper;
import com.marketplace.b2b.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;
    private final IntegrationEventPublisher integrationEvents;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED,
                    "El correo " + email + " ya está registrado");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .companyName(request.companyName().trim())
                .taxId(request.taxId().trim().toUpperCase(Locale.ROOT))
                .role(request.accountType().toRole())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        // Se avisa al CRM (vía outbox, en la misma transacción)
        integrationEvents.customerRegistered(saved);

        // No se registra el correo en el log (dato personal); basta con el id
        log.info("Usuario registrado id={} rol={}", saved.getId(), saved.getRole());
        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Mismo mensaje si el correo no existe o la contraseña es incorrecta,
        // para no revelar qué correos están registrados
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                        "Correo o contraseña incorrectos"));

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "La cuenta está deshabilitada");
        }

        log.info("Inicio de sesión exitoso usuarioId={}", user.getId());
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        IssuedToken token = jwtTokenService.generate(user);
        return new AuthResponse(token.value(), TOKEN_TYPE, token.expiresInSeconds(), userMapper.toResponse(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}