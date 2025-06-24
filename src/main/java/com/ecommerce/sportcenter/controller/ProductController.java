package com.ecommerce.sportcenter.controller;

import com.ecommerce.sportcenter.request.CreateProductRequest;
import com.ecommerce.sportcenter.response.BrandResponse;
import com.ecommerce.sportcenter.response.ProductResponse;
import com.ecommerce.sportcenter.response.TypeResponse;
import com.ecommerce.sportcenter.service.BrandService;
import com.ecommerce.sportcenter.service.ProductService;
import com.ecommerce.sportcenter.service.TypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService _productService;
    private final BrandService _brandService;
    private final TypeService _typeService;

    public ProductController(ProductService productService, BrandService brandService, TypeService typeService) {
        _productService = productService;
        _brandService = brandService;
        _typeService = typeService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> GetProductById(@PathVariable("id") int id){
        ProductResponse response = _productService.GetProductByID(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> GetAll(){
        List<ProductResponse> response = _productService.GetAll();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> CreateProduct(@RequestBody CreateProductRequest request) throws Exception {
        ProductResponse response = _productService.CreateProduct(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/brand")
    public ResponseEntity<List<BrandResponse>> GetAllBrand(){
        List<BrandResponse> response = _brandService.GetAll();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/type")
    public ResponseEntity<List<TypeResponse>> GetAllType(){
        List<TypeResponse> response = _typeService.GetAll();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
