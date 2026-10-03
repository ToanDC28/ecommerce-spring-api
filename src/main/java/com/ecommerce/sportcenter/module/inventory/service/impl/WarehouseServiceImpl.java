package com.ecommerce.sportcenter.module.inventory.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.inventory.dto.mapper.InventoryMapper;
import com.ecommerce.sportcenter.module.inventory.dto.request.CreateWarehouseRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.WarehouseResponse;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.inventory.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAll() {
        log.info("Fetching all warehouses");
        return warehouseRepository.findAll().stream().map(inventoryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getById(int id) {
        log.info("Get warehouse by id - id={}", id);
        return inventoryMapper.toResponse(warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id)));
    }

    @Override
    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        String code = request.getCode().trim();
        if (warehouseRepository.existsByCode(code)) {
            throw new BusinessValidationException("Warehouse code '" + code + "' already exists");
        }
        Warehouse warehouse = Warehouse.builder()
                .code(code)
                .name(request.getName())
                .address(request.getAddress())
                .active(true)
                .build();
        return inventoryMapper.toResponse(warehouseRepository.save(warehouse));
    }
}
