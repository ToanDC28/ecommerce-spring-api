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
     * Cấn trừ toàn bộ cọc vào invoice (cùng khách, invoice còn nợ >= tiền cọc).
     * Sinh Payment + đánh dấu APPLIED, atomic 1 transaction.
     */
    PaymentResponse apply(int advanceId, ApplyAdvanceRequest request);

    AdvanceResponse cancel(int id);
}
