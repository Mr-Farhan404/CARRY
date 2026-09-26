package com.carry.repository;

import com.carry.entity.Product;
import com.carry.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findByIsActiveTrueAndCategoryOrderByCreatedAtDesc(ProductCategory category);

    List<Product> findByIsActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Product> searchActiveProducts(@Param("category") ProductCategory category, @Param("query") String query);

    List<Product> findAllByOrderByCreatedAtDesc();
}
