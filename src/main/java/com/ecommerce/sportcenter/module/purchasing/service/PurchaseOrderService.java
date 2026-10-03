package com.ecommerce.sportcenter.module.purchasing.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreatePurchaseOrderRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.request.SearchPurchaseOrderRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.response.PurchaseOrderResponse;

public interface PurchaseOrderService {
    PageResponse<PurchaseOrderResponse> search(SearchPurchaseOrderRequest request);

    PurchaseOrderResponse getById(int id);

    PurchaseOrderResponse create(CreatePurchaseOrderRequest request, String username);

    PurchaseOrderResponse send(int id);

    PurchaseOrderResponse cancel(int id);
}
