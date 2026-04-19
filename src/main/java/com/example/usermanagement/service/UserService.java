package com.example.usermanagement.service;

import com.example.usermanagement.config.JwtProperties;
import com.example.usermanagement.dto.SigninRequest;
import com.example.usermanagement.dto.SigninResponse;
import com.example.usermanagement.dto.SignupRequest;
import com.example.usermanagement.dto.SignupResponse;
import com.example.usermanagement.exception.EmailAlreadyInUseException;
import com.example.usermanagement.exception.InvalidCredentialsException;
import com.example.usermanagement.model.User;
import com.example.usermanagement.model.UserStatus;
import com.example.usermanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    public SignupResponse register(SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyInUseException("Email is already in use: " + normalizedEmail);
        }
        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .status(UserStatus.ACTIVE)
                .role("USER")
                .build();
        User saved = userRepository.save(user);
        return new SignupResponse(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        );
    }

    public SigninResponse signin(SigninRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        return new SigninResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenExpiry().toSeconds(),
                jwtProperties.getRefreshTokenExpiry().toSeconds()
        );
    }
}


