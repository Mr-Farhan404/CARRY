package com.carry.dto;

import com.carry.entity.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRequestStatusDto {
    @NotNull(message = "Target status must be provided")
    private RequestStatus status;
    private String note;
}
