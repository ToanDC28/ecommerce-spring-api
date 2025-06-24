package com.ecommerce.sportcenter.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TypeResponse {
    private int id;
    private String name;
    private Date createdAt;
    private Date updatedAt;
}
