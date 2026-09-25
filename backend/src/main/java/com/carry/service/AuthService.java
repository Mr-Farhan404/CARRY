package com.carry.service;

import com.carry.dto.AuthResponse;
import com.carry.dto.LoginRequest;
import com.carry.dto.RegisterRequest;
import com.carry.entity.User;
import com.carry.exception.ConflictException;
import com.carry.repository.UserRepository;
import com.carry.security.JwtService;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email is already in use");
        }
        if (userRepository.findByStudentId(request.getStudentId()).isPresent()) {
            throw new ConflictException("Student ID is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .studentId(request.getStudentId())
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .department(request.getDepartment())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtService.generateToken(userDetails);

        return new AuthResponse(jwt);
    }
}
