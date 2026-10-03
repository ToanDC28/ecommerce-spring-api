package com.ecommerce.sportcenter.module.report.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollRepository;
import com.ecommerce.sportcenter.module.report.dto.response.DebtRow;
import com.ecommerce.sportcenter.module.report.dto.response.ProfitResponse;
import com.ecommerce.sportcenter.module.report.dto.response.RevenueByTypeResponse;
import com.ecommerce.sportcenter.module.report.dto.response.RevenuePoint;
import com.ecommerce.sportcenter.module.report.dto.response.SalaryCostResponse;
import com.ecommerce.sportcenter.module.report.dto.response.StockValueResponse;
import com.ecommerce.sportcenter.module.report.dto.response.TopMaterialRow;
import com.ecommerce.sportcenter.module.report.dto.response.WorkOrderProfitRow;
import com.ecommerce.sportcenter.module.report.service.ReportService;
import com.ecommerce.sportcenter.module.sales.repository.SalesOrderItemRepository;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderMaterialRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final List<InvoiceStatus> OPEN_STATUSES =
            List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE);
    private static final List<InvoiceStatus> REVENUE_STATUSES =
            List.of(InvoiceStatus.PAID, InvoiceStatus.PARTIAL);

    private final InvoiceRepository invoiceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderMaterialRepository workOrderMaterialRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final MaterialRepository materialRepository;
    private final PayrollRepository payrollRepository;

    @Override
    public List<RevenuePoint> revenue(LocalDate from, LocalDate to, String groupBy) {
        var range = defaultRange(from, to);
        boolean byMonth = "month".equalsIgnoreCase(groupBy);
        DateTimeFormatter fmt = byMonth ? DateTimeFormatter.ofPattern("yyyy-MM") : DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, long[]> grouped = new TreeMap<>();
        for (var inv : paidInvoices(range.from(), range.to())) {
            String label = inv.getIssueDate().format(fmt);
            long[] acc = grouped.computeIfAbsent(label, k -> new long[2]);
            acc[0] += inv.getGrandTotal();
            acc[1] += 1;
        }
        return grouped.entrySet().stream()
                .map(e -> RevenuePoint.builder().label(e.getKey()).total(e.getValue()[0]).count(e.getValue()[1]).build())
                .toList();
    }

    @Override
    public RevenueByTypeResponse revenueByType(LocalDate from, LocalDate to) {
        var range = defaultRange(from, to);
        long workTotal = 0, salesTotal = 0, workCount = 0, salesCount = 0;
        for (var inv : paidInvoices(range.from(), range.to())) {
            if (inv.getType() == InvoiceType.WORK) {
                workTotal += inv.getGrandTotal();
                workCount++;
            } else {
                salesTotal += inv.getGrandTotal();
                salesCount++;
            }
        }
        return RevenueByTypeResponse.builder()
                .workTotal(workTotal).workCount(workCount)
                .salesTotal(salesTotal).salesCount(salesCount)
                .grandTotal(workTotal + salesTotal).build();
    }

    @Override
    public List<TopMaterialRow> topMaterials(int limit, LocalDate from, LocalDate to) {
        Map<Integer, long[]> consumed = new HashMap<>(); // materialId -> [woQty, soQty]
        Map<Integer, Info> info = new HashMap<>();
        for (var line : workOrderMaterialRepository.findAll()) {
            if (line.getQtyActual() <= 0 || !inCreatedRange(line.getCreatedDate(), from, to)) {
                continue;
            }
            var m = line.getMaterial();
            consumed.computeIfAbsent(m.getId(), k -> new long[2])[0] += line.getQtyActual();
            info.putIfAbsent(m.getId(), new Info(m.getSku(), m.getName(), m.getUnit()));
        }
        for (var item : salesOrderItemRepository.findAll()) {
            var soDate = item.getSalesOrder() == null ? null : item.getSalesOrder().getOrderDate();
            if ((from != null && (soDate == null || soDate.isBefore(from)))
                    || (to != null && (soDate == null || soDate.isAfter(to)))) {
                continue;
            }
            var m = item.getMaterial();
            consumed.computeIfAbsent(m.getId(), k -> new long[2])[1] += item.getQty();
            info.putIfAbsent(m.getId(), new Info(m.getSku(), m.getName(), m.getUnit()));
        }
        return consumed.entrySet().stream()
                .map(e -> {
                    var i = info.get(e.getKey());
                    long wo = e.getValue()[0], so = e.getValue()[1];
                    return TopMaterialRow.builder()
                            .materialId(e.getKey()).sku(i.sku()).name(i.name()).unit(i.unit())
                            .consumedQty(wo).soldQty(so).totalQty(wo + so).build();
                })
                .sorted(Comparator.comparingLong(TopMaterialRow::getTotalQty).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    public StockValueResponse stockValue() {
        long totalValue = 0L;
        var lowStock = new java.util.ArrayList<StockValueResponse.LowStockRow>();
        var materials = materialRepository.findAll();
        for (var m : materials) {
            totalValue += m.getStockQty() * m.getCostPrice();
            if (m.isActive() && m.getStockQty() <= m.getMinStock()) {
                lowStock.add(StockValueResponse.LowStockRow.builder()
                        .materialId(m.getId()).sku(m.getSku()).name(m.getName())
                        .stockQty(m.getStockQty()).minStock(m.getMinStock()).build());
            }
        }
        lowStock.sort(Comparator.comparingLong(StockValueResponse.LowStockRow::getStockQty));
        return StockValueResponse.builder()
                .totalValue(totalValue).materialCount(materials.size()).lowStock(lowStock).build();
    }

    @Override
    public List<DebtRow> supplierDebt() {
        Map<String, DebtAcc> grouped = new HashMap<>();
        for (var inv : openInvoices(InvoiceType.PURCHASE)) {
            long owed = inv.getGrandTotal() - inv.getPaidAmount();
            if (owed <= 0) {
                continue;
            }
            String key = inv.getSupplierName() == null ? "?" : inv.getSupplierName();
            grouped.computeIfAbsent(key, k -> new DebtAcc(
                    inv.getSupplierName(), null)).add(owed);
        }
        return toDebtRows(grouped);
    }

    @Override
    public List<DebtRow> customerDebt() {
        Map<String, DebtAcc> grouped = new HashMap<>();
        for (var inv : openInvoices(null)) { // WORK + SALES
            if (inv.getType() == InvoiceType.PURCHASE) {
                continue;
            }
            long owed = inv.getGrandTotal() - inv.getPaidAmount();
            if (owed <= 0) {
                continue;
            }
            // Ưu tiên mã KH master, fallback snapshot tên+SĐT (khách vãng lai).
            String key = inv.getCustomerId() != null ? "ID:" + inv.getCustomerId()
                    : "SNAPSHOT:" + (inv.getCustomerName() == null ? "?" : inv.getCustomerName())
                    + "|" + (inv.getCustomerPhone() == null ? "" : inv.getCustomerPhone());
            grouped.computeIfAbsent(key, k -> new DebtAcc(
                    inv.getCustomerId(), inv.getCustomerName(), inv.getCustomerPhone())).add(owed);
        }
        return toDebtRows(grouped);
    }

    @Override
    public SalaryCostResponse salaryCost(String period) {
        if (period == null || !period.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new BusinessValidationException("Period must be YYYY-MM");
        }
        var payrolls = payrollRepository.findByPeriod(period);
        long gross = 0L, net = 0L;
        var staff = new java.util.HashSet<Integer>();
        for (var p : payrolls) {
            if (p.getStatus() != com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus.PAID) {
                continue;
            }
            gross += p.getGrossPay();
            net += p.getNetPay();
            staff.add(p.getStaff().getId());
        }
        return SalaryCostResponse.builder()
                .period(period).headcount(staff.size()).totalNet(net).totalGross(gross).build();
    }

    @Override
    public ProfitResponse profit(LocalDate from, LocalDate to) {
        var range = defaultRange(from, to);
        var byType = revenueByType(range.from(), range.to());
        long materialCost = 0L;
        // WO actual cost theo ngày tạo WO trong kỳ
        for (var line : workOrderMaterialRepository.findAll()) {
            if (line.getQtyActual() <= 0 || !inCreatedRange(line.getCreatedDate(), range.from(), range.to())) {
                continue;
            }
            materialCost += line.getQtyActual() * line.getUnitCost();
        }
        // SO sold cost theo giá vốn HIỆN TẠI (xấp xỉ — snapshot cost theo thời điểm bán để phase 2)
        for (var item : salesOrderItemRepository.findAll()) {
            var soDate = item.getSalesOrder() == null ? null : item.getSalesOrder().getOrderDate();
            if (soDate == null || soDate.isBefore(range.from()) || soDate.isAfter(range.to())) {
                continue;
            }
            materialCost += item.getQty() * item.getMaterial().getCostPrice();
        }
        // Lương PAID có kỳ giao với range (so sánh YYYY-MM chuỗi)
        String fromYm = range.from().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String toYm = range.to().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long salaryCost = 0L;
        for (var p : payrollRepository.findAll()) {
            if (p.getStatus() != com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus.PAID) {
                continue;
            }
            if (p.getPeriod().compareTo(fromYm) >= 0 && p.getPeriod().compareTo(toYm) <= 0) {
                salaryCost += p.getNetPay();
            }
        }
        long revenue = byType.getGrandTotal();
        return ProfitResponse.builder()
                .from(range.from()).to(range.to())
                .revenue(revenue).workRevenue(byType.getWorkTotal()).salesRevenue(byType.getSalesTotal())
                .materialCost(materialCost).salaryCost(salaryCost)
                .profit(revenue - materialCost - salaryCost).build();
    }

    @Override
    public List<WorkOrderProfitRow> workOrderProfit(LocalDate from, LocalDate to) {
        Specification<com.ecommerce.sportcenter.module.workorder.entity.WorkOrder> spec = (root, query, builder) -> {
            if (from == null && to == null) {
                return builder.conjunction();
            }
            var created = root.get("createdDate").as(Date.class);
            if (from != null && to != null) {
                return builder.between(created, startOfDay(from), endOfDay(to));
            }
            if (from != null) {
                return builder.greaterThanOrEqualTo(created, startOfDay(from));
            }
            return builder.lessThanOrEqualTo(created, endOfDay(to));
        };
        var rows = new java.util.ArrayList<WorkOrderProfitRow>();
        for (var wo : workOrderRepository.findAll(spec)) {
            long materialCost = 0L;
            if (wo.getMaterials() != null) {
                for (var line : wo.getMaterials()) {
                    materialCost += line.getQtyActual() * line.getUnitCost();
                }
            }
            long totalCost = materialCost + wo.getLaborCost() + wo.getOverheadCost();
            long invoicedTotal = invoiceRepository.findByWorkOrderId(wo.getId()).stream()
                    .filter(inv -> inv.getStatus() != com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus.CANCELLED)
                    .mapToLong(com.ecommerce.sportcenter.module.invoice.entity.Invoice::getGrandTotal).sum();
            Long revenue = wo.getAgreedPrice() != null ? wo.getAgreedPrice()
                    : (invoicedTotal > 0 ? invoicedTotal : null);
            rows.add(WorkOrderProfitRow.builder()
                    .workOrderId(wo.getId()).code(wo.getCode()).type(wo.getType()).status(wo.getStatus())
                    .customerName(wo.getCustomerName()).agreedPrice(wo.getAgreedPrice())
                    .invoicedTotal(invoicedTotal).materialActualCost(materialCost)
                    .laborCost(wo.getLaborCost()).overheadCost(wo.getOverheadCost())
                    .totalCost(totalCost).revenue(revenue)
                    .margin(revenue == null ? null : revenue - totalCost).build());
        }
        rows.sort(Comparator.comparing(WorkOrderProfitRow::getCode).reversed());
        return rows;
    }

    private List<com.ecommerce.sportcenter.module.invoice.entity.Invoice> paidInvoices(LocalDate from, LocalDate to) {
        Specification<com.ecommerce.sportcenter.module.invoice.entity.Invoice> spec = (root, query, builder) -> builder.and(
                root.get("type").in(InvoiceType.WORK, InvoiceType.SALES),
                root.get("status").in(REVENUE_STATUSES),
                builder.greaterThanOrEqualTo(root.get("issueDate"), from),
                builder.lessThanOrEqualTo(root.get("issueDate"), to));
        return invoiceRepository.findAll(spec);
    }

    private List<com.ecommerce.sportcenter.module.invoice.entity.Invoice> openInvoices(InvoiceType type) {
        Specification<com.ecommerce.sportcenter.module.invoice.entity.Invoice> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            predicates.add(root.get("status").in(OPEN_STATUSES));
            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return invoiceRepository.findAll(spec);
    }

    private Range defaultRange(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (end.isBefore(start)) {
            throw new BusinessValidationException("To date must be >= from date");
        }
        return new Range(start, end);
    }

    private boolean inCreatedRange(Date createdDate, LocalDate from, LocalDate to) {
        if (createdDate == null) {
            return from == null && to == null;
        }
        var instant = createdDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return (from == null || !instant.isBefore(from)) && (to == null || !instant.isAfter(to));
    }

    private Date startOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date endOfDay(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private List<DebtRow> toDebtRows(Map<String, DebtAcc> grouped) {
        return grouped.entrySet().stream()
                .map(e -> DebtRow.builder().customerId(e.getValue().customerId())
                        .name(e.getValue().name()).phone(e.getValue().phone())
                        .invoiceCount(e.getValue().count()).totalOwed(e.getValue().total()).build())
                .sorted(Comparator.comparingLong(DebtRow::getTotalOwed).reversed())
                .toList();
    }

    private record Range(LocalDate from, LocalDate to) {
    }

    private record Info(String sku, String name,
                        com.ecommerce.sportcenter.module.material.entity.MaterialUnit unit) {
    }

    private static class DebtAcc {
        private final Integer customerId;
        private final String name;
        private final String phone;
        private long count;
        private long total;

        DebtAcc(String name, String phone) {
            this(null, name, phone);
        }

        DebtAcc(Integer customerId, String name, String phone) {
            this.customerId = customerId;
            this.name = name;
            this.phone = phone;
        }

        void add(long owed) {
            count++;
            total += owed;
        }

        Integer customerId() {
            return customerId;
        }

        String name() {
            return name;
        }

        String phone() {
            return phone;
        }

        long count() {
            return count;
        }

        long total() {
            return total;
        }
    }
}
