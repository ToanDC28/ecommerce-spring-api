package com.ecommerce.sportcenter.module.sales.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.customer.service.CustomerService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.sales.dto.mapper.SalesOrderMapper;
import com.ecommerce.sportcenter.module.sales.dto.request.CreateSalesOrderRequest;
import com.ecommerce.sportcenter.module.sales.dto.request.SearchSalesOrderRequest;
import com.ecommerce.sportcenter.module.sales.dto.response.SalesOrderResponse;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueStatus;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrder;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrderItem;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrderStatus;
import com.ecommerce.sportcenter.module.sales.repository.GoodsIssueNoteRepository;
import com.ecommerce.sportcenter.module.sales.repository.SalesOrderRepository;
import com.ecommerce.sportcenter.module.sales.service.SalesOrderService;
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
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final GoodsIssueNoteRepository goodsIssueNoteRepository;
    private final MaterialRepository materialRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final SalesOrderMapper salesOrderMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SalesOrderResponse> search(SearchSalesOrderRequest request) {
        log.info("Search sales orders - keyword={}, status={}", request.getKeyword(), request.getStatus());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<SalesOrder> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("code")), like),
                        builder.like(builder.lower(root.get("customerName")), like),
                        builder.like(builder.lower(root.get("note")), like)));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<SalesOrder>().build(request));
        var page = salesOrderRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(salesOrderMapper::toResponse).toList();
        return PageResponse.<SalesOrderResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponse getById(int id) {
        log.info("Get sales order by id - id={}", id);
        return salesOrderMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public SalesOrderResponse create(CreateSalesOrderRequest request, String username) {
        var seenMaterials = new HashSet<Integer>();
        for (var item : request.getItems()) {
            if (!seenMaterials.add(item.getMaterialId())) {
                throw new BusinessValidationException("Duplicate material in SO (materialId=" + item.getMaterialId() + ") — merge into one line");
            }
        }
        long discount = request.getDiscount() == null ? 0L : request.getDiscount();
        // Preload + validate vật tư trước khi lưu (tránh mồ côi SO rỗng).
        // UI tạo Customer trước nếu chưa có — SO luôn link mã KH, không snapshot tự do.
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        if (!customer.isActive()) {
            throw new BusinessValidationException("Customer '" + customer.getCode() + "' is inactive");
        }
        String customerName = request.getCustomerName() == null || request.getCustomerName().isBlank()
                ? customer.getName() : request.getCustomerName();
        String customerPhone = request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()
                ? customer.getPhone() : request.getCustomerPhone();
        Map<Integer, Material> materials = new HashMap<>();
        Map<Integer, Long> unitPrices = new HashMap<>();
        for (var item : request.getItems()) {
            if (!materials.containsKey(item.getMaterialId())) {
                Material material = materialRepository.findById(item.getMaterialId())
                        .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + item.getMaterialId()));
                if (!material.isActive()) {
                    throw new BusinessValidationException("Material '" + material.getSku() + "' is inactive");
                }
                // Giá riêng của khách (nếu có) -> sellPrice chung -> null = hàng nội bộ.
                Long unitPrice = customerService.resolveSellPrice(customer.getId(), material);
                if (unitPrice == null) {
                    throw new BusinessValidationException("Material '" + material.getSku() + "' is internal-only (no sell price)");
                }
                materials.put(item.getMaterialId(), material);
                unitPrices.put(item.getMaterialId(), unitPrice);
            }
        }
        // Tính totals + check hạn mức TRƯỚC khi lưu (tránh mồ côi SO).
        long subTotal = 0L;
        for (var item : request.getItems()) {
            Material material = materials.get(item.getMaterialId());
            long itemDiscount = item.getDiscount() == null ? 0L : item.getDiscount();
            long lineTotal = item.getQty() * unitPrices.get(item.getMaterialId()) - itemDiscount;
            if (lineTotal < 0) {
                throw new BusinessValidationException("Line total must be >= 0 (material '" + material.getSku() + "')");
            }
            subTotal += lineTotal;
        }
        if (discount > subTotal) {
            throw new BusinessValidationException("Order discount must be <= subTotal");
        }
        long grandTotal = subTotal - discount;
        SalesOrder so = SalesOrder.builder()
                .code(nextCode())
                .customer(customer)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PENDING)
                .discount(discount)
                .note(request.getNote())
                .createdBy(username)
                .subTotal(subTotal)
                .grandTotal(grandTotal)
                .build();
        so = salesOrderRepository.save(so);

        for (var item : request.getItems()) {
            Material material = materials.get(item.getMaterialId());
            long itemDiscount = item.getDiscount() == null ? 0L : item.getDiscount();
            long unitPrice = unitPrices.get(item.getMaterialId());
            so.getItems().add(SalesOrderItem.builder()
                    .salesOrder(so)
                    .material(material)
                    .qty(item.getQty())
                    .unitPrice(unitPrice)
                    .discount(itemDiscount)
                    .lineTotal(item.getQty() * unitPrice - itemDiscount)
                    .issuedQty(0L)
                    .returnedQty(0L)
                    .build());
        }
        so = salesOrderRepository.save(so);
        log.info("Sales order created - code={}, customer={}, grandTotal={}", so.getCode(), so.getCustomerName(), so.getGrandTotal());
        return salesOrderMapper.toResponse(so);
    }

    @Override
    @Transactional
    public SalesOrderResponse confirm(int id) {
        SalesOrder so = findOrThrow(id);
        if (so.getStatus() != SalesOrderStatus.PENDING) {
            throw new BusinessValidationException("Only PENDING sales order can be confirmed (current=" + so.getStatus() + ")");
        }
        // Kiểm tra tồn kho (không giữ chỗ — GIN confirm mới trừ, thiếu thì 409).
        // Tiệm nhỏ,Concurrency thấp: check-then-deduct, enforced lại ở GIN confirm.
        for (var line : so.getItems()) {
            long available = line.getMaterial().getStockQty();
            if (available < line.getQty()) {
                throw new BusinessValidationException("Insufficient stock for material '"
                        + line.getMaterial().getSku() + "': requested=" + line.getQty()
                        + ", available=" + available);
            }
        }
        so.setStatus(SalesOrderStatus.CONFIRMED);
        return salesOrderMapper.toResponse(salesOrderRepository.save(so));
    }

    @Override
    @Transactional
    public SalesOrderResponse cancel(int id) {
        SalesOrder so = findOrThrow(id);
        if (so.getStatus() != SalesOrderStatus.PENDING
                && so.getStatus() != SalesOrderStatus.CONFIRMED
                && so.getStatus() != SalesOrderStatus.DELIVERING) {
            throw new BusinessValidationException("Only PENDING/CONFIRMED/DELIVERING sales order can be cancelled (current=" + so.getStatus() + ")");
        }
        boolean hasConfirmedGins = !goodsIssueNoteRepository
                .findBySalesOrder_IdAndStatus(id, GoodsIssueStatus.CONFIRMED).isEmpty();
        if (hasConfirmedGins) {
            throw new BusinessValidationException("Sales order already has confirmed issues — return via EXPORT_RETURN GIN");
        }
        so.setStatus(SalesOrderStatus.CANCELLED);
        return salesOrderMapper.toResponse(salesOrderRepository.save(so));
    }

    private SalesOrder findOrThrow(int id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder not found with id: " + id));
    }

    private String nextCode() {
        long count = salesOrderRepository.count() + 1;
        String code;
        do {
            code = String.format("SO-2026-%04d", count++);
        } while (salesOrderRepository.existsByCode(code));
        return code;
    }
}
