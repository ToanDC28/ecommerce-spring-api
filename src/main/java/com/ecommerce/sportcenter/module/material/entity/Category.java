package com.ecommerce.sportcenter.module.material.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_code", columnList = "code"),
        @Index(name = "idx_category_name", columnList = "name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE category SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Category extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // e.g. CAT-THEP-TAM

    @Column(name = "name", nullable = false, length = 255)
    private String name; // e.g. Thép tấm

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
