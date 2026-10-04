package com.ecommerce.sportcenter.module.workorder.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.customer.service.CustomerService;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.workorder.dto.mapper.WorkOrderMapper;
import com.ecommerce.sportcenter.module.workorder.dto.request.ConsumeMaterialRequest;
import com.ecommerce.sportcenter.module.workorder.dto.request.CreateWorkOrderRequest;
import com.ecommerce.sportcenter.module.workorder.dto.request.SearchWorkOrderRequest;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderAttachmentResponse;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrder;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderAttachment;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderMaterial;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderAttachmentRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderMaterialRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
import com.ecommerce.sportcenter.module.workorder.service.WorkOrderService;
import com.ecommerce.sportcenter.utils.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderMaterialRepository materialLineRepository;
    private final WorkOrderAttachmentRepository attachmentRepository;
    private final MaterialRepository materialRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final InventoryService inventoryService;
    private final ZiplineClient ziplineClient;
    private final WorkOrderMapper workOrderMapper;

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> search(SearchWorkOrderRequest request) {
        log.info("Search work orders - keyword={}, type={}, status={}, page={}, size={}",
                request.getKeyword(), request.getType(), request.getStatus(), request.getPage(), request.getSize());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<WorkOrder> spec = withSearchAndFilters(request);
        spec = spec.and(new SpecificationBuilder<WorkOrder>().build(request));
        var page = workOrderRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(workOrderMapper::toResponse).toList();
        return PageResponse.<WorkOrderResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private Specification<WorkOrder> withSearchAndFilters(SearchWorkOrderRequest request) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("code")), like),
                        builder.like(builder.lower(root.get("customerName")), like),
                        builder.like(builder.lower(root.get("contractNo")), like)));
            }
            if (request.getType() != null) {
                predicates.add(builder.equal(root.get("type"), request.getType()));
            }
            if (request.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), request.getStatus()));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderResponse getById(int id) {
        log.info("Get work order by id - id={}", id);
        WorkOrder wo = findOrThrow(id);
        return workOrderMapper.toResponse(wo, attachmentRepository.findByWorkOrder_Id(id));
    }

    @Override
    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        if (!customer.isActive()) {
            throw new BusinessValidationException("Customer '" + customer.getCode() + "' is inactive");
        }
        // Preload + validate vật tư trước khi lưu (tránh mồ côi WO rỗng).
        Map<Integer, Material> materials = new HashMap<>();
        for (var item : request.getItems()) {
            if (!materials.containsKey(item.getMaterialId())) {
                Material material = materialRepository.findById(item.getMaterialId())
                        .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + item.getMaterialId()));
                if (!material.isActive()) {
                    throw new BusinessValidationException("Material '" + material.getSku() + "' is inactive");
                }
                materials.put(item.getMaterialId(), material);
            }
        }
        String code = nextCode();
        // Giá bán dự toán theo khách (giá riêng -> sellPrice chung -> costPrice).
        Map<Integer, Long> plannedPrices = new HashMap<>();
        for (var item : request.getItems()) {
            Material material = materials.get(item.getMaterialId());
            Long sellPrice = customerService.resolveSellPrice(customer.getId(), material);
            plannedPrices.put(item.getMaterialId(), sellPrice == null ? material.getCostPrice() : sellPrice);
        }
        WorkOrder wo = WorkOrder.builder()
                .code(code)
                .type(request.getType())
                .contractNo(request.getContractNo())
                .customer(customer)
                .customerName(request.getCustomerName() == null || request.getCustomerName().isBlank()
                        ? customer.getName() : request.getCustomerName())
                .customerPhone(request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()
                        ? customer.getPhone() : request.getCustomerPhone())
                .machineInfo(request.getMachineInfo())
                .receivedDate(LocalDate.now())
                .dueDate(request.getDueDate())
                .status(WorkOrderStatus.DRAFT)
                .laborCost(request.getLaborCost() == null ? 0L : request.getLaborCost())
                .overheadCost(request.getOverheadCost() == null ? 0L : request.getOverheadCost())
                .agreedPrice(request.getAgreedPrice())
                .build();
        wo = workOrderRepository.save(wo);

        for (var item : request.getItems()) {
            Material material = materials.get(item.getMaterialId());
            long sellPrice = plannedPrices.get(item.getMaterialId());
            WorkOrderMaterial line = WorkOrderMaterial.builder()
                    .workOrder(wo)
                    .material(material)
                    .qtyPlanned(item.getQtyPlanned())
                    .qtyActual(0L)
                    .unitCost(material.getCostPrice())
                    .unitSellPrice(sellPrice)
                    .build();
            materialLineRepository.save(line);
            wo.getMaterials().add(line);
        }
        return workOrderMapper.toResponse(wo);
    }

    @Override
    @Transactional
    public WorkOrderResponse confirm(int id) {
        WorkOrder wo = findOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.DRAFT) {
            throw new BusinessValidationException("Only DRAFT work order can be confirmed");
        }
        wo.setStatus(WorkOrderStatus.CONFIRMED);
        // auto move to IN_PROGRESS on first consume, but set here for clarity if needed
        return workOrderMapper.toResponse(workOrderRepository.save(wo));
    }

    @Override
    @Transactional
    public WorkOrderResponse consume(int id, ConsumeMaterialRequest request, String username) {
        WorkOrder wo = findOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.CONFIRMED && wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new BusinessValidationException("Only CONFIRMED/IN_PROGRESS work order can consume materials");
        }
        Map<Integer, WorkOrderMaterial> lineByMaterial = new HashMap<>();
        for (var line : materialLineRepository.findByWorkOrder_Id(id)) {
            lineByMaterial.put(line.getMaterial().getId(), line);
        }
        for (var consume : request.getItems()) {
            Material material = materialRepository.findById(consume.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + consume.getMaterialId()));
            // trừ kho trước (ném 409 nếu thiếu)
            inventoryService.decrease(material.getId(), request.getWarehouseId(),
                    consume.getQty(), StockRefType.WORK_ORDER, wo.getCode(), username);

            WorkOrderMaterial line = lineByMaterial.get(material.getId());
            if (line == null) {
                // phát sinh ngoài dự toán: tạo line mới với planned = 0
                line = WorkOrderMaterial.builder()
                        .workOrder(wo)
                        .material(material)
                        .qtyPlanned(0L)
                        .qtyActual(0L)
                        .unitCost(material.getCostPrice())
                        .unitSellPrice(material.getSellPrice() == null ? material.getCostPrice() : material.getSellPrice())
                        .note("Phát sinh ngoài dự toán")
                        .build();
                line = materialLineRepository.save(line);
                wo.getMaterials().add(line);
                lineByMaterial.put(material.getId(), line);
            }
            line.setQtyActual(line.getQtyActual() + consume.getQty());
            // snapshot lại giá tại thời điểm xuất cuối
            line.setUnitCost(material.getCostPrice());
            materialLineRepository.save(line);
        }
        if (wo.getStatus() == WorkOrderStatus.CONFIRMED) {
            wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        }
        wo = workOrderRepository.save(wo);
        return workOrderMapper.toResponse(findOrThrow(wo.getId()));
    }

    @Override
    @Transactional
    public WorkOrderResponse done(int id) {
        WorkOrder wo = findOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS && wo.getStatus() != WorkOrderStatus.CONFIRMED) {
            throw new BusinessValidationException("Only CONFIRMED/IN_PROGRESS work order can be done");
        }
        if (attachmentRepository.countByWorkOrder_Id(id) == 0) {
            throw new BusinessValidationException("Chụp ít nhất 1 ảnh nghiệm thu trước khi chốt (máy đã sửa xong)");
        }
        wo.setStatus(WorkOrderStatus.DONE);
        wo = workOrderRepository.save(wo);
        return workOrderMapper.toResponse(wo, attachmentRepository.findByWorkOrder_Id(id));
    }

    @Override
    @Transactional
    public WorkOrderResponse cancel(int id) {
        WorkOrder wo = findOrThrow(id);
        if (wo.getStatus() == WorkOrderStatus.DONE || wo.getStatus() == WorkOrderStatus.INVOICED) {
            throw new BusinessValidationException("DONE/INVOICED work order cannot be cancelled (revert via return GRN)");
        }
        wo.setStatus(WorkOrderStatus.CANCELLED);
        return workOrderMapper.toResponse(workOrderRepository.save(wo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderAttachmentResponse> attachments(int id) {
        findOrThrow(id);
        return attachmentRepository.findByWorkOrder_Id(id).stream()
                .map(workOrderMapper::toAttachmentResponse).toList();
    }

    @Override
    @Transactional
    public WorkOrderAttachmentResponse uploadAttachment(int id, MultipartFile file, String username) {
        WorkOrder wo = findOrThrow(id);
        if (wo.getStatus() != WorkOrderStatus.CONFIRMED && wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new BusinessValidationException("Only CONFIRMED/IN_PROGRESS work order accepts photos (current=" + wo.getStatus() + ")");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("File is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessValidationException("File exceeds 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessValidationException("Only JPG/PNG/WEBP photos are accepted");
        }
        try {
            // Đẩy sang Zipline (giữ API key ở server), DB chỉ lưu URL.
            String url = ziplineClient.upload(file.getBytes(),
                    file.getOriginalFilename() == null ? "photo.jpg" : file.getOriginalFilename(),
                    contentType);
            WorkOrderAttachment attachment = attachmentRepository.save(WorkOrderAttachment.builder()
                    .workOrder(wo)
                    .fileName(file.getOriginalFilename() == null ? url : file.getOriginalFilename())
                    .url(url)
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .uploadedBy(username)
                    .build());
            log.info("WO photo uploaded - wo={}, url={}", wo.getCode(), url);
            return workOrderMapper.toAttachmentResponse(attachment);
        } catch (IOException e) {
            throw new BusinessValidationException("Cannot read upload file: " + e.getMessage());
        }
    }

    private WorkOrder findOrThrow(int id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));
    }

    private String nextCode() {
        long count = workOrderRepository.count() + 1;
        String code;
        do {
            code = String.format("WO-2026-%04d", count++);
        } while (workOrderRepository.existsByCode(code));
        return code;
    }
}
