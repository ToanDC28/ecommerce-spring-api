package com.ecommerce.sportcenter.module.inventory.service.impl;

import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.inventory.dto.mapper.InventoryMapper;
import com.ecommerce.sportcenter.module.inventory.dto.request.SearchStockRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockResponse;
import com.ecommerce.sportcenter.module.inventory.entity.Stock;
import com.ecommerce.sportcenter.module.inventory.repository.StockRepository;
import com.ecommerce.sportcenter.module.inventory.repository.StockTransactionRepository;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockTransactionRepository transactionRepository;
    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryServiceImpl service;

    private Stock stock(int id, long onHand, long minStock) {
        var material = Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                .costPrice(25000L).stockQty(onHand).minStock(minStock).active(true).build();
        return Stock.builder().id(id).material(material).qtyOnHand(onHand).qtyReserved(0L).build();
    }

    @Nested
    @DisplayName("searchStocks():")
    class SearchStocks {

        @Test
        @DisplayName("success returns mapped page")
        void success() {
            var request = SearchStockRequest.builder().page(0).size(10).build();
            var response = StockResponse.builder().id(1).materialSku("VT-001").qtyOnHand(100L).build();
            when(stockRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(stock(1, 100L, 10L))));
            when(inventoryMapper.toResponse(any())).thenReturn(response);

            var result = service.searchStocks(request);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("lowStock():")
    class LowStock {

        @Test
        @DisplayName("returns only rows at/below minStock")
        void flags() {
            when(stockRepository.findAll(any(Specification.class))).thenReturn(List.of(stock(2, 5L, 50L)));
            when(inventoryMapper.toResponse(any()))
                    .thenReturn(StockResponse.builder().id(2).lowStock(true).build());

            var result = service.lowStock(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isLowStock()).isTrue();
        }
    }

    @Nested
    @DisplayName("increase():")
    class Increase {

        @Test
        @DisplayName("notFound when material missing")
        void notFound() {
            when(materialRepository.findById(99)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.increase(99, null, 10L,
                    com.ecommerce.sportcenter.module.inventory.entity.StockRefType.GRN, "GRN-1", "admin"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Material not found");
        }
    }
}
