package com.ecommerce.sportcenter.module.report.dto.response;

import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopMaterialRow {
    private int materialId;
    private String sku;
    private String name;
    private MaterialUnit unit;
    private long consumedQty; // xuất WO (actual)
    private long soldQty; // bán SO
    private long totalQty;
}
