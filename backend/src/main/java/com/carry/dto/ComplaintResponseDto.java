package com.carry.dto;

import com.carry.entity.Complaint;
import com.carry.entity.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponseDto {
    private Long id;
    private Long requestId;
    private Long raisedById;
    private String raisedByName;
    private Long againstUserId;
    private String againstUserName;
    private String description;
    private ComplaintStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static ComplaintResponseDto fromEntity(Complaint complaint) {
        return ComplaintResponseDto.builder()
                .id(complaint.getId())
                .requestId(complaint.getRequest() != null ? complaint.getRequest().getId() : null)
                .raisedById(complaint.getRaisedBy() != null ? complaint.getRaisedBy().getId() : null)
                .raisedByName(complaint.getRaisedBy() != null ? complaint.getRaisedBy().getFullName() : null)
                .againstUserId(complaint.getAgainstUser() != null ? complaint.getAgainstUser().getId() : null)
                .againstUserName(complaint.getAgainstUser() != null ? complaint.getAgainstUser().getFullName() : null)
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .adminNotes(complaint.getAdminNotes())
                .createdAt(complaint.getCreatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }
}
