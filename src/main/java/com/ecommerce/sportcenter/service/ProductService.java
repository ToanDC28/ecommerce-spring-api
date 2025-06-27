package com.ecommerce.sportcenter.service;

import com.ecommerce.sportcenter.request.CreateProductRequest;
import com.ecommerce.sportcenter.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    List<ProductResponse> GetAll();
    ProductResponse CreateProduct(CreateProductRequest request) throws Exception;
    ProductResponse GetProductByID(int id);
    Page<ProductResponse> GetAllWithPagination(Pageable page, Integer brandId, Integer typeId, String keyword);
}
