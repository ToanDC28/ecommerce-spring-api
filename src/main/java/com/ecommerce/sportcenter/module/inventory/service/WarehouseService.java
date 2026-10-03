package com.ecommerce.sportcenter.module.inventory.service;

import com.ecommerce.sportcenter.module.inventory.dto.request.CreateWarehouseRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.WarehouseResponse;

import java.util.List;

public interface WarehouseService {
    List<WarehouseResponse> getAll();

    WarehouseResponse getById(int id);

    WarehouseResponse create(CreateWarehouseRequest request);
}
