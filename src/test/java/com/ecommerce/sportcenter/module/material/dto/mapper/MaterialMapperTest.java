package com.ecommerce.sportcenter.module.material.dto.mapper;

import com.ecommerce.sportcenter.module.material.dto.request.CreateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.UpdateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.response.MaterialResponse;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialMapperTest {

    private final MaterialMapper mapper = new MaterialMapper();

    private CreateMaterialRequest fullRequest() {
        return CreateMaterialRequest.builder()
                .sku("VT-BULONG-M12").name("Bu-lông M12x50 cấp 8.8")
                .unit(MaterialUnit.CAI).costPrice(5000L).sellPrice(7000L)
                .materialGrade("C45").standard("DIN 931").spec("M12x50")
                .diameterMm(12.0).lengthMm(50.0).strengthGrade("8.8")
                .detail("Bước ren 1.75, mạ kẽm")
                .build();
    }

    @Test
    @DisplayName("toEntity carries spec fields, stock starts 0")
    void toEntity() {
        Material m = mapper.toEntity(fullRequest());

        assertThat(m.getSku()).isEqualTo("VT-BULONG-M12");
        assertThat(m.getMaterialGrade()).isEqualTo("C45");
        assertThat(m.getStandard()).isEqualTo("DIN 931");
        assertThat(m.getSpec()).isEqualTo("M12x50");
        assertThat(m.getDiameterMm()).isEqualTo(12.0);
        assertThat(m.getLengthMm()).isEqualTo(50.0);
        assertThat(m.getThicknessMm()).isNull();
        assertThat(m.getStrengthGrade()).isEqualTo("8.8");
        assertThat(m.getDetail()).isEqualTo("Bước ren 1.75, mạ kẽm");
        assertThat(m.getStockQty()).isEqualTo(0L);
    }

    @Test
    @DisplayName("toResponse carries spec fields")
    void toResponse() {
        MaterialResponse r = mapper.toResponse(mapper.toEntity(fullRequest()));

        assertThat(r.getSpec()).isEqualTo("M12x50");
        assertThat(r.getMaterialGrade()).isEqualTo("C45");
        assertThat(r.getDetail()).isEqualTo("Bước ren 1.75, mạ kẽm");
    }

    @Test
    @DisplayName("updateMaterial sets only non-null spec fields")
    void partialUpdate() {
        Material m = mapper.toEntity(fullRequest());
        mapper.updateMaterial(m, UpdateMaterialRequest.builder()
                .strengthGrade("10.9").detail("Đã tôi luyện").build());

        assertThat(m.getStrengthGrade()).isEqualTo("10.9");
        assertThat(m.getDetail()).isEqualTo("Đã tôi luyện");
        assertThat(m.getSpec()).isEqualTo("M12x50"); // giữ nguyên
        assertThat(m.getThicknessMm()).isNull(); // vẫn null

        mapper.updateMaterial(m, UpdateMaterialRequest.builder().name("Tên mới").build());
        assertThat(m.getName()).isEqualTo("Tên mới");
        assertThat(m.getStrengthGrade()).isEqualTo("10.9"); // không đụng
    }

    @Test
    @DisplayName("null-safe")
    void nullSafe() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.updateMaterial(null, UpdateMaterialRequest.builder().build())).isNull();
    }
}
