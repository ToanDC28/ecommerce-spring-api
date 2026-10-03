package com.ecommerce.sportcenter.module.material.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.material.dto.mapper.MaterialMapper;
import com.ecommerce.sportcenter.module.material.dto.request.CreateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.response.MaterialResponse;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.CategoryRepository;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceImplTest {

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MaterialMapper materialMapper;

    @InjectMocks
    private MaterialServiceImpl service;

    private Material material;
    private MaterialResponse response;

    @BeforeEach
    void setUp() {
        material = Material.builder().id(1).sku("VT-THEP-001").name("Thép").unit(MaterialUnit.KG)
                .costPrice(25000L).stockQty(100L).minStock(10L).active(true).build();
        response = MaterialResponse.builder().id(1).sku("VT-THEP-001").name("Thép").stockQty(100L).build();
    }

    @Nested
    @DisplayName("create():")
    class Create {

        @Test
        @DisplayName("success when SKU unique")
        void success() {
            var request = CreateMaterialRequest.builder().sku("VT-THEP-001").name("Thép")
                    .unit(MaterialUnit.KG).costPrice(25000L).build();
            when(materialRepository.existsBySku("VT-THEP-001")).thenReturn(false);
            when(materialMapper.toEntity(request)).thenReturn(material);
            when(materialRepository.save(material)).thenReturn(material);
            when(materialMapper.toResponse(material)).thenReturn(response);

            assertThat(service.create(request)).isEqualTo(response);
        }

        @Test
        @DisplayName("duplicate when SKU exists")
        void duplicate() {
            var request = CreateMaterialRequest.builder().sku("VT-THEP-001").name("Thép")
                    .unit(MaterialUnit.KG).costPrice(25000L).build();
            when(materialRepository.existsBySku("VT-THEP-001")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("already exists");
            verify(materialRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById():")
    class GetById {

        @Test
        @DisplayName("notFound when missing")
        void notFound() {
            when(materialRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Material not found");
        }
    }
}
