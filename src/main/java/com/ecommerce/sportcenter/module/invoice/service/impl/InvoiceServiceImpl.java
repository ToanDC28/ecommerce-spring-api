package com.ecommerce.sportcenter.module.invoice.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.invoice.dto.mapper.InvoiceMapper;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreatePurchaseInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateSalesInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateWorkInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.SearchInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceItem;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.invoice.service.InvoiceService;
import com.ecommerce.sportcenter.module.payment.service.AdvanceService;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrder;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderMaterialRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private static final Set<Integer> VAT_ALLOWED = Set.of(0, 8, 10);

    private final InvoiceRepository invoiceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderMaterialRepository workOrderMaterialRepository;
    private final CustomerRepository customerRepository;
    private final AdvanceService advanceService;
    private final InvoiceMapper invoiceMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> search(SearchInvoiceRequest request) {
        log.info("Search invoices - keyword={}, type={}, status={}", request.getKeyword(), request.getType(), request.getStatus());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<Invoice> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("code")), like),
                        builder.like(builder.lower(root.get("customerName")), like),
                        builder.like(builder.lower(root.get("supplierName")), like),
                        builder.like(builder.lower(root.get("workOrderCode")), like),
                        builder.like(builder.lower(root.get("soCode")), like)));
            }
            if (request.getType() != null) {
                predicates.add(builder.equal(root.get("type"), request.getType()));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<Invoice>().build(request));
        var page = invoiceRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(invoiceMapper::toResponse).toList();
        return PageResponse.<InvoiceResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(int id) {
        log.info("Get invoice by id - id={}", id);
        return invoiceMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public InvoiceResponse createWorkInvoice(CreateWorkInvoiceRequest request) {
        int vatRate = request.getVatRate() == null ? 10 : request.getVatRate();
        // TODO: VAT does not need now
        if (!VAT_ALLOWED.contains(vatRate)) {
            throw new BusinessValidationException("VAT rate must be one of 0, 8, 10");
        }
        WorkOrder wo = workOrderRepository.findById(request.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + request.getWorkOrderId()));
        if (wo.getStatus() != WorkOrderStatus.DONE) {
            throw new BusinessValidationException("Only DONE work order can be invoiced (current=" + wo.getStatus() + ")");
        }
        long discount = request.getDiscountAmount() == null ? 0L : request.getDiscountAmount();
        if (discount < 0) {
            throw new BusinessValidationException("Discount must be >= 0");
        }
        // Validate có nội dung xuất hóa đơn trước khi lưu (tránh mồ côi invoice rỗng).
        var woLines = workOrderMaterialRepository.findByWorkOrder_Id(wo.getId());
        boolean hasActual = woLines.stream().anyMatch(l -> l.getQtyActual() > 0);
        if (!hasActual && wo.getLaborCost() <= 0 && wo.getOverheadCost() <= 0) {
            throw new BusinessValidationException("Work order has no actual materials or labor to invoice");
        }

        Invoice invoice = Invoice.builder()
                .code(nextCode())
                .type(InvoiceType.WORK)
                .workOrderId(wo.getId())
                .workOrderCode(wo.getCode())
                .customerName(wo.getCustomerName())
                .customerPhone(wo.getCustomerPhone())
                .customerId(wo.getCustomer() == null ? null : wo.getCustomer().getId())
                .issueDate(LocalDate.now())
                .dueDate(request.getDueDays() == null ? null : LocalDate.now().plusDays(request.getDueDays()))
                .discountAmount(discount)
                .vatRate(vatRate)
                .status(InvoiceStatus.DRAFT)
                .build();
        invoice = invoiceRepository.save(invoice);

        long subTotal = 0L;
        // vật tư thực tế (qtyActual > 0)
        for (var line : woLines) {
            if (line.getQtyActual() <= 0) {
                continue;
            }
            long unitPrice = line.getUnitSellPrice() == null ? line.getUnitCost() : line.getUnitSellPrice();
            long lineTotal = line.getQtyActual() * unitPrice;
            subTotal += lineTotal;
            invoice.getItems().add(InvoiceItem.builder()
                    .invoice(invoice)
                    .material(line.getMaterial())
                    .description(line.getMaterial().getSku() + " - " + line.getMaterial().getName())
                    .qty(line.getQtyActual())
                    .unitPrice(unitPrice)
                    .discount(0L)
                    .lineTotal(lineTotal)
                    .build());
        }
        // nhân công
        if (wo.getLaborCost() > 0) {
            subTotal += wo.getLaborCost();
            invoice.getItems().add(InvoiceItem.builder()
                    .invoice(invoice)
                    .description("Nhân công sửa chữa / gia công (" + wo.getCode() + ")")
                    .qty(1L).unitPrice(wo.getLaborCost()).discount(0L).lineTotal(wo.getLaborCost())
                    .build());
        }
        // chi phí chung
        if (wo.getOverheadCost() > 0) {
            subTotal += wo.getOverheadCost();
            invoice.getItems().add(InvoiceItem.builder()
                    .invoice(invoice)
                    .description("Chi phí chung / phụ phí (" + wo.getCode() + ")")
                    .qty(1L).unitPrice(wo.getOverheadCost()).discount(0L).lineTotal(wo.getOverheadCost())
                    .build());
        }
        if (invoice.getItems().isEmpty()) {
            // Không thể xảy ra (đã check hasActual/labor/overhead ở trên) — giữ làm guard.
            throw new BusinessValidationException("Work order has no actual materials or labor to invoice");
        }
        long vatAmount = (subTotal - discount) * vatRate / 100;
        invoice.setSubTotal(subTotal);
        invoice.setVatAmount(vatAmount);
        invoice.setGrandTotal(subTotal - discount + vatAmount);
        invoice = invoiceRepository.save(invoice);

        // khóa WO
        wo.setStatus(WorkOrderStatus.INVOICED);
        workOrderRepository.save(wo);

        log.info("Created WORK invoice - code={}, workOrder={}, grandTotal={}", invoice.getCode(), wo.getCode(), invoice.getGrandTotal());
        // Cọc của đúng WO tự trừ như tiền trả 1 phần (cọc nào vừa thì apply hết, lớn hơn thì giữ lại).
        int autoApplied = advanceService.autoApply(invoice.getId());
        if (autoApplied > 0) {
            log.info("Auto-applied {} advances to invoice {}", autoApplied, invoice.getCode());
        }
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse createPurchaseInvoice(CreatePurchaseInvoiceRequest request) {
        int vatRate = request.getVatRate() == null ? 10 : request.getVatRate();
        if (!VAT_ALLOWED.contains(vatRate)) {
            throw new BusinessValidationException("VAT rate must be one of 0, 8, 10");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessValidationException("Invoice items are required");
        }
        for (var line : request.getItems()) {
            if (line.getQty() <= 0) {
                throw new BusinessValidationException("Quantity must be >= 1");
            }
        }
        Invoice invoice = Invoice.builder()
                .code(nextCode())
                .type(InvoiceType.PURCHASE)
                .supplierId(request.getSupplierId())
                .supplierName(request.getSupplierName())
                .refCode(request.getRefCode())
                .issueDate(LocalDate.now())
                .discountAmount(0L)
                .vatRate(vatRate)
                .status(InvoiceStatus.DRAFT)
                .build();
        invoice = invoiceRepository.save(invoice);

        long subTotal = 0L;
        for (var line : request.getItems()) {
            long lineTotal = line.getQty() * line.getUnitCost();
            subTotal += lineTotal;
            invoice.getItems().add(InvoiceItem.builder()
                    .invoice(invoice)
                    .description(line.getDescription())
                    .qty(line.getQty())
                    .unitPrice(line.getUnitCost())
                    .discount(0L)
                    .lineTotal(lineTotal)
                    .build());
        }
        // link material entities is optional here — descriptions are snapshots from GRN.
        // Material links can be backfilled when the manual purchase-invoice endpoint lands.
        long vatAmount = subTotal * vatRate / 100;
        invoice.setSubTotal(subTotal);
        invoice.setVatAmount(vatAmount);
        invoice.setGrandTotal(subTotal + vatAmount);
        invoice = invoiceRepository.save(invoice);
        log.info("Created PURCHASE invoice - code={}, ref={}, grandTotal={}", invoice.getCode(), request.getRefCode(), invoice.getGrandTotal());
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse createSalesInvoice(CreateSalesInvoiceRequest request) {
        int vatRate = request.getVatRate() == null ? 0 : request.getVatRate();
        if (!VAT_ALLOWED.contains(vatRate)) {
            throw new BusinessValidationException("VAT rate must be one of 0, 8, 10");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessValidationException("Invoice items are required");
        }
        for (var line : request.getItems()) {
            if (line.getQty() <= 0) {
                throw new BusinessValidationException("Quantity must be >= 1");
            }
        }
        // Invoice bán luôn link mã KH (UI tạo khách trước); snapshot tên/SĐT tự lấy theo link.
        com.ecommerce.sportcenter.module.customer.entity.Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        if (!customer.isActive()) {
            throw new BusinessValidationException("Customer '" + customer.getCode() + "' is inactive");
        }
        String customerName = request.getCustomerName() == null || request.getCustomerName().isBlank()
                ? customer.getName() : request.getCustomerName();
        String customerPhone = request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()
                ? customer.getPhone() : request.getCustomerPhone();
        Invoice invoice = Invoice.builder()
                .code(nextCode())
                .type(InvoiceType.SALES)
                .soId(request.getSoId())
                .soCode(request.getSoCode())
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerId(customer.getId())
                .refCode(request.getRefCode())
                .issueDate(LocalDate.now())
                .discountAmount(0L)
                .vatRate(vatRate)
                .status(InvoiceStatus.DRAFT)
                .build();
        invoice = invoiceRepository.save(invoice);

        long subTotal = 0L;
        for (var line : request.getItems()) {
            long lineTotal = line.getQty() * line.getUnitPrice();
            subTotal += lineTotal;
            invoice.getItems().add(InvoiceItem.builder()
                    .invoice(invoice)
                    .description(line.getDescription())
                    .qty(line.getQty())
                    .unitPrice(line.getUnitPrice())
                    .discount(0L)
                    .lineTotal(lineTotal)
                    .build());
        }
        long vatAmount = subTotal * vatRate / 100;
        invoice.setSubTotal(subTotal);
        invoice.setVatAmount(vatAmount);
        invoice.setGrandTotal(subTotal + vatAmount);
        invoice = invoiceRepository.save(invoice);
        log.info("Created SALES invoice - code={}, ref={}, grandTotal={}", invoice.getCode(), request.getRefCode(), invoice.getGrandTotal());
        // Cọc của đúng SO (nếu có) tự trừ như tiền trả 1 phần.
        int autoApplied = advanceService.autoApply(invoice.getId());
        if (autoApplied > 0) {
            log.info("Auto-applied {} advances to invoice {}", autoApplied, invoice.getCode());
        }
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveInvoice(String refCode, InvoiceType type) {
        if (refCode == null) {
            return false;
        }
        return invoiceRepository.existsByRefCodeAndTypeAndStatusNot(refCode, type, InvoiceStatus.CANCELLED);
    }

    @Override
    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * *")
    public int markOverdue() {
        var overdue = invoiceRepository.findByStatusInAndDueDateBefore(
                java.util.List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL), LocalDate.now());
        for (var invoice : overdue) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
        }
        if (!overdue.isEmpty()) {
            invoiceRepository.saveAll(overdue);
            log.info("Marked {} invoices OVERDUE", overdue.size());
        }
        return overdue.size();
    }

    @Override
    @Transactional
    public InvoiceResponse issue(int id) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessValidationException("Only DRAFT invoice can be issued");
        }
        invoice.setStatus(InvoiceStatus.ISSUED);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public InvoiceResponse cancel(int id) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getPaidAmount() > 0) {
            throw new BusinessValidationException("Cannot cancel invoice with payments (refund instead)");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessValidationException("Cannot cancel PAID invoice (refund instead)");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        // mở lại WO nếu là WORK
        if (invoice.getType() == InvoiceType.WORK && invoice.getWorkOrderId() != null) {
            workOrderRepository.findById(invoice.getWorkOrderId()).ifPresent(wo -> {
                if (wo.getStatus() == WorkOrderStatus.INVOICED) {
                    wo.setStatus(WorkOrderStatus.DONE);
                    workOrderRepository.save(wo);
                }
            });
        }
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    private Invoice findOrThrow(int id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    private String nextCode() {
        long count = invoiceRepository.count() + 1;
        String code;
        do {
            code = String.format("INV-2026-%05d", count++);
        } while (invoiceRepository.existsByCode(code));
        return code;
    }
}
