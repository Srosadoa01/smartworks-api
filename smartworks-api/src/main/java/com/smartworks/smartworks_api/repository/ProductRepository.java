package com.smartworks.smartworks_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smartworks.smartworks_api.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select count(p) from Product p where p.stock <= p.lowStockThreshold")
    long countLowStock();

    @Query("select p from Product p where p.stock <= p.lowStockThreshold")
    List<Product> findLowStock();

    @Query("select p from Product p where p.stock = 0")
    List<Product> findOutOfStock();

    @Query("select count(p) from Product p")
    long countAllProducts();

    @Query("select count(p) from Product p where p.stock = 0")
    long countOutOfStock();

    @Query("select count(p) from Product p where p.stock > 0")
    long countAvailableProducts();

    List<Product> findByStockLessThanEqual(int stock);

    List<Product> findByNameContainingIgnoreCase(String name);
}