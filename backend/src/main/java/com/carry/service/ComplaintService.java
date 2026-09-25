package com.carry.service;

import com.carry.dto.ComplaintResponseDto;
import com.carry.dto.CreateComplaintDto;
import com.carry.entity.Complaint;
import com.carry.entity.ComplaintStatus;
import com.carry.entity.ProductRequest;
import com.carry.entity.User;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.ComplaintRepository;
import com.carry.repository.ProductRequestRepository;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ProductRequestRepository productRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public ComplaintResponseDto createComplaint(CreateComplaintDto dto, UserDetailsImpl currentUser) {
        User raisedBy = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        ProductRequest request = null;
        if (dto.getRequestId() != null) {
            request = productRequestRepository.findById(dto.getRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + dto.getRequestId()));
        }

        User againstUser = null;
        if (dto.getAgainstUserId() != null) {
            againstUser = userRepository.findById(dto.getAgainstUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getAgainstUserId()));
        }

        Complaint complaint = Complaint.builder()
                .raisedBy(raisedBy)
                .request(request)
                .againstUser(againstUser)
                .description(dto.getDescription())
                .status(ComplaintStatus.OPEN)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return ComplaintResponseDto.fromEntity(saved);
    }
}
