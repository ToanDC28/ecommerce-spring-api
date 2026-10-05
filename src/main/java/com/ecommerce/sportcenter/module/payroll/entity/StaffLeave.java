package com.ecommerce.sportcenter.module.payroll.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "staff_leave", indexes = {
        @Index(name = "idx_leave_staff", columnList = "staff_id"),
        @Index(name = "idx_leave_date", columnList = "leave_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_leave_staff_date", columnNames = {"staff_id", "leave_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE staff_leave SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class StaffLeave extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
