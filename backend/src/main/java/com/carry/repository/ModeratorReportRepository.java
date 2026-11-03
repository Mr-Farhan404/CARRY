package com.carry.repository;

import com.carry.entity.ModeratorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModeratorReportRepository extends JpaRepository<ModeratorReport, Long> {
    List<ModeratorReport> findAllByOrderByCreatedAtDesc();
    void deleteByModeratorId(Long moderatorId);
}
