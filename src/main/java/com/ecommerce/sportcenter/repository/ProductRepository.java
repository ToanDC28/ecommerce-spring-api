package com.ecommerce.sportcenter.repository;

import com.ecommerce.sportcenter.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query(value = "SELECT * FROM product as p WHERE p.name = ?1", nativeQuery = true)
    Product findProductByName(String name);
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
    Specification<Product> searchByNameContaining(String keyword);
    Specification<Product> findByBrandId(Integer id);
    Specification<Product> findByTypeId(Integer id);
    Specification<Product> findByBrandAndType(Integer brandId, Integer typeId);
}
