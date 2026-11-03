package com.carry.service;

import com.carry.dto.ModeratorReportDto;
import com.carry.entity.ModeratorReport;
import com.carry.entity.User;
import com.carry.repository.ModeratorReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModeratorReportService {

    private final ModeratorReportRepository moderatorReportRepository;

    @Transactional
    public void logActivity(User moderator, String actionType, String description, Long targetId) {
        if (moderator == null) {
            log.warn("Cannot log moderator activity without a moderator user.");
            return;
        }

        ModeratorReport report = ModeratorReport.builder()
                .moderator(moderator)
                .moderatorName(moderator.getFullName())
                .actionType(actionType)
                .description(description)
                .targetId(targetId)
                .build();

        moderatorReportRepository.save(report);
        log.info("Logged activity for moderator [{}]: {} on target {}", moderator.getEmail(), actionType, targetId);
    }

    @Transactional(readOnly = true)
    public List<ModeratorReportDto> getAllReports() {
        return moderatorReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ModeratorReportDto::fromEntity)
                .collect(Collectors.toList());
    }
}
