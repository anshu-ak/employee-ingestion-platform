package com.employee.employeeingestionplatform.controller;

import com.employee.employeeingestionplatform.dto.auth.LoginRequest;
import com.employee.employeeingestionplatform.dto.auth.LoginResponse;
import com.employee.employeeingestionplatform.entity.ApplicationUser;
import com.employee.employeeingestionplatform.repository.ApplicationUserRepository;
import com.employee.employeeingestionplatform.service.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final ApplicationUserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            ApplicationUserRepository userRepository,
            JwtTokenService jwtTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        ApplicationUser user = userRepository
                .findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"
                ));

        return new LoginResponse(
                jwtTokenService.generateToken(user),
                "Bearer",
                jwtTokenService.getExpirySeconds()
        );
    }
}