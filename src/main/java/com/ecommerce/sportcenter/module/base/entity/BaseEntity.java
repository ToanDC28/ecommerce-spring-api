package com.ecommerce.sportcenter.module.base.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class BaseEntity {
    @Id
    @Column(name = "Id", insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "createdDate", updatable = false/*, columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP"*/)
    @CreationTimestamp
    private Date createdDate;

    @Column(name = "updatedDate"/*, columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP"*/)
    @UpdateTimestamp
    private Date updatedDate;

    @Column(name = "deleted_at")
    private Date deletedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
