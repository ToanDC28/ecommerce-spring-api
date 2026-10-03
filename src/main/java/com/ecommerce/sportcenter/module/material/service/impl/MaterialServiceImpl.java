package com.ecommerce.sportcenter.module.material.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.material.dto.mapper.MaterialMapper;
import com.ecommerce.sportcenter.module.material.dto.request.CreateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.SearchMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.UpdateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.response.MaterialResponse;
import com.ecommerce.sportcenter.module.material.entity.Category;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.repository.CategoryRepository;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.material.service.MaterialService;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialMapper materialMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MaterialResponse> search(SearchMaterialRequest request) {
        log.info("Search materials - keyword={}, sku={}, categoryId={}, lowStockOnly={}, page={}, size={}",
                request.getKeyword(), request.getSku(), request.getCategoryId(),
                request.getLowStockOnly(), request.getPage(), request.getSize());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<Material> spec = withSearchAndFilters(request);
        Specification<Material> advanced = new SpecificationBuilder<Material>().build(request);
        spec = spec.and(advanced);
        var page = materialRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(materialMapper::toResponse).toList();
        return PageResponse.<MaterialResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private Specification<Material> withSearchAndFilters(SearchMaterialRequest request) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("sku")), like),
                        builder.like(builder.lower(root.get("name")), like)));
            }
            if (request.getSku() != null && !request.getSku().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("sku")),
                        "%" + request.getSku().toLowerCase() + "%"));
            }
            if (request.getCategoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), request.getCategoryId()));
            }
            if (request.getActive() != null) {
                predicates.add(builder.equal(root.get("active"), request.getActive()));
            }
            if (Boolean.TRUE.equals(request.getLowStockOnly())) {
                predicates.add(builder.lessThanOrEqualTo(root.get("stockQty"), root.get("minStock")));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialResponse getById(int id) {
        log.info("Get material by id - id={}", id);
        return materialMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public MaterialResponse create(CreateMaterialRequest request) {
        // nếu nhập kho thêm cùng 1 loại hàng thì sao
        // Nếu 1 loại hàng như có 2 giá thì sao
        String sku = request.getSku().trim();
        if (materialRepository.existsBySku(sku)) {
            throw new BusinessValidationException("Material SKU '" + sku + "' already exists");
        }
        Material material = materialMapper.toEntity(request);
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            material.setCategory(category);
        }
        return materialMapper.toResponse(materialRepository.save(material));
    }

    @Override
    @Transactional
    public MaterialResponse update(int id, UpdateMaterialRequest request) {
        Material material = findOrThrow(id);
        materialMapper.updateMaterial(material, request);
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            material.setCategory(category);
        }
        return materialMapper.toResponse(materialRepository.save(material));
    }

    @Override
    @Transactional
    public MaterialResponse setActive(int id, boolean active) {
        Material material = findOrThrow(id);
        material.setActive(active);
        return materialMapper.toResponse(materialRepository.save(material));
    }

    private Material findOrThrow(int id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));
    }
}
