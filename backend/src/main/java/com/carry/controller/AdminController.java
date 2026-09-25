package com.carry.controller;

import com.carry.dto.ComplaintResponseDto;
import com.carry.dto.ProductRequestResponseDto;
import com.carry.dto.UpdateComplaintStatusDto;
import com.carry.dto.UserDto;
import com.carry.entity.RequestStatus;
import com.carry.security.UserDetailsImpl;
import com.carry.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/complaints")
    public ResponseEntity<List<ComplaintResponseDto>> getComplaints(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.getAllComplaints(currentUser));
    }

    @PutMapping("/complaints/{id}")
    public ResponseEntity<ComplaintResponseDto> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComplaintStatusDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.updateComplaint(id, dto, currentUser));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.getAllUsers(currentUser));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<ProductRequestResponseDto>> getRequests(
            @RequestParam(required = false) RequestStatus status,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(adminService.getAllRequests(status, currentUser));
    }
}
