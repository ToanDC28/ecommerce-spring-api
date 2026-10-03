package com.ecommerce.sportcenter.module.invoice.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreatePurchaseInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateSalesInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateWorkInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.SearchInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;

public interface InvoiceService {
    PageResponse<InvoiceResponse> search(SearchInvoiceRequest request);

    InvoiceResponse getById(int id);

    InvoiceResponse createWorkInvoice(CreateWorkInvoiceRequest request);

    InvoiceResponse createPurchaseInvoice(CreatePurchaseInvoiceRequest request);

    InvoiceResponse createSalesInvoice(CreateSalesInvoiceRequest request);

    /**
     * True nếu đã có invoice cùng refCode+type chưa bị CANCELLED
     * (chống xuất trùng invoice tay cho cùng GIN/GRN).
     */
    boolean existsActiveInvoice(String refCode, InvoiceType type);

    /**
     * Job hằng đêm: ISSUED/PARTIAL quá dueDate -> OVERDUE. Trả về số invoice bị đánh dấu.
     */
    int markOverdue();

    InvoiceResponse issue(int id);

    InvoiceResponse cancel(int id);
}
