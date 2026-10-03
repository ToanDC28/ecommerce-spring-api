package com.ecommerce.sportcenter.module.customer.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.dto.mapper.CustomerMapper;
import com.ecommerce.sportcenter.module.customer.dto.request.CreateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.SearchCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.SetCustomerPriceRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.UpdateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerPriceResponse;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerResponse;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.entity.CustomerMaterialPrice;
import com.ecommerce.sportcenter.module.customer.repository.CustomerMaterialPriceRepository;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.customer.service.CustomerService;
import com.ecommerce.sportcenter.module.invoice.dto.mapper.InvoiceMapper;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.workorder.dto.mapper.WorkOrderMapper;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMaterialPriceRepository customerPriceRepository;
    private final com.ecommerce.sportcenter.module.material.repository.MaterialRepository materialRepository;
    private final InvoiceRepository invoiceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CustomerMapper customerMapper;
    private final InvoiceMapper invoiceMapper;
    private final WorkOrderMapper workOrderMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(SearchCustomerRequest request) {
        log.info("Search customers - keyword={}, type={}, active={}",
                request.getKeyword(), request.getType(), request.getActive());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<Customer> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("code")), like),
                        builder.like(builder.lower(root.get("name")), like),
                        builder.like(builder.lower(root.get("phone")), like)));
            }
            if (request.getType() != null) {
                predicates.add(builder.equal(root.get("type"), request.getType()));
            }
            if (request.getActive() != null) {
                predicates.add(builder.equal(root.get("active"), request.getActive()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        spec = spec.and(new SpecificationBuilder<Customer>().build(request));
        var page = customerRepository.findAll(spec, pageable);
        var content = page.getContent().stream()
                .map(c -> customerMapper.toResponse(c, 0L, 0L)).toList();
        return PageResponse.<CustomerResponse>builder()
                .content(content).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).size(page.getSize())
                .number(page.getNumber()).first(page.isFirst()).last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(int id) {
        log.info("Get customer by id - id={}", id);
        Customer customer = findOrThrow(id);
        return customerMapper.toResponse(customer, countOpen(customer.getId()), sumOwed(customer.getId()));
    }

    @Override
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String phone = normalizePhone(request.getPhone());
        if (phone != null) {
            customerRepository.findByPhone(phone).ifPresent(c -> {
                throw new BusinessValidationException("Phone '" + phone + "' already belongs to customer " + c.getCode());
            });
        }
        String code = nextCode();
        Customer customer = customerMapper.toEntity(request, code, phone);
        customer = customerRepository.save(customer);
        return customerMapper.toResponse(customer, 0L, 0L);
    }

    @Override
    @Transactional
    public CustomerResponse update(int id, UpdateCustomerRequest request) {
        Customer customer = findOrThrow(id);
        String phone = request.getPhone() == null ? null : normalizePhone(request.getPhone());
        if (request.getPhone() != null && phone != null) {
            final String normalized = phone;
            customerRepository.findByPhone(normalized).ifPresent(c -> {
                if (c.getId() != id) {
                    throw new BusinessValidationException("Phone '" + normalized + "' already belongs to customer " + c.getCode());
                }
            });
        }
        customerMapper.updateCustomer(customer, request, request.getPhone() == null ? null : phone);
        customer = customerRepository.save(customer);
        return customerMapper.toResponse(customer, countOpen(id), sumOwed(id));
    }

    @Override
    @Transactional
    public CustomerResponse setActive(int id, boolean active) {
        Customer customer = findOrThrow(id);
        customer.setActive(active);
        customer = customerRepository.save(customer);
        return customerMapper.toResponse(customer, countOpen(id), sumOwed(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> debts(int id) {
        findOrThrow(id);
        Specification<com.ecommerce.sportcenter.module.invoice.entity.Invoice> spec = (root, query, builder) -> builder.and(
                builder.equal(root.get("customerId"), id),
                root.get("status").in(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE));
        return invoiceRepository.findAll(spec).stream()
                .filter(inv -> inv.getGrandTotal() - inv.getPaidAmount() > 0)
                .map(invoiceMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderResponse> workOrders(int id) {
        findOrThrow(id);
        return workOrderRepository.findByCustomer_Id(id).stream()
                .map(workOrderMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerPriceResponse> prices(int id) {
        findOrThrow(id);
        return customerPriceRepository.findByCustomer_Id(id).stream()
                .map(p -> CustomerPriceResponse.builder()
                        .id(p.getId())
                        .materialId(p.getMaterial().getId())
                        .materialSku(p.getMaterial().getSku())
                        .materialName(p.getMaterial().getName())
                        .sellPrice(p.getSellPrice())
                        .defaultSellPrice(p.getMaterial().getSellPrice())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public CustomerPriceResponse setPrice(int id, SetCustomerPriceRequest request) {
        Customer customer = findOrThrow(id);
        var material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new com.ecommerce.sportcenter.exception.ResourceNotFoundException(
                        "Material not found with id: " + request.getMaterialId()));
        if (!material.isActive()) {
            throw new BusinessValidationException("Material '" + material.getSku() + "' is inactive");
        }
        CustomerMaterialPrice price = customerPriceRepository
                .findByCustomer_IdAndMaterial_Id(id, request.getMaterialId())
                .orElseGet(() -> CustomerMaterialPrice.builder().customer(customer).material(material).build());
        price.setSellPrice(request.getSellPrice());
        price = customerPriceRepository.save(price);
        log.info("Customer price set - customer={}, material={}, price={}",
                customer.getCode(), material.getSku(), request.getSellPrice());
        return CustomerPriceResponse.builder()
                .id(price.getId())
                .materialId(material.getId())
                .materialSku(material.getSku())
                .materialName(material.getName())
                .sellPrice(price.getSellPrice())
                .defaultSellPrice(material.getSellPrice())
                .build();
    }

    @Override
    @Transactional
    public void deletePrice(int id, int materialId) {
        CustomerMaterialPrice price = customerPriceRepository
                .findByCustomer_IdAndMaterial_Id(id, materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer price not found (customer=" + id + ", material=" + materialId + ")"));
        customerPriceRepository.delete(price);
    }

    @Override
    @Transactional(readOnly = true)
    public long currentOwed(int customerId) {
        return sumOwed(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveSellPrice(Integer customerId, com.ecommerce.sportcenter.module.material.entity.Material material) {
        if (customerId != null) {
            var special = customerPriceRepository.findByCustomer_IdAndMaterial_Id(customerId, material.getId());
            if (special.isPresent()) {
                return special.get().getSellPrice();
            }
        }
        return material.getSellPrice();
    }

    @Override
    public String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessValidationException("Phone is required (10 digits starting with 0)");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (!digits.matches("0\\d{9}")) {
            throw new BusinessValidationException("Phone must be 10 digits starting with 0 (got '" + raw + "')");
        }
        return digits;
    }

    private Customer findOrThrow(int id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private long countOpen(int customerId) {
        return debts(customerId).size();
    }

    private long sumOwed(int customerId) {
        return debts(customerId).stream()
                .mapToLong(inv -> inv.getGrandTotal() - inv.getPaidAmount()).sum();
    }

    private String nextCode() {
        long count = customerRepository.count() + 1;
        String code;
        do {
            code = String.format("KH-%03d", count++);
        } while (customerRepository.existsByCode(code));
        return code;
    }
}
