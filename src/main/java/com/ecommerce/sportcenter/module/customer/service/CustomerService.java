package com.ecommerce.sportcenter.module.customer.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.dto.request.CreateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.SearchCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.SetCustomerPriceRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.UpdateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerPriceResponse;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerResponse;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;

import java.util.List;

public interface CustomerService {
    PageResponse<CustomerResponse> search(SearchCustomerRequest request);

    CustomerResponse getById(int id);

    CustomerResponse create(CreateCustomerRequest request);

    CustomerResponse update(int id, UpdateCustomerRequest request);

    CustomerResponse setActive(int id, boolean active);

    List<InvoiceResponse> debts(int id);

    List<WorkOrderResponse> workOrders(int id);

    List<CustomerPriceResponse> prices(int id);

    CustomerPriceResponse setPrice(int id, SetCustomerPriceRequest request);

    void deletePrice(int id, int materialId);

    /**
     * Dư nợ hiện tại (WORK+SALES chưa trả). Dùng để check hạn mức khi tạo SO/WO.
     */
    long currentOwed(int customerId);

    /**
     * Giá bán áp dụng cho khách: giá riêng (nếu có) -> sellPrice chung.
     * Trả null khi cả hai đều null (hàng nội bộ).
     */
    Long resolveSellPrice(Integer customerId, com.ecommerce.sportcenter.module.material.entity.Material material);

    String normalizePhone(String raw);
}
