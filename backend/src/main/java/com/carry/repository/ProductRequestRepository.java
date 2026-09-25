package com.carry.repository;

import com.carry.entity.ProductRequest;
import com.carry.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRequestRepository extends JpaRepository<ProductRequest, Long> {
    List<ProductRequest> findByCustomerId(Long customerId);
    List<ProductRequest> findByMatchedTripId(Long matchedTripId);
    List<ProductRequest> findByStatus(RequestStatus status);
    List<ProductRequest> findByStatusAndCustomerIdNotAndPickupAreaInOrderByCreatedAtAsc(
            RequestStatus status, Long customerId, java.util.Collection<com.carry.entity.LocationArea> pickupAreas);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM ProductRequest r WHERE r.matchedTrip.partner.id = :partnerId ORDER BY r.createdAt DESC")
    List<ProductRequest> findByPartnerId(@org.springframework.data.repository.query.Param("partnerId") Long partnerId);
}
