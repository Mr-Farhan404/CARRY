package com.carry.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptRequestDto {
    @NotNull(message = "Trip ID must be provided")
    private Long tripId;
    
    @NotNull(message = "Version must be provided to prevent concurrent accepts")
    private Integer version;
}
