package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.entity.ApplicationUser;
import com.employee.employeeingestionplatform.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    private static final long EXPIRY_MINUTES = 60L;

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(
                jwtEncoder,
                EXPIRY_MINUTES
        );
    }

    @Test
    void shouldGenerateTokenWithExpectedClaims() {
        ApplicationUser user = new ApplicationUser();
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);

        Jwt encodedJwt = mock(Jwt.class);

        when(encodedJwt.getTokenValue())
                .thenReturn("generated.jwt.token");

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(encodedJwt);

        String token = jwtTokenService.generateToken(user);

        assertEquals("generated.jwt.token", token);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(
                        JwtEncoderParameters.class
                );

        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims =
                captor.getValue().getClaims();

        assertAll(
                () -> assertEquals(
                        "employee-ingestion-platform",
                        claims.getClaim("iss")
                ),
                () -> assertEquals(
                        "admin",
                        claims.getSubject()
                ),
                () -> assertEquals(
                        List.of("ADMIN"),
                        claims.getClaim("roles")
                ),
                () -> assertNotNull(claims.getIssuedAt()),
                () -> assertNotNull(claims.getExpiresAt())
        );

        Duration tokenLifetime = Duration.between(
                claims.getIssuedAt(),
                claims.getExpiresAt()
        );

        assertEquals(
                Duration.ofMinutes(EXPIRY_MINUTES),
                tokenLifetime
        );
    }

    @Test
    void shouldGenerateTokenUsingUsersActualRole() {
        ApplicationUser user = new ApplicationUser();
        user.setUsername("regular-user");
        user.setRole(UserRole.USER);

        Jwt encodedJwt = mock(Jwt.class);

        when(encodedJwt.getTokenValue())
                .thenReturn("user.jwt.token");

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(encodedJwt);

        String token = jwtTokenService.generateToken(user);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(
                        JwtEncoderParameters.class
                );

        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims =
                captor.getValue().getClaims();

        assertAll(
                () -> assertEquals("user.jwt.token", token),
                () -> assertEquals(
                        "regular-user",
                        claims.getSubject()
                ),
                () -> assertEquals(
                        List.of("USER"),
                        claims.getClaim("roles")
                )
        );
    }

    @Test
    void shouldConvertExpiryMinutesToSeconds() {
        assertEquals(
                3_600L,
                jwtTokenService.getExpirySeconds()
        );
    }

    @Test
    void shouldSupportDifferentExpiryConfiguration() {
        JwtTokenService ninetyMinuteService =
                new JwtTokenService(jwtEncoder, 90L);

        assertEquals(
                5_400L,
                ninetyMinuteService.getExpirySeconds()
        );
    }
}