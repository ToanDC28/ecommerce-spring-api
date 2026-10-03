package com.ecommerce.sportcenter.module.purchasing.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreatePurchaseInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreatePurchaseInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.service.InvoiceService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.purchasing.dto.mapper.GoodsReceiptMapper;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreateGoodsReceiptRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.request.SearchGoodsReceiptRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.response.GoodsReceiptResponse;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptItem;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptNote;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptStatus;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptType;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrder;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderStatus;
import com.ecommerce.sportcenter.module.purchasing.repository.GoodsReceiptNoteRepository;
import com.ecommerce.sportcenter.module.purchasing.repository.PurchaseOrderRepository;
import com.ecommerce.sportcenter.module.purchasing.service.GoodsReceiptService;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final InventoryService inventoryService;
    private final InvoiceService invoiceService;
    private final GoodsReceiptMapper goodsReceiptMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GoodsReceiptResponse> search(SearchGoodsReceiptRequest request) {
        log.info("Search GRNs - keyword={}, status={}, poId={}, warehouseId={}",
                request.getKeyword(), request.getStatus(), request.getPurchaseOrderId(), request.getWarehouseId());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<GoodsReceiptNote> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("code")),
                        "%" + request.getKeyword().toLowerCase() + "%"));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            if (request.getPurchaseOrderId() != null) {
                predicates.add(builder.equal(root.get("purchaseOrder").get("id"), request.getPurchaseOrderId()));
            }
            if (request.getWarehouseId() != null) {
                predicates.add(builder.equal(root.get("warehouse").get("id"), request.getWarehouseId()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<GoodsReceiptNote>().build(request));
        var page = goodsReceiptNoteRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(goodsReceiptMapper::toResponse).toList();
        return PageResponse.<GoodsReceiptResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptResponse getById(int id) {
        log.info("Get GRN by id - id={}", id);
        return goodsReceiptMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public GoodsReceiptResponse create(CreateGoodsReceiptRequest request, String username) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        PurchaseOrder po = null;
        Supplier supplier;
        if (request.getPurchaseOrderId() != null) {
            po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found with id: " + request.getPurchaseOrderId()));
            if (po.getStatus() != PurchaseOrderStatus.SENT && po.getStatus() != PurchaseOrderStatus.PARTIAL) {
                throw new BusinessValidationException("GRN can only reference SENT/PARTIAL purchase orders (current=" + po.getStatus() + ")");
            }
            if (request.getType() != null && request.getType() != GoodsReceiptType.IMPORT_PURCHASE) {
                throw new BusinessValidationException("GRN with purchase order must be IMPORT_PURCHASE");
            }
            supplier = po.getSupplier();
        } else {
            if (request.getSupplierId() == null) {
                throw new BusinessValidationException("Supplier is required when GRN has no purchase order (mua trực tiếp)");
            }
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));
            if (!supplier.isActive()) {
                throw new BusinessValidationException("Supplier '" + supplier.getCode() + "' is inactive");
            }
        }

        // Preload + validate vật tư trước khi lưu (tránh mồ côi phiếu DRAFT rỗng).
        Map<Integer, Material> materials = new HashMap<>();
        for (var item : request.getItems()) {
            if (!materials.containsKey(item.getMaterialId())) {
                Material material = materialRepository.findById(item.getMaterialId())
                        .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + item.getMaterialId()));
                if (!material.isActive()) {
                    throw new BusinessValidationException("Material '" + material.getSku() + "' is inactive");
                }
                materials.put(item.getMaterialId(), material);
            }
        }

        GoodsReceiptNote grn = GoodsReceiptNote.builder()
                .code(nextCode())
                .purchaseOrder(po)
                .warehouse(warehouse)
                .supplier(supplier)
                .receiptDate(LocalDate.now())
                .type(request.getType() == null ? GoodsReceiptType.IMPORT_PURCHASE : request.getType())
                .status(GoodsReceiptStatus.DRAFT)
                .createdBy(username)
                .build();
        grn = goodsReceiptNoteRepository.save(grn);

        for (var item : request.getItems()) {
            Material material = materials.get(item.getMaterialId());
            grn.getItems().add(GoodsReceiptItem.builder()
                    .goodsReceiptNote(grn)
                    .material(material)
                    .qty(item.getQty())
                    .unitCost(item.getUnitCost())
                    .batchNo(item.getBatchNo())
                    .lineTotal(item.getQty() * item.getUnitCost())
                    .build());
        }
        grn = goodsReceiptNoteRepository.save(grn);
        log.info("GRN created - code={}, po={}, items={}", grn.getCode(),
                po == null ? "direct" : po.getCode(), grn.getItems().size());
        return goodsReceiptMapper.toResponse(grn);
    }

    @Override
    @Transactional
    public GoodsReceiptResponse confirm(int id, String username) {
        GoodsReceiptNote grn = findOrThrow(id);
        if (grn.getStatus() != GoodsReceiptStatus.DRAFT) {
            throw new BusinessValidationException("Only DRAFT GRN can be confirmed (current=" + grn.getStatus() + ")");
        }

        // 1. Validate over-receipt against PO (nếu có) trước khi nhập kho.
        Map<Integer, Long> qtyByMaterial = new HashMap<>();
        for (var item : grn.getItems()) {
            qtyByMaterial.merge(item.getMaterial().getId(), item.getQty(), Long::sum);
        }
        PurchaseOrder po = grn.getPurchaseOrder();
        if (po != null) {
            for (var poLine : po.getItems()) {
                long requested = qtyByMaterial.getOrDefault(poLine.getMaterial().getId(), 0L);
                if (poLine.getReceivedQty() + requested > poLine.getQty()) {
                    throw new BusinessValidationException("Over-receipt for material '"
                            + poLine.getMaterial().getSku() + "': ordered=" + poLine.getQty()
                            + ", already received=" + poLine.getReceivedQty()
                            + ", this GRN=" + requested);
                }
            }
            // vật tư ngoài PO cũng bị chặn (tránh nhập sai)
            for (var materialId : qtyByMaterial.keySet()) {
                boolean inPo = po.getItems().stream().anyMatch(l -> l.getMaterial().getId() == materialId);
                if (!inPo) {
                    throw new BusinessValidationException("Material id=" + materialId + " is not in purchase order " + po.getCode());
                }
            }
        }

        // 2. Nhập kho từng dòng (Stock+ via InventoryService, immutable sau confirm).
        for (var item : grn.getItems()) {
            inventoryService.increase(item.getMaterial().getId(), grn.getWarehouse().getId(),
                    item.getQty(), com.ecommerce.sportcenter.module.inventory.entity.StockRefType.GRN,
                    grn.getCode(), username);
        }

        // 3. Cập nhật PO received + status.
        if (po != null) {
            for (var poLine : po.getItems()) {
                poLine.setReceivedQty(poLine.getReceivedQty() + qtyByMaterial.getOrDefault(poLine.getMaterial().getId(), 0L));
            }
            boolean allDone = po.getItems().stream().allMatch(l -> l.getReceivedQty() >= l.getQty());
            boolean anyReceived = po.getItems().stream().anyMatch(l -> l.getReceivedQty() > 0);
            po.setStatus(allDone ? PurchaseOrderStatus.COMPLETED
                    : anyReceived ? PurchaseOrderStatus.PARTIAL : PurchaseOrderStatus.SENT);
            purchaseOrderRepository.save(po);
        }

        grn.setStatus(GoodsReceiptStatus.CONFIRMED);
        grn = goodsReceiptNoteRepository.save(grn);

        // 4. Auto sinh PURCHASE invoice DRAFT (kế toán issue sau).
        var invoiceLines = grn.getItems().stream()
                .map(item -> CreatePurchaseInvoiceRequest.PurchaseInvoiceLine.builder()
                        .materialId(item.getMaterial().getId())
                        .description(item.getMaterial().getSku() + " - " + item.getMaterial().getName()
                                + (item.getBatchNo() == null ? "" : " [" + item.getBatchNo() + "]"))
                        .qty(item.getQty())
                        .unitCost(item.getUnitCost())
                        .build())
                .toList();
        Supplier supplier = grn.getSupplier() != null ? grn.getSupplier()
                : (po != null ? po.getSupplier() : null);
        if (supplier != null) {
            invoiceService.createPurchaseInvoice(CreatePurchaseInvoiceRequest.builder()
                    .supplierId(supplier.getId())
                    .supplierName(supplier.getName())
                    .refCode(grn.getCode())
                    .items(invoiceLines)
                    .vatRate(10)
                    .build());
        }

        log.info("GRN confirmed - code={}, stock increased, po={}", grn.getCode(), po == null ? "direct" : po.getCode());
        return goodsReceiptMapper.toResponse(grn);
    }

    @Override
    @Transactional
    public GoodsReceiptResponse quickImport(CreateGoodsReceiptRequest request, String username) {
        // Self-invocation: create()/confirm() join this transaction (REQUIRED),
        // nên cả tạo phiếu + nhập kho + invoice là 1 khối atomic.
        GoodsReceiptResponse created = create(request, username);
        return confirm(created.getId(), username);
    }

    @Override
    @Transactional
    public GoodsReceiptResponse cancel(int id) {
        GoodsReceiptNote grn = findOrThrow(id);
        if (grn.getStatus() != GoodsReceiptStatus.DRAFT) {
            throw new BusinessValidationException("Only DRAFT GRN can be cancelled (confirmed stock moves revert via return GRN)");
        }
        grn.setStatus(GoodsReceiptStatus.CANCELLED);
        return goodsReceiptMapper.toResponse(goodsReceiptNoteRepository.save(grn));
    }

    @Override
    @Transactional
    public com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse invoice(int id) {
        GoodsReceiptNote grn = findOrThrow(id);
        if (grn.getStatus() != GoodsReceiptStatus.CONFIRMED) {
            throw new BusinessValidationException("Only CONFIRMED GRN can be invoiced (current=" + grn.getStatus() + ")");
        }
        if (invoiceService.existsActiveInvoice(grn.getCode(), com.ecommerce.sportcenter.module.invoice.entity.InvoiceType.PURCHASE)) {
            throw new BusinessValidationException("GRN " + grn.getCode() + " already has an active PURCHASE invoice");
        }
        var invoiceLines = grn.getItems().stream()
                .map(item -> CreatePurchaseInvoiceRequest.PurchaseInvoiceLine.builder()
                        .materialId(item.getMaterial().getId())
                        .description(item.getMaterial().getSku() + " - " + item.getMaterial().getName()
                                + (item.getBatchNo() == null ? "" : " [" + item.getBatchNo() + "]"))
                        .qty(item.getQty())
                        .unitCost(item.getUnitCost())
                        .build())
                .toList();
        Supplier supplier = grn.getSupplier() != null ? grn.getSupplier()
                : (grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getSupplier() : null);
        if (supplier == null) {
            throw new BusinessValidationException("GRN has no supplier for invoicing");
        }
        return invoiceService.createPurchaseInvoice(CreatePurchaseInvoiceRequest.builder()
                .supplierId(supplier.getId())
                .supplierName(supplier.getName())
                .refCode(grn.getCode())
                .items(invoiceLines)
                .vatRate(10)
                .build());
    }

    private GoodsReceiptNote findOrThrow(int id) {
        return goodsReceiptNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceiptNote not found with id: " + id));
    }

    private String nextCode() {
        long count = goodsReceiptNoteRepository.count() + 1;
        String code;
        do {
            code = String.format("GRN-2026-%04d", count++);
        } while (goodsReceiptNoteRepository.existsByCode(code));
        return code;
    }
}
