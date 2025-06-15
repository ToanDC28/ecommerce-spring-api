package com.ecommerce.sportcenter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@MappedSuperclass
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

//    @Column(name = "active", columnDefinition = "boolean default false")
//    private boolean active;
}
