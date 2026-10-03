package com.ecommerce.sportcenter.module.payment.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.payment.dto.mapper.PaymentMapper;
import com.ecommerce.sportcenter.module.payment.dto.request.CreatePaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.SearchPaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentSummaryResponse;
import com.ecommerce.sportcenter.module.payment.entity.Payment;
import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import com.ecommerce.sportcenter.module.payment.entity.PaymentStatus;
import com.ecommerce.sportcenter.module.payment.repository.PaymentRepository;
import com.ecommerce.sportcenter.module.payment.service.PaymentService;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse pay(int invoiceId, CreatePaymentRequest request, String username) {
        // Idempotent double-submit: cùng transactionRef trả về payment đã có.
        if (request.getTransactionRef() != null && !request.getTransactionRef().isBlank()) {
            var existing = paymentRepository.findByTransactionRef(request.getTransactionRef().trim());
            if (existing.isPresent()) {
                log.info("Duplicate payment submit - ref={}, returning existing id={}",
                        request.getTransactionRef(), existing.get().getId());
                return paymentMapper.toResponse(existing.get());
            }
        }
        if (request.getMethod() == PaymentMethod.BANK_TRANSFER
                && (request.getTransactionRef() == null || request.getTransactionRef().isBlank())) {
            throw new BusinessValidationException("Transaction ref (mã CK) is required for BANK_TRANSFER");
        }
        if (request.getMethod() == PaymentMethod.CASH && (username == null || username.isBlank())) {
            throw new BusinessValidationException("Cash payment requires the receiver (receivedBy)");
        }
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        if (invoice.getStatus() != InvoiceStatus.ISSUED
                && invoice.getStatus() != InvoiceStatus.PARTIAL
                && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BusinessValidationException("Only ISSUED/PARTIAL/OVERDUE invoices can receive payments (current=" + invoice.getStatus() + ")");
        }
        long remaining = invoice.getGrandTotal() - invoice.getPaidAmount();
        if (request.getAmount() > remaining) {
            throw new BusinessValidationException("Overpay rejected: amount=" + request.getAmount()
                    + ", remaining=" + remaining);
        }
        Payment payment = Payment.builder()
                .code(nextCode())
                .invoice(invoice)
                .amount(request.getAmount())
                .method(request.getMethod())
                .paymentDate(LocalDate.now())
                .status(PaymentStatus.SUCCESS)
                .transactionRef(request.getTransactionRef() == null ? null : request.getTransactionRef().trim())
                .receivedBy(username)
                .note(request.getNote())
                .build();
        payment = paymentRepository.save(payment);

        invoice.setPaidAmount(invoice.getPaidAmount() + request.getAmount());
        invoice.setStatus(invoice.getPaidAmount() >= invoice.getGrandTotal()
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);
        invoiceRepository.save(invoice);

        log.info("Payment recorded - code={}, invoice={}, amount={}, method={}, by={}",
                payment.getCode(), invoice.getCode(), request.getAmount(), request.getMethod(), username);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listByInvoice(int invoiceId) {
        invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));
        return paymentRepository.findByInvoice_IdOrderByIdAsc(invoiceId).stream()
                .map(paymentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> search(SearchPaymentRequest request) {
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<Payment> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getInvoiceId() != null) {
                predicates.add(builder.equal(root.get("invoice").get("id"), request.getInvoiceId()));
            }
            if (request.getMethod() != null) {
                predicates.add(builder.equal(root.get("method"), request.getMethod()));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            if (request.getFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("paymentDate"), request.getFrom()));
            }
            if (request.getTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("paymentDate"), request.getTo()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<Payment>().build(request));
        var page = paymentRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(paymentMapper::toResponse).toList();
        return PageResponse.<PaymentResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse summary(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        if (end.isBefore(start)) {
            throw new BusinessValidationException("To date must be >= from date");
        }
        Specification<Payment> spec = (root, query, builder) -> builder.and(
                builder.equal(root.get("status"), PaymentStatus.SUCCESS),
                builder.greaterThanOrEqualTo(root.get("paymentDate"), start),
                builder.lessThanOrEqualTo(root.get("paymentDate"), end));
        var payments = paymentRepository.findAll(spec);

        // Gom theo ngày + method cho đối soát cuối ngày.
        Map<LocalDate, Map<PaymentMethod, long[]>> grouped = new TreeMap<>();
        for (var p : payments) {
            grouped.computeIfAbsent(p.getPaymentDate(), d -> new java.util.EnumMap<>(PaymentMethod.class))
                    .computeIfAbsent(p.getMethod(), m -> new long[2])[0] += p.getAmount();
            grouped.get(p.getPaymentDate()).get(p.getMethod())[1] += 1;
        }
        var lines = new java.util.ArrayList<PaymentSummaryResponse.DayLine>();
        long totalCash = 0L, totalBank = 0L;
        for (var e : grouped.entrySet()) {
            for (var m : e.getValue().entrySet()) {
                lines.add(PaymentSummaryResponse.DayLine.builder()
                        .date(e.getKey()).method(m.getKey())
                        .totalAmount(m.getValue()[0]).count(m.getValue()[1]).build());
                if (m.getKey() == PaymentMethod.CASH) {
                    totalCash += m.getValue()[0];
                } else {
                    totalBank += m.getValue()[0];
                }
            }
        }
        lines.sort(Comparator.comparing(PaymentSummaryResponse.DayLine::getDate));
        return PaymentSummaryResponse.builder()
                .from(start).to(end).lines(lines)
                .totalCash(totalCash).totalBank(totalBank)
                .grandTotal(totalCash + totalBank).count(payments.size())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse refund(int paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessValidationException("Only SUCCESS payments can be refunded (current=" + payment.getStatus() + ")");
        }
        Invoice invoice = invoiceRepository.findByIdForUpdate(payment.getInvoice().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + payment.getInvoice().getId()));
        boolean wasPaid = invoice.getStatus() == InvoiceStatus.PAID;
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        invoice.setPaidAmount(Math.max(0L, invoice.getPaidAmount() - payment.getAmount()));
        if (invoice.getPaidAmount() == 0) {
            invoice.setStatus(wasPaid ? InvoiceStatus.REFUNDED : InvoiceStatus.ISSUED);
        } else if (invoice.getPaidAmount() >= invoice.getGrandTotal()) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }
        invoiceRepository.save(invoice);
        log.info("Payment refunded - code={}, invoice={}", payment.getCode(), invoice.getCode());
        return paymentMapper.toResponse(payment);
    }

    private String nextCode() {
        long count = paymentRepository.count() + 1;
        String code;
        do {
            code = String.format("PAY-2026-%04d", count++);
        } while (paymentRepository.existsByCode(code));
        return code;
    }
}
