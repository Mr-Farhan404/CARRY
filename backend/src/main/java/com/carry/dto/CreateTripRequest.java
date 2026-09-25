package com.carry.dto;

import com.carry.entity.LocationArea;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Expected return time is required")
    private LocalDateTime expectedReturnTime;

    @NotNull(message = "Destination area is required")
    private LocationArea destinationArea;

    private String capacityNotes;
}
