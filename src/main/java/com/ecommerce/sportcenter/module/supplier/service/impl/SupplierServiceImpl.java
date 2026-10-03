package com.ecommerce.sportcenter.module.supplier.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.supplier.dto.mapper.SupplierMapper;
import com.ecommerce.sportcenter.module.supplier.dto.request.SearchSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.entity.Supplier;
import com.ecommerce.sportcenter.module.supplier.repository.SupplierRepository;
import com.ecommerce.sportcenter.module.supplier.dto.request.CreateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.UpdateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.response.SupplierResponse;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.sportcenter.module.supplier.service.SupplierService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(SearchSupplierRequest request) {
        log.info("Search suppliers - keyword={}, isActive={}, page={}, size={}",
                request.getKeyword(), request.getIsActive(), request.getPage(), request.getSize());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<Supplier> spec = withSearchAndFilters(request);
        // Merge generic advanced filters if present
        Specification<Supplier> advanced = new SpecificationBuilder<Supplier>().build(request);
        spec = spec.and(advanced);
        var page = supplierRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(supplierMapper::toResponse).toList();
        return PageResponse.<SupplierResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private Specification<Supplier> withSearchAndFilters(SearchSupplierRequest request) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("code")), like),
                        builder.like(builder.lower(root.get("name")), like),
                        builder.like(builder.lower(root.get("phone")), like)));
            }
            if (request.getIsActive() != null) {
                predicates.add(builder.equal(root.get("active"), request.getIsActive()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getAll(String keyword, Boolean isActive, Pageable pageable) {
        return supplierRepository.search(keyword, isActive, pageable).map(supplierMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getById(int id) {
        log.info("Get supplier by id - id={}", id);
        return supplierMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        String code = request.getCode() == null || request.getCode().isBlank()
                ? nextCode()
                : request.getCode().trim();
        if (supplierRepository.existsByCode(code)) {
            throw new BusinessValidationException("Supplier code '" + code + "' already exists");
        }
        Supplier supplier = supplierMapper.toEntity(request, code);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Override
    @Transactional
    public SupplierResponse update(int id, UpdateSupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        supplierMapper.updateSupplier(supplier, request);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Override
    @Transactional
    public SupplierResponse setActive(int id, boolean active) {
        Supplier supplier = findOrThrow(id);
        supplier.setActive(active);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    private Supplier findOrThrow(int id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    private String nextCode() {
        // Simple sequential code: SUP-001, SUP-002, ... based on row count.
        // Sufficient for phase 1; replace with a sequence table if concurrent creation matters.
        long count = supplierRepository.count() + 1;
        String code;
        do {
            code = String.format("SUP-%03d", count++);
        } while (supplierRepository.existsByCode(code));
        return code;
    }
}
