package com.ecommerce.sportcenter.module.sales.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.sales.dto.request.CreateGoodsIssueRequest;
import com.ecommerce.sportcenter.module.sales.dto.request.SearchGoodsIssueRequest;
import com.ecommerce.sportcenter.module.sales.dto.response.GoodsIssueResponse;

public interface GoodsIssueService {
    PageResponse<GoodsIssueResponse> search(SearchGoodsIssueRequest request);

    GoodsIssueResponse getById(int id);

    GoodsIssueResponse create(CreateGoodsIssueRequest request, String username);

    GoodsIssueResponse confirm(int id, String username);

    GoodsIssueResponse cancel(int id);

    /**
     * Bán lẻ trực tiếp tại quầy (không qua SO): tạo + confirm 1 bước
     * (Stock- , auto SALES invoice DRAFT).
     */
    GoodsIssueResponse quickSale(CreateGoodsIssueRequest request, String username);

    /**
     * Xuất invoice SALES tay cho GIN CONFIRMED (khi auto bị lỗi/mất):
     * chặn trùng nếu đã có invoice cùng refCode chưa CANCELLED.
     */
    InvoiceResponse invoice(int id);
}
