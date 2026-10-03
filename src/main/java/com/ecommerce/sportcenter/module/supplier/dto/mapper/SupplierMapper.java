package com.ecommerce.sportcenter.module.supplier.dto.mapper;

import com.ecommerce.sportcenter.module.supplier.dto.request.CreateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.UpdateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.response.SupplierResponse;
import com.ecommerce.sportcenter.module.supplier.entity.PaymentTerm;
import com.ecommerce.sportcenter.module.supplier.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public SupplierResponse toResponse(Supplier s) {
        if (s == null) {
            return null;
        }
        return SupplierResponse.builder()
                .id(s.getId())
                .createdDate(s.getCreatedDate())
                .updatedDate(s.getUpdatedDate())
                .code(s.getCode())
                .name(s.getName())
                .taxCode(s.getTaxCode())
                .phone(s.getPhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .paymentTerm(s.getPaymentTerm())
                .active(s.isActive())
                .currentDebt(s.getCurrentDebt())
                .build();
    }

    public Supplier toEntity(CreateSupplierRequest request, String code) {
        if (request == null) {
            return null;
        }
        return Supplier.builder()
                .code(code)
                .name(request.getName())
                .taxCode(request.getTaxCode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .paymentTerm(request.getPaymentTerm() != null ? request.getPaymentTerm() : PaymentTerm.NET_30)
                .active(true)
                .currentDebt(0L)
                .build();
    }

    public Supplier updateSupplier(Supplier supplier, UpdateSupplierRequest request) {
        if (supplier == null || request == null) {
            return supplier;
        }
        if (request.getName() != null) {
            supplier.setName(request.getName());
        }
        if (request.getTaxCode() != null) {
            supplier.setTaxCode(request.getTaxCode());
        }
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            supplier.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress());
        }
        if (request.getPaymentTerm() != null) {
            supplier.setPaymentTerm(request.getPaymentTerm());
        }
        return supplier;
    }
}
