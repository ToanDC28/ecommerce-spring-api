package com.ecommerce.sportcenter.module.inventory.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.inventory.dto.mapper.InventoryMapper;
import com.ecommerce.sportcenter.module.inventory.dto.request.SearchStockRequest;
import com.ecommerce.sportcenter.module.inventory.dto.request.SearchStockTransactionRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockResponse;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockTransactionResponse;
import com.ecommerce.sportcenter.module.inventory.entity.Stock;
import com.ecommerce.sportcenter.module.inventory.entity.StockMoveType;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import com.ecommerce.sportcenter.module.inventory.entity.StockTransaction;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.inventory.repository.StockRepository;
import com.ecommerce.sportcenter.module.inventory.repository.StockTransactionRepository;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final StockTransactionRepository transactionRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional
    public void increase(int materialId, Integer warehouseId, long qty, StockRefType refType, String refId, String createdBy) {
        if (qty <= 0) {
            throw new BusinessValidationException("Quantity must be > 0");
        }
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));
        long before = material.getStockQty();
        material.setStockQty(before + qty);
        materialRepository.save(material);

        Warehouse warehouse = resolveWarehouse(warehouseId);
        if (warehouse != null) {
            Stock stock = stockRepository.findByWarehouse_IdAndMaterial_Id(warehouse.getId(), materialId)
                    .orElseGet(() -> stockRepository.save(Stock.builder()
                            .warehouse(warehouse).material(material).qtyOnHand(0L).qtyReserved(0L).build()));
            stock.setQtyOnHand(stock.getQtyOnHand() + qty);
            stockRepository.save(stock);
        }

        transactionRepository.save(StockTransaction.builder()
                .material(material).warehouse(warehouse)
                .type(StockMoveType.IN).refType(refType).refId(refId)
                .qtyBefore(before).qtyChange(qty).qtyAfter(before + qty)
                .createdBy(createdBy)
                .build());
        log.info("Stock IN - materialId={}, qty={}, ref={}:{}", materialId, qty, refType, refId);
    }

    @Override
    @Transactional
    public void decrease(int materialId, Integer warehouseId, long qty, StockRefType refType, String refId, String createdBy) {
        if (qty <= 0) {
            throw new BusinessValidationException("Quantity must be > 0");
        }
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));
        long before = material.getStockQty();
        if (before < qty) {
            throw new BusinessValidationException("Insufficient stock for material '" + material.getSku()
                    + "': requested=" + qty + ", available=" + before);
        }
        material.setStockQty(before - qty);
        materialRepository.save(material);

        Warehouse warehouse = resolveWarehouse(warehouseId);
        if (warehouse != null) {
            Stock stock = stockRepository.findByWarehouse_IdAndMaterial_Id(warehouse.getId(), materialId)
                    .orElseThrow(() -> new BusinessValidationException("No stock record for material in warehouse"));
            if (stock.getQtyOnHand() < qty) {
                throw new BusinessValidationException("Insufficient warehouse stock: requested=" + qty
                        + ", available=" + stock.getQtyOnHand());
            }
            stock.setQtyOnHand(stock.getQtyOnHand() - qty);
            stockRepository.save(stock);
        }

        transactionRepository.save(StockTransaction.builder()
                .material(material).warehouse(warehouse)
                .type(StockMoveType.OUT).refType(refType).refId(refId)
                .qtyBefore(before).qtyChange(-qty).qtyAfter(before - qty)
                .createdBy(createdBy)
                .build());
        log.info("Stock OUT - materialId={}, qty={}, ref={}:{}", materialId, qty, refType, refId);
    }

    private Warehouse resolveWarehouse(Integer warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockResponse> searchStocks(SearchStockRequest request) {
        log.info("Search stocks - warehouseId={}, materialId={}, lowStockOnly={}",
                request.getWarehouseId(), request.getMaterialId(), request.getLowStockOnly());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<Stock> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getWarehouseId() != null) {
                predicates.add(builder.equal(root.get("warehouse").get("id"), request.getWarehouseId()));
            }
            if (request.getMaterialId() != null) {
                predicates.add(builder.equal(root.get("material").get("id"), request.getMaterialId()));
            }
            if (Boolean.TRUE.equals(request.getLowStockOnly())) {
                predicates.add(builder.lessThanOrEqualTo(root.get("qtyOnHand").as(Long.class), root.get("material").get("minStock").as(Long.class)));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<Stock>().build(request));
        var page = stockRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(inventoryMapper::toResponse).toList();
        return PageResponse.<StockResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> lowStock(Integer warehouseId) {
        Specification<Stock> spec = (root, query, builder) -> {
            var low = builder.lessThanOrEqualTo(root.get("qtyOnHand").as(Long.class), root.get("material").get("minStock").as(Long.class));
            if (warehouseId == null) {
                return low;
            }
            return builder.and(low, builder.equal(root.get("warehouse").get("id"), warehouseId));
        };
        return stockRepository.findAll(spec).stream().map(inventoryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockTransactionResponse> searchTransactions(SearchStockTransactionRequest request) {
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<StockTransaction> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getMaterialId() != null) {
                predicates.add(builder.equal(root.get("material").get("id"), request.getMaterialId()));
            }
            if (request.getWarehouseId() != null) {
                predicates.add(builder.equal(root.get("warehouse").get("id"), request.getWarehouseId()));
            }
            if (request.getRefType() != null) {
                predicates.add(builder.equal(root.get("refType"), request.getRefType()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<StockTransaction>().build(request));
        var page = transactionRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(inventoryMapper::toResponse).toList();
        return PageResponse.<StockTransactionResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }
}
