package com.carry.repository;

import com.carry.entity.ApplicationStatus;
import com.carry.entity.RoleApplication;
import com.carry.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleApplicationRepository extends JpaRepository<RoleApplication, Long> {
    List<RoleApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<RoleApplication> findByRequestedRoleAndStatusOrderByCreatedAtDesc(UserRole requestedRole, ApplicationStatus status);
    List<RoleApplication> findByRequestedRoleOrderByCreatedAtDesc(UserRole requestedRole);
    Optional<RoleApplication> findByUserIdAndRequestedRoleAndStatus(Long userId, UserRole requestedRole, ApplicationStatus status);
    List<RoleApplication> findByReviewedById(Long reviewedById);
    void deleteByUserId(Long userId);
}
