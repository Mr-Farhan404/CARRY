package com.carry.controller;

import com.carry.dto.ProductResponseDto;
import com.carry.entity.ProductCategory;
import com.carry.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getActiveProducts(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.getActiveProducts(category, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getActiveProductById(id));
    }
}
