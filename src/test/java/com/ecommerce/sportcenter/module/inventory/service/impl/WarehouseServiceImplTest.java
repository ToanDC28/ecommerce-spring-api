package com.ecommerce.sportcenter.module.inventory.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.inventory.dto.mapper.InventoryMapper;
import com.ecommerce.sportcenter.module.inventory.dto.request.CreateWarehouseRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.WarehouseResponse;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private WarehouseServiceImpl service;

    @Nested
    @DisplayName("create():")
    class Create {

        @Test
        @DisplayName("success when code unique")
        void success() {
            when(warehouseRepository.existsByCode("WH-XUONG-01")).thenReturn(false);
            when(warehouseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(inventoryMapper.toResponse(any()))
                    .thenReturn(WarehouseResponse.builder().code("WH-XUONG-01").build());

            assertThat(service.create(CreateWarehouseRequest.builder()
                    .code("WH-XUONG-01").name("Kho chính").build()).getCode())
                    .isEqualTo("WH-XUONG-01");
        }

        @Test
        @DisplayName("duplicate when code exists")
        void duplicate() {
            when(warehouseRepository.existsByCode("WH-XUONG-01")).thenReturn(true);

            assertThatThrownBy(() -> service.create(CreateWarehouseRequest.builder()
                    .code("WH-XUONG-01").name("Kho chính").build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("already exists");
            verify(warehouseRepository, never()).save(any());
        }
    }
}
