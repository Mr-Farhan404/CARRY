package com.carry.repository;

import com.carry.entity.Trip;
import com.carry.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByPartnerId(Long partnerId);
    List<Trip> findByStatus(TripStatus status);
    List<Trip> findByStatusIn(java.util.Collection<TripStatus> statuses);
}
