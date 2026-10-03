package com.ecommerce.sportcenter.module.purchasing.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.purchasing.dto.mapper.PurchaseOrderMapper;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreatePurchaseOrderRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.request.SearchPurchaseOrderRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.response.PurchaseOrderResponse;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrder;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderItem;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderStatus;
import com.ecommerce.sportcenter.module.purchasing.repository.GoodsReceiptNoteRepository;
import com.ecommerce.sportcenter.module.purchasing.repository.PurchaseOrderRepository;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptStatus;
import com.ecommerce.sportcenter.module.purchasing.service.PurchaseOrderService;
import com.ecommerce.sportcenter.module.supplier.entity.Supplier;
import com.ecommerce.sportcenter.module.supplier.repository.SupplierRepository;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> search(SearchPurchaseOrderRequest request) {
        log.info("Search purchase orders - keyword={}, supplierId={}, status={}",
                request.getKeyword(), request.getSupplierId(), request.getStatus());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<PurchaseOrder> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("code")), like),
                        builder.like(builder.lower(root.get("note")), like)));
            }
            if (request.getSupplierId() != null) {
                predicates.add(builder.equal(root.get("supplier").get("id"), request.getSupplierId()));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<PurchaseOrder>().build(request));
        var page = purchaseOrderRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(purchaseOrderMapper::toResponse).toList();
        return PageResponse.<PurchaseOrderResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(int id) {
        log.info("Get purchase order by id - id={}", id);
        return purchaseOrderMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request, String username) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
        if (!supplier.isActive()) {
            throw new BusinessValidationException("Supplier '" + supplier.getCode() + "' is inactive");
        }
        var seenMaterials = new HashSet<Integer>();
        for (var item : request.getItems()) {
            if (!seenMaterials.add(item.getMaterialId())) {
                throw new BusinessValidationException("Duplicate material in PO (materialId=" + item.getMaterialId() + ") — merge into one line");
            }
        }
        // Preload + validate vật tư trước khi lưu (tránh mồ côi PO rỗng).
        Map<Integer, Material> materials = new HashMap<>();
        for (var item : request.getItems()) {
            Material material = materialRepository.findById(item.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + item.getMaterialId()));
            if (!material.isActive()) {
                throw new BusinessValidationException("Material '" + material.getSku() + "' is inactive");
            }
            materials.put(item.getMaterialId(), material);
        }
        PurchaseOrder po = PurchaseOrder.builder()
                .code(nextCode())
                .supplier(supplier)
                .orderDate(LocalDate.now())
                .expectedDate(request.getExpectedDate())
                .status(PurchaseOrderStatus.DRAFT)
                .note(request.getNote())
                .createdBy(username)
                .build();
        po = purchaseOrderRepository.save(po);

        long total = 0L;
        for (var item : request.getItems()) {
            Material material = materials.get(item.getMaterialId());
            long lineTotal = item.getQty() * item.getUnitCost();
            total += lineTotal;
            po.getItems().add(PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .material(material)
                    .qty(item.getQty())
                    .unitCost(item.getUnitCost())
                    .lineTotal(lineTotal)
                    .receivedQty(0L)
                    .build());
        }
        po.setTotalAmount(total);
        po = purchaseOrderRepository.save(po);
        log.info("Purchase order created - code={}, supplier={}, total={}", po.getCode(), supplier.getCode(), total);
        return purchaseOrderMapper.toResponse(po);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse send(int id) {
        PurchaseOrder po = findOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessValidationException("Only DRAFT purchase order can be sent (current=" + po.getStatus() + ")");
        }
        po.setStatus(PurchaseOrderStatus.SENT);
        return purchaseOrderMapper.toResponse(purchaseOrderRepository.save(po));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancel(int id) {
        PurchaseOrder po = findOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT && po.getStatus() != PurchaseOrderStatus.SENT) {
            throw new BusinessValidationException("Only DRAFT/SENT purchase order can be cancelled (current=" + po.getStatus() + ")");
        }
        // SENT PO with confirmed receipts must be returned via GRN, not cancelled.
        boolean hasConfirmedReceipts = !goodsReceiptNoteRepository
                .findByPurchaseOrder_IdAndStatus(id, GoodsReceiptStatus.CONFIRMED).isEmpty();
        if (hasConfirmedReceipts) {
            throw new BusinessValidationException("Purchase order already has confirmed receipts — revert via return GRN");
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseOrderMapper.toResponse(purchaseOrderRepository.save(po));
    }

    private PurchaseOrder findOrThrow(int id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found with id: " + id));
    }

    private String nextCode() {
        long count = purchaseOrderRepository.count() + 1;
        String code;
        do {
            code = String.format("PO-2026-%04d", count++);
        } while (purchaseOrderRepository.existsByCode(code));
        return code;
    }
}
