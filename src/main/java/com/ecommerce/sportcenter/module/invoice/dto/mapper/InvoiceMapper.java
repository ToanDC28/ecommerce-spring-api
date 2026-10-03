package com.ecommerce.sportcenter.module.invoice.dto.mapper;

import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceItemResponse;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceItem;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice inv) {
        if (inv == null) {
            return null;
        }
        return InvoiceResponse.builder()
                .id(inv.getId())
                .createdDate(inv.getCreatedDate())
                .code(inv.getCode())
                .type(inv.getType())
                .workOrderId(inv.getWorkOrderId())
                .workOrderCode(inv.getWorkOrderCode())
                .supplierId(inv.getSupplierId())
                .supplierName(inv.getSupplierName())
                .soId(inv.getSoId())
                .soCode(inv.getSoCode())
                .refCode(inv.getRefCode())
                .customerName(inv.getCustomerName())
                .customerId(inv.getCustomerId())
                .issueDate(inv.getIssueDate())
                .dueDate(inv.getDueDate())
                .subTotal(inv.getSubTotal())
                .discountAmount(inv.getDiscountAmount())
                .vatRate(inv.getVatRate())
                .vatAmount(inv.getVatAmount())
                .grandTotal(inv.getGrandTotal())
                .paidAmount(inv.getPaidAmount())
                .status(inv.getStatus())
                .items(inv.getItems() == null ? java.util.List.of()
                        : inv.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public InvoiceItemResponse toItemResponse(InvoiceItem item) {
        if (item == null) {
            return null;
        }
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .materialId(item.getMaterial() == null ? null : item.getMaterial().getId())
                .description(item.getDescription())
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .discount(item.getDiscount())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
