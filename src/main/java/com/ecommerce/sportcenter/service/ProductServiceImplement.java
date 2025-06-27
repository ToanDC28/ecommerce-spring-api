package com.ecommerce.sportcenter.service;

import com.ecommerce.sportcenter.entity.Brand;
import com.ecommerce.sportcenter.entity.Product;
import com.ecommerce.sportcenter.entity.Type;
import com.ecommerce.sportcenter.repository.BrandRepository;
import com.ecommerce.sportcenter.repository.ProductRepository;
import com.ecommerce.sportcenter.repository.TypeRepository;
import com.ecommerce.sportcenter.request.CreateProductRequest;
import com.ecommerce.sportcenter.response.BrandResponse;
import com.ecommerce.sportcenter.response.ProductResponse;
import com.ecommerce.sportcenter.response.TypeResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ProductServiceImplement implements ProductService{
    private final ProductRepository _repository;
    private final BrandRepository _brandRepository;
    private final TypeRepository _typeRepository;
    public ProductServiceImplement(ProductRepository repository, BrandRepository brandRepository, TypeRepository typeRepository) {
        _repository = repository;
        _brandRepository = brandRepository;
        _typeRepository = typeRepository;
    }

    @Override
    public List<ProductResponse> GetAll() {
        log.info("Fetching All Product...");

        List<Product> result = _repository.findAll();
        List<ProductResponse> response = result.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        log.info("Fetched All Product...");
        return response;
    }

    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .createdDate(product.getCreatedDate())
                .updatedDate(product.getUpdatedDate())
                .price(product.getPrice())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .brand(BrandResponse.builder()
                        .id(product.getBrand().getId())
                        .updatedAt(product.getBrand().getUpdatedDate())
                        .createdAt(product.getBrand().getCreatedDate())
                        .name(product.getBrand().getName())
                        .build()
                )
                .type(TypeResponse.builder()
                        .id(product.getType().getId())
                        .updatedAt(product.getType().getUpdatedDate())
                        .createdAt(product.getType().getCreatedDate())
                        .name(product.getType().getName())
                        .build()
                )
                .build();
    }

    @Override
    public ProductResponse CreateProduct(CreateProductRequest request) throws Exception {
        Product existingProduct = _repository.findProductByName(request.getName());
        if(existingProduct != null){
            throw new Exception("The product named '" + request.getName() + "' already exists");
        }
        Optional<Brand> brand = _brandRepository.findById(request.getBrandId());
        if (brand.isEmpty()){
            throw new Exception("Brand Can not be found");
        }
        Optional<Type> type = _typeRepository.findById(request.getTypeId());
        if (type.isEmpty()){
            throw new Exception("Type Can not be found");
        }
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .brand(brand.get())
                .type(type.get())
                .build();
        Product savedProduct = _repository.save(product);
        return convertToResponse(savedProduct);
    }

    @Override
    public ProductResponse GetProductByID(int id) {
        log.info("Fetching Product...");

        Optional<Product> result = _repository.findById(id);
        if (result.isEmpty()){
            return null;
        }

        ProductResponse response = convertToResponse(result.get());
        log.info("Fetched Product...");
        return response;
    }

    @Override
    public Page<ProductResponse> GetAllWithPagination(Pageable page, Integer brandId, Integer typeId, String keyword) {
        log.info("Fetching All Product With Pagination...");

        Specification<Product> spec = Specification.not(null);
        if(brandId != null){
            spec = spec.and((root, query, builder) -> builder.equal(root.get("brand").get("id"), brandId));
        }
        if(typeId != null){
            spec = spec.and((root, query, builder) -> builder.equal(root.get("type").get("id"), typeId));
        }
        if(keyword != null && !keyword.isEmpty()){
            spec = spec.and((root, query, builder) -> builder.like(root.get("name"), "%" + keyword + "%"));
        }
        Page<Product> result = _repository.findAll(spec, page);
        Page<ProductResponse> response = result
                .map(this::convertToResponse);
        log.info("Fetched All Product With Pagination...");
        return response;
    }
}
