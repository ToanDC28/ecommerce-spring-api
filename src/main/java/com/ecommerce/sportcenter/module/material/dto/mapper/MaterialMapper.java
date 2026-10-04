package com.ecommerce.sportcenter.module.material.dto.mapper;

import com.ecommerce.sportcenter.module.material.dto.request.CreateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.UpdateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.response.MaterialResponse;
import com.ecommerce.sportcenter.module.material.entity.Material;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {

    public MaterialResponse toResponse(Material m) {
        if (m == null) {
            return null;
        }
        return MaterialResponse.builder()
                .id(m.getId())
                .createdDate(m.getCreatedDate())
                .updatedDate(m.getUpdatedDate())
                .sku(m.getSku())
                .name(m.getName())
                .categoryId(m.getCategory() == null ? null : m.getCategory().getId())
                .categoryName(m.getCategory() == null ? null : m.getCategory().getName())
                .brand(m.getBrand())
                .unit(m.getUnit())
                .costPrice(m.getCostPrice())
                .sellPrice(m.getSellPrice())
                .stockQty(m.getStockQty())
                .minStock(m.getMinStock())
                .location(m.getLocation())
                .active(m.isActive())
                .lowStock(m.getStockQty() <= m.getMinStock())
                .materialGrade(m.getMaterialGrade())
                .standard(m.getStandard())
                .spec(m.getSpec())
                .thicknessMm(m.getThicknessMm())
                .widthMm(m.getWidthMm())
                .lengthMm(m.getLengthMm())
                .diameterMm(m.getDiameterMm())
                .strengthGrade(m.getStrengthGrade())
                .detail(m.getDetail())
                .build();
    }

    public Material toEntity(CreateMaterialRequest r) {
        if (r == null) {
            return null;
        }
        return Material.builder()
                .sku(r.getSku().trim())
                .name(r.getName())
                .brand(r.getBrand())
                .unit(r.getUnit())
                .costPrice(r.getCostPrice() == null ? 0L : r.getCostPrice())
                .sellPrice(r.getSellPrice())
                .minStock(r.getMinStock() == null ? 0L : r.getMinStock())
                .location(r.getLocation())
                .materialGrade(r.getMaterialGrade())
                .standard(r.getStandard())
                .spec(r.getSpec())
                .thicknessMm(r.getThicknessMm())
                .widthMm(r.getWidthMm())
                .lengthMm(r.getLengthMm())
                .diameterMm(r.getDiameterMm())
                .strengthGrade(r.getStrengthGrade())
                .detail(r.getDetail())
                .stockQty(0L)
                .active(true)
                .build();
    }

    public Material updateMaterial(Material m, UpdateMaterialRequest r) {
        if (m == null || r == null) {
            return m;
        }
        if (r.getName() != null) {
            m.setName(r.getName());
        }
        if (r.getBrand() != null) {
            m.setBrand(r.getBrand());
        }
        if (r.getUnit() != null) {
            m.setUnit(r.getUnit());
        }
        if (r.getCostPrice() != null) {
            m.setCostPrice(r.getCostPrice());
        }
        if (r.getSellPrice() != null) {
            m.setSellPrice(r.getSellPrice());
        }
        if (r.getMinStock() != null) {
            m.setMinStock(r.getMinStock());
        }
        if (r.getLocation() != null) {
            m.setLocation(r.getLocation());
        }
        if (r.getMaterialGrade() != null) {
            m.setMaterialGrade(r.getMaterialGrade());
        }
        if (r.getStandard() != null) {
            m.setStandard(r.getStandard());
        }
        if (r.getSpec() != null) {
            m.setSpec(r.getSpec());
        }
        if (r.getThicknessMm() != null) {
            m.setThicknessMm(r.getThicknessMm());
        }
        if (r.getWidthMm() != null) {
            m.setWidthMm(r.getWidthMm());
        }
        if (r.getLengthMm() != null) {
            m.setLengthMm(r.getLengthMm());
        }
        if (r.getDiameterMm() != null) {
            m.setDiameterMm(r.getDiameterMm());
        }
        if (r.getStrengthGrade() != null) {
            m.setStrengthGrade(r.getStrengthGrade());
        }
        if (r.getDetail() != null) {
            m.setDetail(r.getDetail());
        }
        if (r.getActive() != null) {
            m.setActive(r.getActive());
        }
        return m;
    }
}
