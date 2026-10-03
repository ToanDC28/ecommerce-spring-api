package com.ecommerce.sportcenter.module.supplier.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.supplier.dto.request.CreateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.SearchSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.UpdateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.response.SupplierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {
    PageResponse<SupplierResponse> search(SearchSupplierRequest request);

    Page<SupplierResponse> getAll(String keyword, Boolean isActive, Pageable pageable);
    SupplierResponse getById(int id);
    SupplierResponse create(CreateSupplierRequest request);
    SupplierResponse update(int id, UpdateSupplierRequest request);
    SupplierResponse setActive(int id, boolean active);
}
