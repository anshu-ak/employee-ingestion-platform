package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.entity.ApplicationUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final long expiryMinutes;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiry-minutes}") long expiryMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expiryMinutes = expiryMinutes;
    }

    public String generateToken(ApplicationUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
                expiryMinutes,
                ChronoUnit.MINUTES
        );

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("employee-ingestion-platform")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(claims)
        ).getTokenValue();
    }

    public long getExpirySeconds() {
        return expiryMinutes * 60;
    }
}