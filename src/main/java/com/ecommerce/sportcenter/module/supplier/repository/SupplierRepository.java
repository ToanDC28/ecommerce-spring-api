package com.ecommerce.sportcenter.module.supplier.repository;

import com.ecommerce.sportcenter.module.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer>, JpaSpecificationExecutor<Supplier> {
    Optional<Supplier> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Integer id);

    @Query("SELECT s FROM Supplier s WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:isActive IS NULL OR s.active = :isActive)")
    Page<Supplier> search(@Param("keyword") String keyword,
                          @Param("isActive") Boolean isActive,
                          Pageable pageable);
}
