package com.carry.repository;

import com.carry.entity.StatusUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusUpdateRepository extends JpaRepository<StatusUpdate, Long> {
    List<StatusUpdate> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
