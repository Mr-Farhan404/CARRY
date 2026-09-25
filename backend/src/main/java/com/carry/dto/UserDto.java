package com.carry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.carry.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String studentId;
    private String email;
    private String phone;
    private String department;
    private Boolean isAdmin;
    private BigDecimal avgRating;
    private LocalDateTime createdAt;

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .studentId(user.getStudentId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .isAdmin(user.getIsAdmin())
                .avgRating(user.getAvgRating())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
