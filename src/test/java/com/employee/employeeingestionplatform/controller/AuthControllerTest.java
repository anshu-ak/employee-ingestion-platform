package com.employee.employeeingestionplatform.controller;

import com.employee.employeeingestionplatform.dto.auth.LoginRequest;
import com.employee.employeeingestionplatform.dto.auth.LoginResponse;
import com.employee.employeeingestionplatform.entity.ApplicationUser;
import com.employee.employeeingestionplatform.entity.UserRole;
import com.employee.employeeingestionplatform.repository.ApplicationUserRepository;
import com.employee.employeeingestionplatform.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ApplicationUserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                authenticationManager,
                userRepository,
                jwtTokenService
        );
    }

    @Test
    void shouldReturnJwtForValidCredentials() {
        LoginRequest request = new LoginRequest(
                "admin",
                "Admin@123"
        );

        ApplicationUser user = new ApplicationUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded-password");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null
                )
        );

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));

        when(jwtTokenService.generateToken(user))
                .thenReturn("test-jwt-token");

        when(jwtTokenService.getExpirySeconds())
                .thenReturn(3600L);

        LoginResponse response = authController.login(request);

        assertAll(
                () -> assertEquals(
                        "test-jwt-token",
                        response.accessToken()
                ),
                () -> assertEquals(
                        "Bearer",
                        response.tokenType()
                ),
                () -> assertEquals(
                        3600L,
                        response.expiresInSeconds()
                )
        );

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );

        verify(jwtTokenService).generateToken(user);
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() {
        LoginRequest request = new LoginRequest(
                "admin",
                "wrong-password"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(
                new BadCredentialsException(
                        "Bad credentials"
                )
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authController.login(request)
                );

        assertAll(
                () -> assertEquals(
                        HttpStatus.UNAUTHORIZED,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "Invalid username or password",
                        exception.getReason()
                )
        );

        verify(userRepository, never())
                .findByUsername(any());

        verify(jwtTokenService, never())
                .generateToken(any());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotFoundAfterAuthentication() {
        LoginRequest request = new LoginRequest(
                "missing-user",
                "Password@123"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "missing-user",
                        null
                )
        );

        when(userRepository.findByUsername("missing-user"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authController.login(request)
                );

        assertAll(
                () -> assertEquals(
                        HttpStatus.UNAUTHORIZED,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "Invalid username or password",
                        exception.getReason()
                )
        );

        verify(jwtTokenService, never())
                .generateToken(any());
    }
}