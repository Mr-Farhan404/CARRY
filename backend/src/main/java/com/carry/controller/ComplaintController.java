package com.carry.controller;

import com.carry.dto.ComplaintResponseDto;
import com.carry.dto.CreateComplaintDto;
import com.carry.security.UserDetailsImpl;
import com.carry.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintResponseDto> createComplaint(
            @Valid @RequestBody CreateComplaintDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        ComplaintResponseDto response = complaintService.createComplaint(dto, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
