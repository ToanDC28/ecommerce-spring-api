package com.ecommerce.sportcenter.module.supplier.entity;

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
 * Parts supplier (manufacturer / distributor).
 * Separate table — suppliers are NOT users and have no login.
 * Managed by ADMIN via /api/suppliers.
 */
@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_supplier_code", columnList = "Code"),
        @Index(name = "idx_supplier_name", columnList = "Name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE suppliers SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Supplier extends BaseEntity {

    @Column(name = "Code", unique = true, nullable = false)
    private String code; // e.g. SUP-001, auto-generated when blank

    @Column(name = "Name", nullable = false)
    private String name;

    @Column(name = "TaxCode")
    private String taxCode;

    @Column(name = "Phone")
    private String phone;

    @Column(name = "Email")
    private String email;

    @Column(name = "Address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentTerm")
    @Builder.Default
    private PaymentTerm paymentTerm = PaymentTerm.NET_30;

    @Column(name = "IsActive", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "CurrentDebt", nullable = false)
    @Builder.Default
    private long currentDebt = 0L;
}
