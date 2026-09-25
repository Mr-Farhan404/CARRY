package com.carry.dto;

import com.carry.entity.LocationArea;
import com.carry.entity.Trip;
import com.carry.entity.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponseDto {

    private Long id;
    private Long partnerId;
    private String partnerName;
    private LocalDateTime departureTime;
    private LocalDateTime expectedReturnTime;
    private LocalDateTime actualReturnTime;
    private LocationArea destinationArea;
    private String capacityNotes;
    private TripStatus status;
    private LocalDateTime createdAt;

    public static TripResponseDto fromEntity(Trip trip) {
        return TripResponseDto.builder()
                .id(trip.getId())
                .partnerId(trip.getPartner() != null ? trip.getPartner().getId() : null)
                .partnerName(trip.getPartner() != null ? trip.getPartner().getFullName() : null)
                .departureTime(trip.getDepartureTime())
                .expectedReturnTime(trip.getExpectedReturnTime())
                .actualReturnTime(trip.getActualReturnTime())
                .destinationArea(trip.getDestinationArea())
                .capacityNotes(trip.getCapacityNotes())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .build();
    }
}
