package com.carry.dto;

import com.carry.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyRoleDto {

    @NotNull(message = "Requested role is required")
    private UserRole requestedRole;

    private String alternativePhone;

    private String bkashNumber;

    private String estimatedReturnTime;

    private Boolean activeHonestCommitment;

    private Boolean agreedToRules;

    private String reason;
}
