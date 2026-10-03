package com.ecommerce.sportcenter.module.sales.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.sales.dto.request.CreateSalesOrderRequest;
import com.ecommerce.sportcenter.module.sales.dto.request.SearchSalesOrderRequest;
import com.ecommerce.sportcenter.module.sales.dto.response.SalesOrderResponse;

public interface SalesOrderService {
    PageResponse<SalesOrderResponse> search(SearchSalesOrderRequest request);

    SalesOrderResponse getById(int id);

    SalesOrderResponse create(CreateSalesOrderRequest request, String username);

    SalesOrderResponse confirm(int id);

    SalesOrderResponse cancel(int id);
}
