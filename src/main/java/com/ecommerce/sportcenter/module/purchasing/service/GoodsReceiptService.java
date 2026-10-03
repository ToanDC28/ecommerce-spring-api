package com.ecommerce.sportcenter.module.purchasing.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreateGoodsReceiptRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.request.SearchGoodsReceiptRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.response.GoodsReceiptResponse;

public interface GoodsReceiptService {
    PageResponse<GoodsReceiptResponse> search(SearchGoodsReceiptRequest request);

    GoodsReceiptResponse getById(int id);

    GoodsReceiptResponse create(CreateGoodsReceiptRequest request, String username);

    GoodsReceiptResponse confirm(int id, String username);

    GoodsReceiptResponse cancel(int id);

    /**
     * Nhập nhanh 1 bước cho tiệm nhỏ lẻ (hàng đã mua, phiếu chỉ ghi lịch sử):
     * tạo DRAFT + confirm ngay trong cùng 1 transaction
     * (Stock+ , PO received/status nếu có, auto PURCHASE invoice DRAFT).
     */
    GoodsReceiptResponse quickImport(CreateGoodsReceiptRequest request, String username);

    /**
     * Xuất invoice PURCHASE tay cho GRN CONFIRMED (khi auto bị lỗi/mất):
     * chặn trùng nếu đã có invoice cùng refCode chưa CANCELLED.
     */
    InvoiceResponse invoice(int id);
}
