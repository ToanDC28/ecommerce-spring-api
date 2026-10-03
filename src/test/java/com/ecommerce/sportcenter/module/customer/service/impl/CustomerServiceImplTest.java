package com.ecommerce.sportcenter.module.customer.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.customer.dto.mapper.CustomerMapper;
import com.ecommerce.sportcenter.module.customer.dto.request.CreateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.SetCustomerPriceRequest;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerResponse;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.entity.CustomerMaterialPrice;
import com.ecommerce.sportcenter.module.customer.entity.CustomerType;
import com.ecommerce.sportcenter.module.customer.repository.CustomerMaterialPriceRepository;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.invoice.dto.mapper.InvoiceMapper;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.workorder.dto.mapper.WorkOrderMapper;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
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
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerMaterialPriceRepository customerPriceRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private WorkOrderMapper workOrderMapper;

    @InjectMocks
    private CustomerServiceImpl service;

    @Nested
    @DisplayName("create():")
    class Create {

        @Test
        @DisplayName("success normalizes phone (+84 -> 0) and auto code")
        void success() {
            when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.empty());
            when(customerRepository.count()).thenReturn(0L);
            when(customerRepository.existsByCode(any())).thenReturn(false);
            when(customerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(customerMapper.toEntity(any(), any(), any())).thenCallRealMethod();
            // toEntity real cần type default — gọi trực tiếp qua service, stub toResponse
            when(customerMapper.toResponse(any(), any(Long.class), any(Long.class)))
                    .thenReturn(CustomerResponse.builder().code("KH-001").phone("0901234567").build());

            var result = service.create(CreateCustomerRequest.builder()
                    .name("Anh Ba").phone("+84901234567").type(CustomerType.HOP_DONG).build());

            assertThat(result.getCode()).isEqualTo("KH-001");
            assertThat(result.getPhone()).isEqualTo("0901234567");
        }

        @Test
        @DisplayName("duplicate when phone belongs to another customer")
        void duplicatePhone() {
            var other = Customer.builder().id(1).code("KH-001").name("A Ba").phone("0901234567").build();
            when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.create(CreateCustomerRequest.builder()
                    .name("Anh Ba").phone("0901234567").build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("already belongs");
            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject bad phone format")
        void badPhone() {
            assertThatThrownBy(() -> service.create(CreateCustomerRequest.builder()
                    .name("Anh Ba").phone("12345").build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("10 digits");
        }
    }

    @Nested
    @DisplayName("normalizePhone():")
    class Normalize {

        @Test
        @DisplayName("null/blank rejected, 10-digit kept, +84 normalized")
        void cases() {
            assertThatThrownBy(() -> service.normalizePhone(null))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Phone is required");
            assertThatThrownBy(() -> service.normalizePhone("   "))
                    .isInstanceOf(BusinessValidationException.class);
            assertThat(service.normalizePhone("0901234567")).isEqualTo("0901234567");
        }
    }

    @Nested
    @DisplayName("prices + resolveSellPrice():")
    class Prices {

        private Customer customer() {
            return Customer.builder().id(1).code("KH-001").name("Anh Ba")
                    .phone("0901234567").active(true).build();
        }

        private Material material() {
            return Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                    .costPrice(25000L).sellPrice(30000L).active(true).build();
        }

        @Test
        @DisplayName("setPrice creates special price")
        void setPrice() {
            when(customerRepository.findById(1)).thenReturn(Optional.of(customer()));
            when(materialRepository.findById(1)).thenReturn(Optional.of(material()));
            when(customerPriceRepository.findByCustomer_IdAndMaterial_Id(1, 1)).thenReturn(Optional.empty());
            when(customerPriceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.setPrice(1, SetCustomerPriceRequest.builder()
                    .materialId(1).sellPrice(27000L).build());

            assertThat(result.getSellPrice()).isEqualTo(27000L);
            assertThat(result.getDefaultSellPrice()).isEqualTo(30000L);
        }

        @Test
        @DisplayName("resolveSellPrice prefers special price, falls back to sellPrice")
        void resolve() {
            var special = CustomerMaterialPrice.builder().customer(customer()).material(material()).sellPrice(27000L).build();
            when(customerPriceRepository.findByCustomer_IdAndMaterial_Id(1, 1)).thenReturn(Optional.of(special));

            assertThat(service.resolveSellPrice(1, material())).isEqualTo(27000L);
            assertThat(service.resolveSellPrice(null, material())).isEqualTo(30000L);
            // khách khác không có giá riêng -> giá chung
            assertThat(service.resolveSellPrice(2, material())).isEqualTo(30000L);
            // hàng nội bộ (null sellPrice), không giá riêng -> null
            var internal = Material.builder().id(2).sku("VT-002").name("Dung dịch")
                    .unit(MaterialUnit.LIT).costPrice(10000L).sellPrice(null).active(true).build();
            assertThat(service.resolveSellPrice(2, internal)).isNull();
        }
    }
}
