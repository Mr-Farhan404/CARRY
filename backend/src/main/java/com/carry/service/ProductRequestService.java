package com.carry.service;

import com.carry.dto.AcceptRequestDto;
import com.carry.dto.CreateProductRequest;
import com.carry.dto.ProductRequestResponseDto;
import com.carry.dto.StatusUpdateResponseDto;
import com.carry.dto.UpdateProductRequest;
import com.carry.dto.UpdateRequestStatusDto;
import com.carry.entity.LocationArea;
import com.carry.entity.Payment;
import com.carry.entity.PaymentStatus;
import com.carry.entity.ProductRequest;
import com.carry.entity.RequestStatus;
import com.carry.entity.StatusUpdate;
import com.carry.entity.Trip;
import com.carry.entity.TripStatus;
import com.carry.entity.User;
import com.carry.exception.BadRequestException;
import com.carry.exception.ForbiddenException;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.PaymentRepository;
import com.carry.repository.ProductRequestRepository;
import com.carry.repository.StatusUpdateRepository;
import com.carry.repository.TripRepository;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductRequestService {

    private final ProductRequestRepository productRequestRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final StatusUpdateRepository statusUpdateRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public ProductRequestResponseDto createRequest(CreateProductRequest request, UserDetailsImpl currentUser) {
        User customer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProductRequest productRequest = ProductRequest.builder()
                .customer(customer)
                .productName(request.getProductName())
                .category(request.getCategory())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .preferredShop(request.getPreferredShop())
                .pickupArea(request.getPickupArea())
                .budget(request.getBudget())
                .instructions(request.getInstructions())
                .status(RequestStatus.REQUESTED)
                .build();

        ProductRequest saved = productRequestRepository.save(productRequest);
        return ProductRequestResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductRequestResponseDto> getMyRequests(UserDetailsImpl currentUser) {
        return productRequestRepository.findByCustomerId(currentUser.getId())
                .stream()
                .map(ProductRequestResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductRequestResponseDto getRequestById(Long id, UserDetailsImpl currentUser) {
        ProductRequest existing = productRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));

        boolean isCustomer = existing.getCustomer().getId().equals(currentUser.getId());
        boolean isPartner = existing.getMatchedTrip() != null && existing.getMatchedTrip().getPartner().getId().equals(currentUser.getId());
        boolean isAdmin = Boolean.TRUE.equals(currentUser.getIsAdmin());

        if (!isCustomer && !isPartner && !isAdmin) {
            throw new ForbiddenException("You do not have permission to view this request");
        }

        return ProductRequestResponseDto.fromEntity(existing);
    }

    @Transactional(readOnly = true)
    public List<ProductRequestResponseDto> getPartnerDeliveries(UserDetailsImpl currentUser) {
        return productRequestRepository.findByPartnerId(currentUser.getId())
                .stream()
                .map(ProductRequestResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public ProductRequestResponseDto updateRequest(Long id, UpdateProductRequest updateDto, UserDetailsImpl currentUser) {
        ProductRequest existing = productRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));

        if (!existing.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the owning customer can edit this request");
        }

        if (existing.getStatus() != RequestStatus.REQUESTED) {
            throw new BadRequestException("Request can only be edited while in REQUESTED status. Current status is " + existing.getStatus());
        }

        if (updateDto.getVersion() != null && !updateDto.getVersion().equals(existing.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(ProductRequest.class, existing.getId());
        }

        existing.setProductName(updateDto.getProductName());
        existing.setCategory(updateDto.getCategory());
        if (updateDto.getQuantity() != null) {
            existing.setQuantity(updateDto.getQuantity());
        }
        existing.setPreferredShop(updateDto.getPreferredShop());
        existing.setPickupArea(updateDto.getPickupArea());
        existing.setBudget(updateDto.getBudget());
        existing.setInstructions(updateDto.getInstructions());

        ProductRequest updated = productRequestRepository.saveAndFlush(existing);
        return ProductRequestResponseDto.fromEntity(updated);
    }

    @Transactional
    public ProductRequestResponseDto cancelRequest(Long id, UserDetailsImpl currentUser) {
        ProductRequest existing = productRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));

        if (!existing.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the owning customer can cancel this request");
        }

        if (existing.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("Request is already cancelled");
        }

        // Allowed only before status reaches COLLECTED. From COLLECTED onward, reject.
        if (existing.getStatus() == RequestStatus.COLLECTED ||
            existing.getStatus() == RequestStatus.RETURNING ||
            existing.getStatus() == RequestStatus.READY_FOR_DELIVERY ||
            existing.getStatus() == RequestStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel request: partner has already spent money and collected the item. Please file a complaint instead.");
        }

        existing.setStatus(RequestStatus.CANCELLED);
        ProductRequest updated = productRequestRepository.save(existing);
        return ProductRequestResponseDto.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<ProductRequestResponseDto> getAvailableRequests(UserDetailsImpl currentUser) {
        List<LocationArea> activeAreas = tripRepository.findByPartnerId(currentUser.getId()).stream()
                .filter(t -> t.getStatus().isActive())
                .map(Trip::getDestinationArea)
                .distinct()
                .toList();

        if (activeAreas.isEmpty()) {
            return Collections.emptyList();
        }

        return productRequestRepository.findByStatusAndCustomerIdNotAndPickupAreaInOrderByCreatedAtAsc(
                RequestStatus.REQUESTED, currentUser.getId(), activeAreas)
                .stream()
                .map(ProductRequestResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public ProductRequestResponseDto acceptRequest(Long id, AcceptRequestDto acceptDto, UserDetailsImpl currentUser) {
        ProductRequest existing = productRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));

        if (existing.getCustomer().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot accept your own request.");
        }

        if (!acceptDto.getVersion().equals(existing.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(ProductRequest.class, existing.getId());
        }

        if (existing.getStatus() != RequestStatus.REQUESTED) {
            throw new BadRequestException("This request is no longer available (current status: " + existing.getStatus() + ").");
        }

        Trip trip = tripRepository.findById(acceptDto.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + acceptDto.getTripId()));

        if (!trip.getPartner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this trip.");
        }
        
        if (!trip.getStatus().isActive()) {
            throw new BadRequestException("Trip is not in a valid state to accept requests (PLANNED or IN_CITY).");
        }
        
        if (existing.getPickupArea() != trip.getDestinationArea()) {
            throw new BadRequestException("Trip destination does not match the request pickup area.");
        }

        existing.setMatchedTrip(trip);
        existing.setStatus(RequestStatus.ACCEPTED);
        
        // Optimistic locking handles concurrent saves correctly as version will be bumped
        ProductRequest updated = productRequestRepository.saveAndFlush(existing);

        // create initial status update record
        StatusUpdate update = StatusUpdate.builder()
                .request(updated)
                .status(RequestStatus.ACCEPTED.name())
                .note("Request accepted and matched with trip ID: " + trip.getId())
                .build();
        statusUpdateRepository.save(update);

        return ProductRequestResponseDto.fromEntity(updated);
    }

    @Transactional
    public ProductRequestResponseDto updateRequestStatus(Long id, UpdateRequestStatusDto updateDto, UserDetailsImpl currentUser) {
        ProductRequest existing = productRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));

        if (existing.getMatchedTrip() == null || !existing.getMatchedTrip().getPartner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the assigned partner can update the request status.");
        }

        validateStatusTransition(existing.getStatus(), updateDto.getStatus());

        existing.setStatus(updateDto.getStatus());
        ProductRequest updated = productRequestRepository.saveAndFlush(existing);

        StatusUpdate update = StatusUpdate.builder()
                .request(updated)
                .status(updateDto.getStatus().name())
                .note(updateDto.getNote())
                .build();
        statusUpdateRepository.save(update);

        if (updateDto.getStatus() == RequestStatus.DELIVERED) {
            BigDecimal cost = updated.getBudget() != null ? updated.getBudget() : BigDecimal.ZERO;
            BigDecimal fee = new BigDecimal("50.00"); // Standard delivery fee placeholder

            Payment payment = Payment.builder()
                    .request(updated)
                    .productCost(cost)
                    .deliveryFee(fee)
                    .total(cost.add(fee))
                    .status(PaymentStatus.SIMULATED_PAID)
                    .build();
            paymentRepository.save(payment);
        }

        return ProductRequestResponseDto.fromEntity(updated);
    }

    private void validateStatusTransition(RequestStatus current, RequestStatus target) {
        if (!current.canTransitionTo(target)) {
            throw new BadRequestException("Invalid request status transition from " + current + " to " + target);
        }
    }

    @Transactional(readOnly = true)
    public List<StatusUpdateResponseDto> getRequestTimeline(Long id, UserDetailsImpl currentUser) {
        ProductRequest existing = productRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));

        boolean isCustomer = existing.getCustomer().getId().equals(currentUser.getId());
        boolean isPartner = existing.getMatchedTrip() != null && existing.getMatchedTrip().getPartner().getId().equals(currentUser.getId());

        if (!isCustomer && !isPartner) {
            throw new ForbiddenException("Only the customer or assigned partner can view the timeline.");
        }

        return statusUpdateRepository.findByRequestIdOrderByCreatedAtAsc(id)
                .stream()
                .map(StatusUpdateResponseDto::fromEntity)
                .toList();
    }
}
