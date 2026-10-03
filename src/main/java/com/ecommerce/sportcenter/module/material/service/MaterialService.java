package com.ecommerce.sportcenter.module.material.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.material.dto.request.CreateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.SearchMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.UpdateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.response.MaterialResponse;

public interface MaterialService {
    PageResponse<MaterialResponse> search(SearchMaterialRequest request);

    MaterialResponse getById(int id);

    MaterialResponse create(CreateMaterialRequest request);

    MaterialResponse update(int id, UpdateMaterialRequest request);

    MaterialResponse setActive(int id, boolean active);
}
