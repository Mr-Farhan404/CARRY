package com.carry.service;

import com.carry.dto.CreateProductDto;
import com.carry.dto.ProductResponseDto;
import com.carry.dto.UpdateProductDto;
import com.carry.entity.Product;
import com.carry.entity.ProductCategory;
import com.carry.entity.User;
import com.carry.entity.UserRole;
import com.carry.exception.ForbiddenException;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.ProductRepository;
import com.carry.repository.UserRepository;
import com.carry.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DeliveryChargeService deliveryChargeService;
    private final ModeratorReportService moderatorReportService;

    public void verifyAdminOrModerator(UserDetailsImpl currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Access denied: Authentication required");
        }
        boolean isAdmin = Boolean.TRUE.equals(currentUser.getIsAdmin()) || currentUser.getRole() == UserRole.ADMIN;
        boolean isModerator = currentUser.getRole() == UserRole.MODERATOR;
        if (!isAdmin && !isModerator) {
            throw new ForbiddenException("Access denied: Admin or Moderator privileges required");
        }
    }

    public void verifyAdmin(UserDetailsImpl currentUser) {
        if (currentUser == null || (!Boolean.TRUE.equals(currentUser.getIsAdmin()) && currentUser.getRole() != UserRole.ADMIN)) {
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
        verifyAdminOrModerator(currentUser);
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
        verifyAdminOrModerator(currentUser);

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

        if (currentUser.getRole() == UserRole.MODERATOR) {
            User mod = userRepository.findById(currentUser.getId()).orElse(null);
            if (mod != null) {
                moderatorReportService.logActivity(mod, "CREATE_PRODUCT",
                        String.format("Created product '%s' [Category: %s, Price: ৳%s, Img: %s]",
                                saved.getName(), saved.getCategory(), saved.getEstimatedPrice(),
                                saved.getImageUrl() != null ? saved.getImageUrl() : "None"),
                        saved.getId());
            }
        }

        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(saved, 1);
        return ProductResponseDto.fromEntity(saved, charge);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long id, UpdateProductDto dto, UserDetailsImpl currentUser) {
        verifyAdminOrModerator(currentUser);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        StringBuilder changeLog = new StringBuilder("Updated product '").append(product.getName()).append("': ");

        if (dto.getName() != null && !dto.getName().isBlank()) {
            changeLog.append("Name [").append(product.getName()).append(" -> ").append(dto.getName().trim()).append("] ");
            product.setName(dto.getName().trim());
        }
        if (dto.getCategory() != null) {
            product.setCategory(dto.getCategory());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription().trim());
        }
        if (dto.getImageUrl() != null) {
            changeLog.append("Image updated. ");
            product.setImageUrl(dto.getImageUrl().trim());
        }
        if (dto.getEstimatedPrice() != null) {
            changeLog.append("Price [৳").append(product.getEstimatedPrice()).append(" -> ৳").append(dto.getEstimatedPrice()).append("] ");
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

        if (currentUser.getRole() == UserRole.MODERATOR) {
            User mod = userRepository.findById(currentUser.getId()).orElse(null);
            if (mod != null) {
                moderatorReportService.logActivity(mod, "UPDATE_PRODUCT", changeLog.toString(), saved.getId());
            }
        }

        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(saved, 1);
        return ProductResponseDto.fromEntity(saved, charge);
    }

    @Transactional
    public ProductResponseDto toggleProductActive(Long id, Boolean isActive, UserDetailsImpl currentUser) {
        verifyAdminOrModerator(currentUser);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setIsActive(isActive != null ? isActive : !product.getIsActive());
        Product saved = productRepository.save(product);

        if (currentUser.getRole() == UserRole.MODERATOR) {
            User mod = userRepository.findById(currentUser.getId()).orElse(null);
            if (mod != null) {
                moderatorReportService.logActivity(mod, "TOGGLE_PRODUCT_STATUS",
                        (saved.getIsActive() ? "Activated" : "Deactivated") + " product: " + saved.getName(),
                        saved.getId());
            }
        }

        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(saved, 1);
        return ProductResponseDto.fromEntity(saved, charge);
    }
}
