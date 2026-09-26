package com.carry.service;

import com.carry.dto.CreateProductDto;
import com.carry.dto.ProductResponseDto;
import com.carry.dto.UpdateProductDto;
import com.carry.entity.Product;
import com.carry.entity.ProductCategory;
import com.carry.exception.ForbiddenException;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.ProductRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DeliveryChargeService deliveryChargeService;

    public void verifyAdmin(UserDetailsImpl currentUser) {
        if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
            throw new ForbiddenException("Access denied: Admin privileges required");
        }
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getActiveProducts(ProductCategory category, String search) {
        String query = (search != null && !search.isBlank()) ? search.trim() : null;
        List<Product> products = productRepository.searchActiveProducts(category, query);

        return products.stream()
                .map(p -> {
                    BigDecimal charge = deliveryChargeService.calculateCatalogCharge(p, 1);
                    return ProductResponseDto.fromEntity(p, charge);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getActiveProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active product not found with id: " + id));

        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(product, 1);
        return ProductResponseDto.fromEntity(product, charge);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProductsForAdmin(UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);
        return productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(p -> {
                    BigDecimal charge = deliveryChargeService.calculateCatalogCharge(p, 1);
                    return ProductResponseDto.fromEntity(p, charge);
                })
                .toList();
    }

    @Transactional
    public ProductResponseDto createProduct(CreateProductDto dto, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);

        Product product = Product.builder()
                .name(dto.getName().trim())
                .category(dto.getCategory())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .imageUrl(dto.getImageUrl() != null ? dto.getImageUrl().trim() : null)
                .estimatedPrice(dto.getEstimatedPrice())
                .weightClass(dto.getWeightClass())
                .sizeClass(dto.getSizeClass())
                .isSensitive(Boolean.TRUE.equals(dto.getIsSensitive()))
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Product saved = productRepository.save(product);
        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(saved, 1);
        return ProductResponseDto.fromEntity(saved, charge);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long id, UpdateProductDto dto, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            product.setName(dto.getName().trim());
        }
        if (dto.getCategory() != null) {
            product.setCategory(dto.getCategory());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription().trim());
        }
        if (dto.getImageUrl() != null) {
            product.setImageUrl(dto.getImageUrl().trim());
        }
        if (dto.getEstimatedPrice() != null) {
            product.setEstimatedPrice(dto.getEstimatedPrice());
        }
        if (dto.getWeightClass() != null) {
            product.setWeightClass(dto.getWeightClass());
        }
        if (dto.getSizeClass() != null) {
            product.setSizeClass(dto.getSizeClass());
        }
        if (dto.getIsSensitive() != null) {
            product.setIsSensitive(dto.getIsSensitive());
        }
        if (dto.getIsActive() != null) {
            product.setIsActive(dto.getIsActive());
        }

        Product saved = productRepository.save(product);
        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(saved, 1);
        return ProductResponseDto.fromEntity(saved, charge);
    }

    @Transactional
    public ProductResponseDto toggleProductActive(Long id, Boolean isActive, UserDetailsImpl currentUser) {
        verifyAdmin(currentUser);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setIsActive(isActive != null ? isActive : !product.getIsActive());
        Product saved = productRepository.save(product);
        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(saved, 1);
        return ProductResponseDto.fromEntity(saved, charge);
    }
}
