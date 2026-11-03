package com.carry.controller;

import com.carry.dto.ApplyRoleDto;
import com.carry.dto.RoleApplicationResponseDto;
import com.carry.dto.UserDto;
import com.carry.entity.User;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import com.carry.service.RoleApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RoleApplicationService roleApplicationService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    @PostMapping("/apply-role")
    public ResponseEntity<RoleApplicationResponseDto> applyRole(
            @Valid @RequestBody ApplyRoleDto dto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        RoleApplicationResponseDto response = roleApplicationService.applyForRole(dto, userDetails.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<RoleApplicationResponseDto>> getMyApplications(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(roleApplicationService.getMyApplications(userDetails.getId()));
    }
}
