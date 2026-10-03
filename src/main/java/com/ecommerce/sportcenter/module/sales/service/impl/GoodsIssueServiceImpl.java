package com.ecommerce.sportcenter.module.sales.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateSalesInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.service.InvoiceService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.sales.dto.mapper.GoodsIssueMapper;
import com.ecommerce.sportcenter.module.sales.dto.request.CreateGoodsIssueRequest;
import com.ecommerce.sportcenter.module.sales.dto.request.SearchGoodsIssueRequest;
import com.ecommerce.sportcenter.module.sales.dto.response.GoodsIssueResponse;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueItem;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueNote;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueStatus;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueType;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrder;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrderStatus;
import com.ecommerce.sportcenter.module.sales.repository.GoodsIssueNoteRepository;
import com.ecommerce.sportcenter.module.sales.repository.SalesOrderRepository;
import com.ecommerce.sportcenter.module.sales.service.GoodsIssueService;
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
public class GoodsIssueServiceImpl implements GoodsIssueService {

    private final GoodsIssueNoteRepository goodsIssueNoteRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final MaterialRepository materialRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;
    private final InvoiceService invoiceService;
    private final GoodsIssueMapper goodsIssueMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GoodsIssueResponse> search(SearchGoodsIssueRequest request) {
        log.info("Search GINs - keyword={}, status={}, soId={}, warehouseId={}",
                request.getKeyword(), request.getStatus(), request.getSalesOrderId(), request.getWarehouseId());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<GoodsIssueNote> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("code")),
                        "%" + request.getKeyword().toLowerCase() + "%"));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            if (request.getSalesOrderId() != null) {
                predicates.add(builder.equal(root.get("salesOrder").get("id"), request.getSalesOrderId()));
            }
            if (request.getWarehouseId() != null) {
                predicates.add(builder.equal(root.get("warehouse").get("id"), request.getWarehouseId()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<GoodsIssueNote>().build(request));
        var page = goodsIssueNoteRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(goodsIssueMapper::toResponse).toList();
        return PageResponse.<GoodsIssueResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsIssueResponse getById(int id) {
        log.info("Get GIN by id - id={}", id);
        return goodsIssueMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public GoodsIssueResponse create(CreateGoodsIssueRequest request, String username) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));
        GoodsIssueType type = request.getType() == null ? GoodsIssueType.EXPORT_SALE : request.getType();

        SalesOrder so = null;
        com.ecommerce.sportcenter.module.customer.entity.Customer customer = null;
        String customerName = request.getCustomerName();
        if (type == GoodsIssueType.EXPORT_SALE) {
            if (request.getSalesOrderId() != null) {
                so = salesOrderRepository.findById(request.getSalesOrderId())
                        .orElseThrow(() -> new ResourceNotFoundException("SalesOrder not found with id: " + request.getSalesOrderId()));
                if (so.getStatus() != SalesOrderStatus.CONFIRMED && so.getStatus() != SalesOrderStatus.DELIVERING) {
                    throw new BusinessValidationException("GIN can only reference CONFIRMED/DELIVERING sales orders (current=" + so.getStatus() + ")");
                }
                customer = so.getCustomer();
                if (customerName == null || customerName.isBlank()) {
                    customerName = so.getCustomerName();
                }
            } else {
                // Bán trực tiếp: UI tạo Customer trước, GIN bắt link mã KH.
                if (request.getCustomerId() == null) {
                    throw new BusinessValidationException("Customer id is required for direct sale (tạo Customer trước nếu chưa có)");
                }
                customer = customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
                if (!customer.isActive()) {
                    throw new BusinessValidationException("Customer '" + customer.getCode() + "' is inactive");
                }
                if (customerName == null || customerName.isBlank()) {
                    customerName = customer.getName();
                }
            }
        } else { // EXPORT_RETURN
            if (request.getSalesOrderId() == null) {
                throw new BusinessValidationException("Return GIN must reference a sales order");
            }
            so = salesOrderRepository.findById(request.getSalesOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("SalesOrder not found with id: " + request.getSalesOrderId()));
            customer = so.getCustomer();
            customerName = so.getCustomerName();
        }

        // Validate toàn bộ + preload vật tư trước khi lưu
        // (tránh mồ côi phiếu DRAFT rỗng khi rớt validate).
        Map<Integer, Long> qtyByMaterial = new HashMap<>();
        for (var item : request.getItems()) {
            qtyByMaterial.merge(item.getMaterialId(), item.getQty(), Long::sum);
        }
        Map<Integer, SalesOrderLine> soLines = new HashMap<>();
        if (so != null) {
            for (var soLine : so.getItems()) {
                soLines.put(soLine.getMaterial().getId(), new SalesOrderLine(soLine.getQty(), soLine.getUnitPrice(), soLine.getIssuedQty(), soLine.getReturnedQty()));
            }
            for (var entry : qtyByMaterial.entrySet()) {
                var soLine = soLines.get(entry.getKey());
                if (type == GoodsIssueType.EXPORT_SALE) {
                    if (soLine == null) {
                        throw new BusinessValidationException("Material id=" + entry.getKey() + " is not in sales order " + so.getCode());
                    }
                    long remaining = soLine.qty() - soLine.issuedQty();
                    if (entry.getValue() > remaining) {
                        throw new BusinessValidationException("Issue exceeds remaining order qty (material id=" + entry.getKey()
                                + ": ordered=" + soLine.qty() + ", issued=" + soLine.issuedQty()
                                + ", this GIN=" + entry.getValue() + ")");
                    }
                } else {
                    if (soLine == null) {
                        throw new BusinessValidationException("Material id=" + entry.getKey() + " was never sold in order " + so.getCode());
                    }
                    long returnable = soLine.issuedQty() - soLine.returnedQty();
                    if (entry.getValue() > returnable) {
                        throw new BusinessValidationException("Return exceeds net issued qty (material id=" + entry.getKey()
                                + ": issued=" + soLine.issuedQty() + ", returned=" + soLine.returnedQty()
                                + ", this GIN=" + entry.getValue() + ")");
                    }
                }
            }
        }

        Map<Integer, Material> materials = new HashMap<>();
        Map<Integer, Long> unitPrices = new HashMap<>();
        for (var item : request.getItems()) {
            Material material = materialRepository.findById(item.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + item.getMaterialId()));
            if (!material.isActive()) {
                throw new BusinessValidationException("Material '" + material.getSku() + "' is inactive");
            }
            long unitPrice;
            if (so != null && type == GoodsIssueType.EXPORT_SALE) {
                unitPrice = soLines.get(item.getMaterialId()).unitPrice();
            } else {
                if (material.getSellPrice() == null) {
                    throw new BusinessValidationException("Material '" + material.getSku() + "' is internal-only (no sell price)");
                }
                unitPrice = material.getSellPrice();
            }
            materials.put(item.getMaterialId(), material);
            unitPrices.put(item.getMaterialId(), unitPrice);
        }

        GoodsIssueNote gin = GoodsIssueNote.builder()
                .code(nextCode())
                .salesOrder(so)
                .customer(customer)
                .customerName(so == null ? customerName : null)
                .warehouse(warehouse)
                .issueDate(LocalDate.now())
                .type(type)
                .status(GoodsIssueStatus.DRAFT)
                .createdBy(username)
                .build();
        gin = goodsIssueNoteRepository.save(gin);

        for (var item : request.getItems()) {
            Material material = materials.get(item.getMaterialId());
            long unitPrice = unitPrices.get(item.getMaterialId());
            gin.getItems().add(GoodsIssueItem.builder()
                    .goodsIssueNote(gin)
                    .material(material)
                    .qty(item.getQty())
                    .unitPrice(unitPrice)
                    .lineTotal(item.getQty() * unitPrice)
                    .build());
        }
        gin = goodsIssueNoteRepository.save(gin);
        log.info("GIN created - code={}, type={}, so={}", gin.getCode(), type, so == null ? "direct" : so.getCode());
        return goodsIssueMapper.toResponse(gin);
    }

    @Override
    @Transactional
    public GoodsIssueResponse confirm(int id, String username) {
        GoodsIssueNote gin = findOrThrow(id);
        if (gin.getStatus() != GoodsIssueStatus.DRAFT) {
            throw new BusinessValidationException("Only DRAFT GIN can be confirmed (current=" + gin.getStatus() + ")");
        }
        SalesOrder so = gin.getSalesOrder();

        if (gin.getType() == GoodsIssueType.EXPORT_SALE) {
            // Xuất kho từng dòng (thiếu hàng -> 409 từ InventoryService).
            for (var item : gin.getItems()) {
                inventoryService.decrease(item.getMaterial().getId(), gin.getWarehouse().getId(),
                        item.getQty(), StockRefType.GIN, gin.getCode(), username);
            }
            // Cập nhật SO issued + status.
            if (so != null) {
                Map<Integer, Long> qtyByMaterial = new HashMap<>();
                for (var item : gin.getItems()) {
                    qtyByMaterial.merge(item.getMaterial().getId(), item.getQty(), Long::sum);
                }
                for (var soLine : so.getItems()) {
                    soLine.setIssuedQty(soLine.getIssuedQty() + qtyByMaterial.getOrDefault(soLine.getMaterial().getId(), 0L));
                }
                boolean allIssued = so.getItems().stream().allMatch(l -> l.getIssuedQty() >= l.getQty());
                so.setStatus(allIssued ? SalesOrderStatus.COMPLETED : SalesOrderStatus.DELIVERING);
                salesOrderRepository.save(so);
            }
        } else { // EXPORT_RETURN: nhập lại kho
            for (var item : gin.getItems()) {
                inventoryService.increase(item.getMaterial().getId(), gin.getWarehouse().getId(),
                        item.getQty(), StockRefType.GIN, gin.getCode(), username);
            }
            if (so != null) {
                Map<Integer, Long> qtyByMaterial = new HashMap<>();
                for (var item : gin.getItems()) {
                    qtyByMaterial.merge(item.getMaterial().getId(), item.getQty(), Long::sum);
                }
                for (var soLine : so.getItems()) {
                    soLine.setReturnedQty(soLine.getReturnedQty() + qtyByMaterial.getOrDefault(soLine.getMaterial().getId(), 0L));
                }
                salesOrderRepository.save(so);
            }
        }

        gin.setStatus(GoodsIssueStatus.CONFIRMED);
        gin = goodsIssueNoteRepository.save(gin);

        // Auto sinh SALES invoice DRAFT cho EXPORT_SALE (return thì không).
        // Mọi GIN bán đều có customer link (SO link sẵn, trực tiếp bắt customerId) — không snapshot tự do.
        if (gin.getType() == GoodsIssueType.EXPORT_SALE) {
            Integer ginCustomerId = gin.getCustomer() != null ? gin.getCustomer().getId()
                    : (so == null || so.getCustomer() == null ? null : so.getCustomer().getId());
            if (ginCustomerId == null) {
                throw new BusinessValidationException("GIN " + gin.getCode() + " thiếu link Customer (dữ liệu cũ trước khi bắt buộc mã KH)");
            }
            String customerName = so != null ? so.getCustomerName() : gin.getCustomerName();
            String customerPhone = so != null ? so.getCustomerPhone() : null;
            var invoiceLines = gin.getItems().stream()
                    .map(item -> CreateSalesInvoiceRequest.SalesInvoiceLine.builder()
                            .materialId(item.getMaterial().getId())
                            .description(item.getMaterial().getSku() + " - " + item.getMaterial().getName())
                            .qty(item.getQty())
                            .unitPrice(item.getUnitPrice())
                            .build())
                    .toList();
            invoiceService.createSalesInvoice(CreateSalesInvoiceRequest.builder()
                    .soId(so == null ? null : so.getId())
                    .soCode(so == null ? null : so.getCode())
                    .customerId(ginCustomerId)
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .customerId(so == null ? null : (so.getCustomer() == null ? null : so.getCustomer().getId()))
                    .refCode(gin.getCode())
                    .items(invoiceLines)
                    .vatRate(0)
                    .build());
        }

        log.info("GIN confirmed - code={}, type={}, so={}", gin.getCode(), gin.getType(), so == null ? "direct" : so.getCode());
        return goodsIssueMapper.toResponse(gin);
    }

    @Override
    @Transactional
    public GoodsIssueResponse cancel(int id) {
        GoodsIssueNote gin = findOrThrow(id);
        if (gin.getStatus() != GoodsIssueStatus.DRAFT) {
            throw new BusinessValidationException("Only DRAFT GIN can be cancelled (confirmed stock moves revert via return GIN)");
        }
        gin.setStatus(GoodsIssueStatus.CANCELLED);
        return goodsIssueMapper.toResponse(goodsIssueNoteRepository.save(gin));
    }

    @Override
    @Transactional
    public GoodsIssueResponse quickSale(CreateGoodsIssueRequest request, String username) {
        // Self-invocation: create()/confirm() join this transaction (REQUIRED).
        GoodsIssueResponse created = create(request, username);
        return confirm(created.getId(), username);
    }

    @Override
    @Transactional
    public com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse invoice(int id) {
        GoodsIssueNote gin = findOrThrow(id);
        if (gin.getStatus() != GoodsIssueStatus.CONFIRMED) {
            throw new BusinessValidationException("Only CONFIRMED GIN can be invoiced (current=" + gin.getStatus() + ")");
        }
        if (gin.getType() != GoodsIssueType.EXPORT_SALE) {
            throw new BusinessValidationException("Only EXPORT_SALE GIN can be invoiced");
        }
        if (invoiceService.existsActiveInvoice(gin.getCode(), com.ecommerce.sportcenter.module.invoice.entity.InvoiceType.SALES)) {
            throw new BusinessValidationException("GIN " + gin.getCode() + " already has an active SALES invoice");
        }
        SalesOrder so = gin.getSalesOrder();
        Integer ginCustomerId = gin.getCustomer() != null ? gin.getCustomer().getId()
                : (so == null || so.getCustomer() == null ? null : so.getCustomer().getId());
        if (ginCustomerId == null) {
            throw new BusinessValidationException("GIN " + gin.getCode() + " thiếu link Customer (dữ liệu cũ trước khi bắt buộc mã KH)");
        }
        var invoiceLines = gin.getItems().stream()
                .map(item -> CreateSalesInvoiceRequest.SalesInvoiceLine.builder()
                        .materialId(item.getMaterial().getId())
                        .description(item.getMaterial().getSku() + " - " + item.getMaterial().getName())
                        .qty(item.getQty())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .toList();
        return invoiceService.createSalesInvoice(CreateSalesInvoiceRequest.builder()
                .soId(so == null ? null : so.getId())
                .soCode(so == null ? null : so.getCode())
                .customerId(ginCustomerId)
                .customerName(so != null ? so.getCustomerName() : gin.getCustomerName())
                .customerPhone(so != null ? so.getCustomerPhone() : null)
                .refCode(gin.getCode())
                .items(invoiceLines)
                .vatRate(0)
                .build());
    }

    private GoodsIssueNote findOrThrow(int id) {
        return goodsIssueNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsIssueNote not found with id: " + id));
    }

    private String nextCode() {
        long count = goodsIssueNoteRepository.count() + 1;
        String code;
        do {
            code = String.format("GIN-2026-%04d", count++);
        } while (goodsIssueNoteRepository.existsByCode(code));
        return code;
    }

    private record SalesOrderLine(long qty, long unitPrice, long issuedQty, long returnedQty) {
    }
}
