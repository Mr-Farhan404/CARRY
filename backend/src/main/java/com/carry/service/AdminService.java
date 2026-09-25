package com.carry.service;

import com.carry.dto.ComplaintResponseDto;
import com.carry.dto.ProductRequestResponseDto;
import com.carry.dto.UpdateComplaintStatusDto;
import com.carry.dto.UserDto;
import com.carry.entity.Complaint;
import com.carry.entity.ComplaintStatus;
import com.carry.entity.ProductRequest;
import com.carry.entity.RequestStatus;
import com.carry.exception.ForbiddenException;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.ComplaintRepository;
import com.carry.repository.ProductRequestRepository;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ProductRequestRepository productRequestRepository;

    /**
     * Reusable admin verification check used across all admin operations.
     */
    public void verifyAdmin(UserDetailsImpl currentUser) {
        if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
            throw new ForbiddenException("Access denied: Admin privileges required");
        }
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDto> getAllComplaints(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ComplaintResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public ComplaintResponseDto updateComplaint(Long id, UpdateComplaintStatusDto dto, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));

        complaint.setStatus(dto.getStatus());
        complaint.setAdminNotes(dto.getAdminNotes());

        if (dto.getStatus() == ComplaintStatus.RESOLVED && complaint.getResolvedAt() == null) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        Complaint updated = complaintRepository.save(complaint);
        return ComplaintResponseDto.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return userRepository.findAll()
                .stream()
                .map(UserDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductRequestResponseDto> getAllRequests(RequestStatus status, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        List<ProductRequest> requests;
        if (status != null) {
            requests = productRequestRepository.findByStatus(status);
        } else {
            requests = productRequestRepository.findAll();
        }

        return requests.stream()
                .map(ProductRequestResponseDto::fromEntity)
                .toList();
    }
}
