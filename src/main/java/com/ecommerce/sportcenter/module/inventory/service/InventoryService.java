package com.ecommerce.sportcenter.module.inventory.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.inventory.dto.request.SearchStockRequest;
import com.ecommerce.sportcenter.module.inventory.dto.request.SearchStockTransactionRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockResponse;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockTransactionResponse;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;

import java.util.List;

public interface InventoryService {
    void increase(int materialId, Integer warehouseId, long qty, StockRefType refType, String refId, String createdBy);

    void decrease(int materialId, Integer warehouseId, long qty, StockRefType refType, String refId, String createdBy);

    PageResponse<StockResponse> searchStocks(SearchStockRequest request);

    List<StockResponse> lowStock(Integer warehouseId);

    PageResponse<StockTransactionResponse> searchTransactions(SearchStockTransactionRequest request);
}
