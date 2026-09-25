package com.carry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintDto {

    @NotBlank(message = "Description is required")
    private String description;

    private Long requestId;
    private Long againstUserId;
}
