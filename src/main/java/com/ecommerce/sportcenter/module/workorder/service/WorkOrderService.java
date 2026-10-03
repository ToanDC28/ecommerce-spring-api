package com.ecommerce.sportcenter.module.workorder.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.workorder.dto.request.ConsumeMaterialRequest;
import com.ecommerce.sportcenter.module.workorder.dto.request.CreateWorkOrderRequest;
import com.ecommerce.sportcenter.module.workorder.dto.request.SearchWorkOrderRequest;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;

public interface WorkOrderService {
    PageResponse<WorkOrderResponse> search(SearchWorkOrderRequest request);

    WorkOrderResponse getById(int id);

    WorkOrderResponse create(CreateWorkOrderRequest request);

    WorkOrderResponse confirm(int id);

    WorkOrderResponse consume(int id, ConsumeMaterialRequest request, String username);

    WorkOrderResponse done(int id);

    WorkOrderResponse cancel(int id);
}
