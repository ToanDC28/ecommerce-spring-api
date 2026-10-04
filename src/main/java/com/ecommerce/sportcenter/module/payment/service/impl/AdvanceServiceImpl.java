package com.ecommerce.sportcenter.module.payment.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.payment.dto.request.CreatePaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.mapper.AdvanceMapper;
import com.ecommerce.sportcenter.module.payment.dto.request.ApplyAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.CreateAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.SearchAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.AdvanceResponse;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.entity.AdvanceDeposit;
import com.ecommerce.sportcenter.module.payment.entity.AdvanceStatus;
import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import com.ecommerce.sportcenter.module.payment.repository.AdvanceDepositRepository;
import com.ecommerce.sportcenter.module.payment.service.AdvanceService;
import com.ecommerce.sportcenter.module.payment.service.PaymentService;
import com.ecommerce.sportcenter.module.sales.repository.SalesOrderRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
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
public class AdvanceServiceImpl implements AdvanceService {

    private final AdvanceDepositRepository advanceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;
    private final AdvanceMapper advanceMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdvanceResponse> search(SearchAdvanceRequest request) {
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<AdvanceDeposit> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getCustomerId() != null) {
                predicates.add(builder.equal(root.get("customer").get("id"), request.getCustomerId()));
            }
            if (request.getWorkOrderId() != null) {
                predicates.add(builder.equal(root.get("workOrderId"), request.getWorkOrderId()));
            }
            if (request.getSalesOrderId() != null) {
                predicates.add(builder.equal(root.get("salesOrderId"), request.getSalesOrderId()));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<AdvanceDeposit>().build(request));
        var page = advanceRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(advanceMapper::toResponse).toList();
        return PageResponse.<AdvanceResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdvanceResponse getById(int id) {
        return advanceMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public AdvanceResponse create(CreateAdvanceRequest request, String username) {
        // Cọc bám theo hợp đồng/đơn (bắt buộc đúng 1 link) — customer suy ra từ đơn.
        boolean hasWo = request.getWorkOrderId() != null;
        boolean hasSo = request.getSalesOrderId() != null;
        if (hasWo == hasSo) {
            throw new BusinessValidationException("Advance must link exactly one work order or sales order");
        }
        Customer customer;
        String orderLabel;
        if (hasWo) {
            var wo = workOrderRepository.findById(request.getWorkOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + request.getWorkOrderId()));
            if (wo.getStatus() == com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus.CANCELLED
                    || wo.getStatus() == com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus.INVOICED) {
                throw new BusinessValidationException("Cannot record advance for " + wo.getStatus() + " work order");
            }
            if (wo.getCustomer() == null) {
                throw new BusinessValidationException("Work order has no customer link (dữ liệu cũ)");
            }
            customer = wo.getCustomer();
            orderLabel = wo.getCode();
        } else {
            var so = salesOrderRepository.findById(request.getSalesOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("SalesOrder not found with id: " + request.getSalesOrderId()));
            if (so.getStatus() == com.ecommerce.sportcenter.module.sales.entity.SalesOrderStatus.CANCELLED
                    || so.getStatus() == com.ecommerce.sportcenter.module.sales.entity.SalesOrderStatus.COMPLETED) {
                throw new BusinessValidationException("Cannot record advance for " + so.getStatus() + " sales order");
            }
            if (so.getCustomer() == null) {
                throw new BusinessValidationException("Sales order has no customer link (dữ liệu cũ)");
            }
            customer = so.getCustomer();
            orderLabel = so.getCode();
        }
        if (!customer.isActive()) {
            throw new BusinessValidationException("Customer '" + customer.getCode() + "' is inactive");
        }
        if (request.getMethod() == PaymentMethod.BANK_TRANSFER
                && (request.getTransactionRef() == null || request.getTransactionRef().isBlank())) {
            throw new BusinessValidationException("Transaction ref (mã CK) is required for BANK_TRANSFER");
        }
        AdvanceDeposit advance = AdvanceDeposit.builder()
                .code(nextCode())
                .customer(customer)
                .workOrderId(request.getWorkOrderId())
                .workOrderCode(hasWo ? orderLabel : null)
                .salesOrderId(request.getSalesOrderId())
                .salesOrderCode(hasWo ? null : orderLabel)
                .amount(request.getAmount())
                .method(request.getMethod())
                .transactionRef(request.getTransactionRef())
                .receivedBy(username)
                .status(AdvanceStatus.ACTIVE)
                .note(request.getNote())
                .build();
        advance = advanceRepository.save(advance);
        log.info("Advance recorded - code={}, order={}, customer={}, amount={}",
                advance.getCode(), orderLabel, customer.getCode(), advance.getAmount());
        return advanceMapper.toResponse(advance);
    }

    @Override
    @Transactional
    public PaymentResponse apply(int advanceId, ApplyAdvanceRequest request) {
        AdvanceDeposit advance = findOrThrow(advanceId);
        if (advance.getStatus() != AdvanceStatus.ACTIVE) {
            throw new BusinessValidationException("Only ACTIVE advances can be applied (current=" + advance.getStatus() + ")");
        }
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));
        // Cọc theo đơn nào thì chỉ cấn vào invoice của đúng đơn đó.
        if (advance.getWorkOrderId() != null) {
            if (!advance.getWorkOrderId().equals(invoice.getWorkOrderId())) {
                throw new BusinessValidationException("Advance " + advance.getCode()
                        + " belongs to work order " + advance.getWorkOrderCode()
                        + " and cannot offset other invoices");
            }
        } else {
            if (!advance.getSalesOrderId().equals(invoice.getSoId())) {
                throw new BusinessValidationException("Advance " + advance.getCode()
                        + " belongs to sales order " + advance.getSalesOrderCode()
                        + " and cannot offset other invoices");
            }
        }
        long remaining = invoice.getGrandTotal() - invoice.getPaidAmount();
        if (advance.getAmount() > remaining) {
            throw new BusinessValidationException("Advance amount (" + advance.getAmount()
                    + ") exceeds invoice remaining (" + remaining + ")");
        }
        // Ghi payment cấn trừ (giữ method/ref gốc, note rõ nguồn cọc).
        var paymentRequest = new CreatePaymentRequest(
                advance.getAmount(),
                advance.getMethod(),
                advance.getTransactionRef(),
                "Cấn trừ cọc " + advance.getCode());
        // Dùng receivedBy của người apply (không phải người thu cọc).
        PaymentResponse payment = paymentService.pay(invoice.getId(), paymentRequest,
                "advance:" + advance.getCode());
        advance.setStatus(AdvanceStatus.APPLIED);
        advance.setAppliedInvoiceId(invoice.getId());
        advanceRepository.save(advance);
        log.info("Advance applied - code={}, invoice={}", advance.getCode(), invoice.getCode());
        return payment;
    }

    @Override
    @Transactional
    public int autoApply(int invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        List<AdvanceDeposit> actives;
        if (invoice.getWorkOrderId() != null) {
            actives = advanceRepository.findByWorkOrderIdAndStatusOrderByIdAsc(
                    invoice.getWorkOrderId(), AdvanceStatus.ACTIVE);
        } else if (invoice.getSoId() != null) {
            actives = advanceRepository.findBySalesOrderIdAndStatusOrderByIdAsc(
                    invoice.getSoId(), AdvanceStatus.ACTIVE);
        } else {
            return 0;
        }
        int applied = 0;
        for (var advance : actives) {
            long remaining = invoice.getGrandTotal() - invoice.getPaidAmount();
            if (remaining <= 0) {
                break;
            }
            if (advance.getAmount() > remaining) {
                log.info("Advance kept - code={}, amount={} exceeds invoice remaining={}",
                        advance.getCode(), advance.getAmount(), remaining);
                continue;
            }
            apply(advance.getId(), new ApplyAdvanceRequest(invoiceId));
            applied++;
        }
        return applied;
    }

    @Override
    @Transactional
    public AdvanceResponse cancel(int id) {        AdvanceDeposit advance = findOrThrow(id);
        if (advance.getStatus() != AdvanceStatus.ACTIVE) {
            throw new BusinessValidationException("Only ACTIVE advances can be cancelled (current=" + advance.getStatus() + ")");
        }
        advance.setStatus(AdvanceStatus.CANCELLED);
        return advanceMapper.toResponse(advanceRepository.save(advance));
    }

    private AdvanceDeposit findOrThrow(int id) {
        return advanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdvanceDeposit not found with id: " + id));
    }

    private String nextCode() {
        long count = advanceRepository.count() + 1;
        String code;
        do {
            code = String.format("ADV-2026-%04d", count++);
        } while (advanceRepository.existsByCode(code));
        return code;
    }
}
