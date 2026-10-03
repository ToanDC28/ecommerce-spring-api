package com.ecommerce.sportcenter.module.role.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Fine-grained permission for RBAC, stored in the {@code permissions} table.
 * Roles aggregate permissions via {@code role_permissions(roleId, permissionId)};
 * controllers enforce them via {@code @PreAuthorize("hasAuthority('PRODUCT_WRITE')")}.
 *
 * Grouped by the ecommerce / auto-parts domains:
 * catalog, inventory, supplier/purchasing, sales/invoice, HR/payroll, users.
 */
@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permission_name", columnList = "Name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE permissions SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Permission extends BaseEntity {

    @Column(name = "Name", unique = true, nullable = false)
    private String name; // e.g. PRODUCT_READ, SUPPLIER_WRITE, USER_READ

    @Column(name = "Description")
    private String description;
}
