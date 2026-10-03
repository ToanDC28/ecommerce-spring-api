package com.ecommerce.sportcenter.module.customer.entity;

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
@Table(name = "customer", indexes = {
        @Index(name = "idx_customer_code", columnList = "code"),
        @Index(name = "idx_customer_name", columnList = "name"),
        @Index(name = "idx_customer_phone", columnList = "phone")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE customer SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Customer extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // KH-001 auto

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone; // chuẩn hóa 0xxxxxxxxx, unique khi có giá trị (check ở service)

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private CustomerType type = CustomerType.LE_QUEN;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
