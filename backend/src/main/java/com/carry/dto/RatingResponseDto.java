package com.carry.dto;

import com.carry.entity.Rating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponseDto {
    private Long id;
    private Long requestId;
    private Long ratedById;
    private String ratedByName;
    private Long ratedUserId;
    private String ratedUserName;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;

    public static RatingResponseDto fromEntity(Rating rating) {
        return RatingResponseDto.builder()
                .id(rating.getId())
                .requestId(rating.getRequest() != null ? rating.getRequest().getId() : null)
                .ratedById(rating.getRatedBy() != null ? rating.getRatedBy().getId() : null)
                .ratedByName(rating.getRatedBy() != null ? rating.getRatedBy().getFullName() : null)
                .ratedUserId(rating.getRatedUser() != null ? rating.getRatedUser().getId() : null)
                .ratedUserName(rating.getRatedUser() != null ? rating.getRatedUser().getFullName() : null)
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
