package com.carry.dto;

import com.carry.entity.StatusUpdate;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusUpdateResponseDto {
    private Long id;
    private Long requestId;
    private String status;
    private String note;
    private LocalDateTime createdAt;

    public static StatusUpdateResponseDto fromEntity(StatusUpdate update) {
        return StatusUpdateResponseDto.builder()
            .id(update.getId())
            .requestId(update.getRequest().getId())
            .status(update.getStatus())
            .note(update.getNote())
            .createdAt(update.getCreatedAt())
            .build();
    }
}
