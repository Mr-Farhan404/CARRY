package com.carry.service;

import com.carry.dto.CreateTripRequest;
import com.carry.dto.TripResponseDto;
import com.carry.dto.UpdateTripStatusRequest;
import com.carry.entity.Trip;
import com.carry.entity.TripStatus;
import com.carry.entity.User;
import com.carry.exception.BadRequestException;
import com.carry.exception.ForbiddenException;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.TripRepository;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public TripResponseDto createTrip(CreateTripRequest request, UserDetailsImpl currentUser) {
        if (!request.getExpectedReturnTime().isAfter(request.getDepartureTime())) {
            throw new BadRequestException("Expected return time must be after departure time");
        }

        User partner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Trip trip = Trip.builder()
                .partner(partner)
                .departureTime(request.getDepartureTime())
                .expectedReturnTime(request.getExpectedReturnTime())
                .destinationArea(request.getDestinationArea())
                .capacityNotes(request.getCapacityNotes())
                .status(TripStatus.PLANNED)
                .build();

        Trip saved = tripRepository.save(trip);
        return TripResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TripResponseDto> getMyTrips(UserDetailsImpl currentUser) {
        return tripRepository.findByPartnerId(currentUser.getId())
                .stream()
                .map(TripResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripResponseDto> getActiveTrips() {
        return tripRepository.findByStatusIn(List.of(TripStatus.PLANNED, TripStatus.IN_CITY))
                .stream()
                .map(TripResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public TripResponseDto updateTripStatus(Long tripId, UpdateTripStatusRequest request, UserDetailsImpl currentUser) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        if (!trip.getPartner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the owning partner can update this trip's status");
        }

        validateStatusTransition(trip.getStatus(), request.getStatus());

        trip.setStatus(request.getStatus());
        if (request.getStatus() == TripStatus.COMPLETED && trip.getActualReturnTime() == null) {
            trip.setActualReturnTime(LocalDateTime.now());
        }

        Trip updated = tripRepository.save(trip);
        return TripResponseDto.fromEntity(updated);
    }

    /**
     * Centralized transition rules:
     * PLANNED -> IN_CITY -> RETURNING -> COMPLETED
     * -> CANCELLED allowed only from PLANNED or IN_CITY
     */
    public void validateStatusTransition(TripStatus currentStatus, TripStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            throw new BadRequestException("Status cannot be null");
        }
        boolean valid = switch (currentStatus) {
            case PLANNED -> (newStatus == TripStatus.IN_CITY || newStatus == TripStatus.CANCELLED);
            case IN_CITY -> (newStatus == TripStatus.RETURNING || newStatus == TripStatus.CANCELLED);
            case RETURNING -> (newStatus == TripStatus.COMPLETED);
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }
    }
}
