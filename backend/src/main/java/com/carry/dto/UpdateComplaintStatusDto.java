package com.carry.dto;

import com.carry.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplaintStatusDto {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    private String adminNotes;
}
