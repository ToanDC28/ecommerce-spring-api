package com.ecommerce.sportcenter.module.customer.dto.mapper;

import com.ecommerce.sportcenter.module.customer.dto.request.CreateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.UpdateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerResponse;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer c, long openInvoiceCount, long totalOwed) {
        if (c == null) {
            return null;
        }
        return CustomerResponse.builder()
                .id(c.getId())
                .createdDate(c.getCreatedDate())
                .code(c.getCode())
                .name(c.getName())
                .phone(c.getPhone())
                .address(c.getAddress())
                .type(c.getType())
                .active(c.isActive())
                .openInvoiceCount(openInvoiceCount)
                .totalOwed(totalOwed)
                .build();
    }

    public Customer toEntity(CreateCustomerRequest r, String code, String phone) {
        if (r == null) {
            return null;
        }
        return Customer.builder()
                .code(code)
                .name(r.getName())
                .phone(phone)
                .address(r.getAddress())
                .type(r.getType() == null ? com.ecommerce.sportcenter.module.customer.entity.CustomerType.LE_QUEN : r.getType())
                .active(true)
                .build();
    }

    public Customer updateCustomer(Customer c, UpdateCustomerRequest r, String phone) {
        if (c == null || r == null) {
            return c;
        }
        if (r.getName() != null) {
            c.setName(r.getName());
        }
        if (phone != null) {
            c.setPhone(phone);
        }
        if (r.getAddress() != null) {
            c.setAddress(r.getAddress());
        }
        if (r.getType() != null) {
            c.setType(r.getType());
        }
        if (r.getActive() != null) {
            c.setActive(r.getActive());
        }
        return c;
    }
}
