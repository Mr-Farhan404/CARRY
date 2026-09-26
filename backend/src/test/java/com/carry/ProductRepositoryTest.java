package com.carry;

import com.carry.entity.Product;
import com.carry.entity.ProductCategory;
import com.carry.entity.SizeClass;
import com.carry.entity.WeightClass;
import com.carry.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAllInBatch();
    }

    @Test
    void testSaveAndFindActiveProductsExcludesInactive() {
        Product activeFood = Product.builder()
                .name("Kacchi Biryani")
                .category(ProductCategory.FOOD)
                .estimatedPrice(new BigDecimal("320.00"))
                .weightClass(WeightClass.MEDIUM)
                .sizeClass(SizeClass.MEDIUM)
                .isSensitive(false)
                .isActive(true)
                .build();

        Product inactiveFood = Product.builder()
                .name("Old Cold Drink")
                .category(ProductCategory.FOOD)
                .estimatedPrice(new BigDecimal("30.00"))
                .weightClass(WeightClass.LIGHT)
                .sizeClass(SizeClass.SMALL)
                .isSensitive(false)
                .isActive(false)
                .build();

        Product activeElectronics = Product.builder()
                .name("Arduino Uno")
                .category(ProductCategory.ELECTRONICS)
                .estimatedPrice(new BigDecimal("850.00"))
                .weightClass(WeightClass.LIGHT)
                .sizeClass(SizeClass.SMALL)
                .isSensitive(true)
                .isActive(true)
                .build();

        productRepository.saveAll(List.of(activeFood, inactiveFood, activeElectronics));

        List<Product> activeProducts = productRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        assertEquals(2, activeProducts.size());
        assertTrue(activeProducts.stream().allMatch(Product::getIsActive));

        List<Product> allProducts = productRepository.findAllByOrderByCreatedAtDesc();
        assertEquals(3, allProducts.size());
    }

    @Test
    void testFilterByCategoryAndSearch() {
        Product biryani = Product.builder()
                .name("Hyderabadi Kacchi Biryani")
                .category(ProductCategory.FOOD)
                .estimatedPrice(new BigDecimal("320.00"))
                .weightClass(WeightClass.MEDIUM)
                .sizeClass(SizeClass.MEDIUM)
                .isSensitive(false)
                .isActive(true)
                .build();

        Product chicken = Product.builder()
                .name("Crispy Fried Chicken")
                .category(ProductCategory.FOOD)
                .estimatedPrice(new BigDecimal("280.00"))
                .weightClass(WeightClass.LIGHT)
                .sizeClass(SizeClass.SMALL)
                .isSensitive(false)
                .isActive(true)
                .build();

        Product keyboard = Product.builder()
                .name("Mechanical Gaming Keyboard")
                .category(ProductCategory.ELECTRONICS)
                .estimatedPrice(new BigDecimal("1950.00"))
                .weightClass(WeightClass.MEDIUM)
                .sizeClass(SizeClass.MEDIUM)
                .isSensitive(true)
                .isActive(true)
                .build();

        productRepository.saveAll(List.of(biryani, chicken, keyboard));

        // Category filter
        List<Product> foodItems = productRepository.searchActiveProducts(ProductCategory.FOOD, null);
        assertEquals(2, foodItems.size());

        // Text search across all categories
        List<Product> searchResults = productRepository.searchActiveProducts(null, "keyboard");
        assertEquals(1, searchResults.size());
        assertEquals("Mechanical Gaming Keyboard", searchResults.get(0).getName());

        // Category + Text search
        List<Product> filteredSearch = productRepository.searchActiveProducts(ProductCategory.FOOD, "chicken");
        assertEquals(1, filteredSearch.size());
        assertEquals("Crispy Fried Chicken", filteredSearch.get(0).getName());

        // No matches
        List<Product> noMatches = productRepository.searchActiveProducts(ProductCategory.FOOD, "keyboard");
        assertTrue(noMatches.isEmpty());
    }
}
