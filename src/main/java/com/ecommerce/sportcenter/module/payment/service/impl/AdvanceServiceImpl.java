package com.ecommerce.sportcenter.module.payment.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.payment.dto.request.CreatePaymentRequest;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
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
public class AdvanceServiceImpl implements AdvanceService {

    private final AdvanceDepositRepository advanceRepository;
    private final CustomerRepository customerRepository;
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
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
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
                .salesOrderId(request.getSalesOrderId())
                .amount(request.getAmount())
                .method(request.getMethod())
                .transactionRef(request.getTransactionRef())
                .receivedBy(username)
                .status(AdvanceStatus.ACTIVE)
                .note(request.getNote())
                .build();
        advance = advanceRepository.save(advance);
        log.info("Advance recorded - code={}, customer={}, amount={}", advance.getCode(), customer.getCode(), advance.getAmount());
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
        if (invoice.getCustomerId() == null || invoice.getCustomerId() != advance.getCustomer().getId()) {
            throw new BusinessValidationException("Advance belongs to customer " + advance.getCustomer().getCode()
                    + " but invoice is for another customer");
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
    public AdvanceResponse cancel(int id) {
        AdvanceDeposit advance = findOrThrow(id);
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
