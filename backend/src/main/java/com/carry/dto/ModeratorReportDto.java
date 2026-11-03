package com.carry.dto;

import com.carry.entity.ModeratorReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeratorReportDto {

    private Long id;
    private Long moderatorId;
    private String moderatorName;
    private String actionType;
    private String description;
    private Long targetId;
    private LocalDateTime createdAt;

    public static ModeratorReportDto fromEntity(ModeratorReport report) {
        if (report == null) return null;
        return ModeratorReportDto.builder()
                .id(report.getId())
                .moderatorId(report.getModerator() != null ? report.getModerator().getId() : null)
                .moderatorName(report.getModeratorName())
                .actionType(report.getActionType())
                .description(report.getDescription())
                .targetId(report.getTargetId())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
