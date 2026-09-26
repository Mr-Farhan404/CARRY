package com.carry.controller;

import com.carry.dto.AcceptRequestDto;
import com.carry.dto.CheckoutLineDto;
import com.carry.dto.CreateProductRequest;
import com.carry.dto.NeedMorePaymentDto;
import com.carry.dto.PaymentResponseDto;
import com.carry.dto.ProductRequestResponseDto;
import com.carry.dto.StatusUpdateResponseDto;
import com.carry.dto.SubmitAdditionalPaymentDto;
import com.carry.dto.UpdateProductRequest;
import com.carry.dto.UpdateRequestStatusDto;
import com.carry.exception.BadRequestException;
import com.carry.security.UserDetailsImpl;
import com.carry.service.ProductRequestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class ProductRequestController {

    private final ProductRequestService productRequestService;
    private final ObjectMapper objectMapper;

    @PostMapping("/checkout")
    public ResponseEntity<List<ProductRequestResponseDto>> checkout(
            @RequestBody JsonNode payload,
            @AuthenticationPrincipal UserDetailsImpl currentUser) throws JsonProcessingException {
        List<CheckoutLineDto> lines = new ArrayList<>();
        if (payload != null && payload.isArray()) {
            for (JsonNode node : payload) {
                lines.add(objectMapper.treeToValue(node, CheckoutLineDto.class));
            }
        } else if (payload != null && payload.isObject() && payload.has("items") && payload.get("items").isArray()) {
            for (JsonNode node : payload.get("items")) {
                lines.add(objectMapper.treeToValue(node, CheckoutLineDto.class));
            }
        } else {
            throw new BadRequestException("Invalid checkout payload format. Expected list of cart lines.");
        }
        return new ResponseEntity<>(productRequestService.checkout(lines, currentUser), HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<ProductRequestResponseDto> createRequest(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        ProductRequestResponseDto response = productRequestService.createRequest(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ProductRequestResponseDto>> getMyRequests(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.getMyRequests(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductRequestResponseDto> getRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.getRequestById(id, currentUser));
    }

    @GetMapping("/available")
    public ResponseEntity<List<ProductRequestResponseDto>> getAvailableRequests(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.getAvailableRequests(currentUser));
    }

    @GetMapping({"/assigned", "/deliveries"})
    public ResponseEntity<List<ProductRequestResponseDto>> getPartnerDeliveries(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.getPartnerDeliveries(currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductRequestResponseDto> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.updateRequest(id, request, currentUser));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ProductRequestResponseDto> cancelRequestPut(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.cancelRequest(id, currentUser));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ProductRequestResponseDto> cancelRequestPost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.cancelRequest(id, currentUser));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ProductRequestResponseDto> acceptRequest(
            @PathVariable Long id,
            @Valid @RequestBody AcceptRequestDto acceptDto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.acceptRequest(id, acceptDto, currentUser));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProductRequestResponseDto> updateRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestStatusDto updateDto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.updateRequestStatus(id, updateDto, currentUser));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<StatusUpdateResponseDto>> getRequestTimeline(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.getRequestTimeline(id, currentUser));
    }

    @PostMapping("/{id}/need-more")
    public ResponseEntity<PaymentResponseDto> requestNeedMore(
            @PathVariable Long id,
            @Valid @RequestBody NeedMorePaymentDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.requestAdditionalPayment(id, dto, currentUser));
    }

    @PostMapping("/{id}/additional-payment")
    public ResponseEntity<PaymentResponseDto> submitAdditionalPayment(
            @PathVariable Long id,
            @Valid @RequestBody SubmitAdditionalPaymentDto dto,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.submitAdditionalPayment(id, dto, currentUser));
    }

    @GetMapping("/{id}/payment")
    public ResponseEntity<PaymentResponseDto> getPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(productRequestService.getPaymentByRequestId(id, currentUser));
    }
}
