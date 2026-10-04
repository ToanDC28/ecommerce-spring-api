package com.ecommerce.sportcenter.module.payment.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.payment.dto.request.ApplyAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.CreateAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.SearchAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.AdvanceResponse;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;

public interface AdvanceService {
    PageResponse<AdvanceResponse> search(SearchAdvanceRequest request);

    AdvanceResponse getById(int id);

    AdvanceResponse create(CreateAdvanceRequest request, String username);

    /**
     * Cấn trừ cọc vào invoice (cùng đơn): cấn NGUYÊN CỤC, invoice còn nợ
     * phải >= tiền cọc, coi như 1 lần trả tiền.
     */
    PaymentResponse apply(int advanceId, ApplyAdvanceRequest request);

    /**
     * Tự cấn các cọc ACTIVE của đúng đơn khi xuất invoice: cọc nào vừa
     * (<= số còn nợ lúc đó) thì apply hết, cọc lớn hơn thì giữ lại.
     * Trả về số cọc đã cấn.
     */
    int autoApply(int invoiceId);

    AdvanceResponse cancel(int id);
}
