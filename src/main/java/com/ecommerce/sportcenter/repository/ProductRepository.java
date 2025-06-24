package com.ecommerce.sportcenter.repository;

import com.ecommerce.sportcenter.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query(value = "SELECT * FROM product as p WHERE p.name = ?1", nativeQuery = true)
    Product findProductByName(String name);
}
