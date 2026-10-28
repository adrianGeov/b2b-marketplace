package com.marketplace.b2b.auth.service;

import com.marketplace.b2b.auth.dto.AccountType;
import com.marketplace.b2b.auth.dto.AuthResponse;
import com.marketplace.b2b.auth.dto.LoginRequest;
import com.marketplace.b2b.auth.dto.RegisterRequest;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.security.JwtTokenService;
import com.marketplace.b2b.security.JwtTokenService.IssuedToken;
import com.marketplace.b2b.user.dto.UserResponse;
import com.marketplace.b2b.user.entity.Role;
import com.marketplace.b2b.user.entity.User;
import com.marketplace.b2b.user.mapper.UserMapper;
import com.marketplace.b2b.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final IssuedToken TOKEN = new IssuedToken("jwt-token", 3600);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    // ------------------------------------------------------------------
    // Registro
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Registro exitoso: normaliza datos, cifra la contraseña y devuelve el token")
    void shouldRegisterUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("  Compras@Acme.MX ", "Secreta123",
                "Ana Lopez", "Acme SA de CV", "aaa010101aaa", AccountType.DISTRIBUTOR);
        UserResponse userResponse = new UserResponse(10L, "compras@acme.mx", "Ana Lopez",
                "Acme SA de CV", "AAA010101AAA", Role.DISTRIBUTOR);

        given(userRepository.existsByEmailIgnoreCase("compras@acme.mx")).willReturn(false);
        given(passwordEncoder.encode("Secreta123")).willReturn("hash-bcrypt");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        given(jwtTokenService.generate(any(User.class))).willReturn(TOKEN);
        given(userMapper.toResponse(any(User.class))).willReturn(userResponse);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("compras@acme.mx");
        assertThat(saved.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(saved.getTaxId()).isEqualTo("AAA010101AAA");
        assertThat(saved.getRole()).isEqualTo(Role.DISTRIBUTOR);
        assertThat(saved.isEnabled()).isTrue();

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user()).isEqualTo(userResponse);
    }

    @Test
    @DisplayName("Registro con correo existente lanza EMAIL_ALREADY_REGISTERED y no guarda nada")
    void shouldRejectDuplicatedEmail() {
        RegisterRequest request = new RegisterRequest("compras@acme.mx", "Secreta123",
                "Ana Lopez", "Acme SA de CV", "AAA010101AAA", AccountType.CUSTOMER);
        given(userRepository.existsByEmailIgnoreCase("compras@acme.mx")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Login exitoso con credenciales correctas")
    void shouldLoginWithValidCredentials() {
        User user = activeUser();
        given(userRepository.findByEmailIgnoreCase("compras@acme.mx")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Secreta123", "hash-bcrypt")).willReturn(true);
        given(jwtTokenService.generate(user)).willReturn(TOKEN);

        AuthResponse response = authService.login(new LoginRequest("Compras@Acme.mx", "Secreta123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        verify(jwtTokenService).generate(user);
    }

    @Test
    @DisplayName("Login con contraseña incorrecta lanza INVALID_CREDENTIALS")
    void shouldRejectWrongPassword() {
        User user = activeUser();
        given(userRepository.findByEmailIgnoreCase("compras@acme.mx")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("incorrecta1", "hash-bcrypt")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("compras@acme.mx", "incorrecta1")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(jwtTokenService, never()).generate(any());
    }

    @Test
    @DisplayName("Login con correo inexistente lanza el MISMO error que contraseña incorrecta")
    void shouldRejectUnknownEmailWithSameError() {
        given(userRepository.findByEmailIgnoreCase("nadie@acme.mx")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@acme.mx", "Secreta123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Login de una cuenta deshabilitada lanza ACCESS_DENIED")
    void shouldRejectDisabledAccount() {
        User user = activeUser();
        user.setEnabled(false);
        given(userRepository.findByEmailIgnoreCase("compras@acme.mx")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Secreta123", "hash-bcrypt")).willReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("compras@acme.mx", "Secreta123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    private static User activeUser() {
        return User.builder()
                .id(1L)
                .email("compras@acme.mx")
                .passwordHash("hash-bcrypt")
                .fullName("Ana Lopez")
                .companyName("Acme SA de CV")
                .taxId("AAA010101AAA")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
    }
}