package com.ecommerce.sportcenter.module.workorder.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "work_order_attachment", indexes = {
        @Index(name = "idx_woa_wo", columnList = "work_order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE work_order_attachment SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class WorkOrderAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName; // tên gốc upload

    @Column(name = "url", length = 500)
    private String url; // link ảnh trên Zipline (DB chỉ giữ link, không giữ file)

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;
}
