package com.ecommerce.sportcenter.module.supplier.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.supplier.dto.mapper.SupplierMapper;
import com.ecommerce.sportcenter.module.supplier.dto.request.CreateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.SearchSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.UpdateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.response.SupplierResponse;
import com.ecommerce.sportcenter.module.supplier.entity.Supplier;
import com.ecommerce.sportcenter.module.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierServiceImpl service;

    private Supplier supplier;
    private SupplierResponse response;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder().id(1).code("SUP-001").name("ACME").active(true).currentDebt(0L).build();
        response = SupplierResponse.builder().id(1).code("SUP-001").name("ACME").active(true).currentDebt(0L).build();
    }

    @Nested
    @DisplayName("create():")
    class Create {

        @Test
        @DisplayName("success when code unique")
        void success() {
            var request = CreateSupplierRequest.builder().code("SUP-001").name("ACME").build();
            when(supplierRepository.existsByCode("SUP-001")).thenReturn(false);
            when(supplierMapper.toEntity(request, "SUP-001")).thenReturn(supplier);
            when(supplierRepository.save(supplier)).thenReturn(supplier);
            when(supplierMapper.toResponse(supplier)).thenReturn(response);

            assertThat(service.create(request)).isEqualTo(response);
            verify(supplierRepository).save(supplier);
        }

        @Test
        @DisplayName("duplicate when code exists")
        void duplicate() {
            var request = CreateSupplierRequest.builder().code("SUP-001").name("ACME").build();
            when(supplierRepository.existsByCode("SUP-001")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("already exists");
            verify(supplierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById():")
    class GetById {

        @Test
        @DisplayName("success when found")
        void success() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
            when(supplierMapper.toResponse(supplier)).thenReturn(response);

            assertThat(service.getById(1)).isEqualTo(response);
        }

        @Test
        @DisplayName("notFound when missing")
        void notFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Supplier not found");
        }
    }

    @Nested
    @DisplayName("search():")
    class Search {

        @Test
        @DisplayName("success returns page")
        void success() {
            var request = SearchSupplierRequest.builder().keyword("acme").page(0).size(10).build();
            var page = new PageImpl<>(List.of(supplier));
            when(supplierRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
            when(supplierMapper.toResponse(supplier)).thenReturn(response);

            var result = service.search(request);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("update():")
    class Update {

        @Test
        @DisplayName("success partial update")
        void success() {
            var request = UpdateSupplierRequest.builder().name("ACME Updated").build();
            when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
            when(supplierMapper.updateSupplier(supplier, request)).thenReturn(supplier);
            when(supplierRepository.save(supplier)).thenReturn(supplier);
            when(supplierMapper.toResponse(supplier)).thenReturn(response);

            assertThat(service.update(1, request)).isEqualTo(response);
        }

        @Test
        @DisplayName("notFound when missing")
        void notFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99, UpdateSupplierRequest.builder().name("x").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(supplierRepository, never()).save(any());
        }
    }
}
