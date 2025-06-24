package com.ecommerce.sportcenter.service;


import com.ecommerce.sportcenter.entity.Brand;
import com.ecommerce.sportcenter.repository.BrandRepository;
import com.ecommerce.sportcenter.response.BrandResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class BrandServiceImplement implements BrandService {

    private static final Logger log = LogManager.getLogger(BrandServiceImplement.class);
    private final BrandRepository _repository;

    public BrandServiceImplement(BrandRepository repository) {
        _repository = repository;
    }


    @Override
    public List<BrandResponse> GetAll() {
        log.info("Fetching All Brand...");

        List<Brand> result = _repository.findAll();
        List<BrandResponse> response = result.stream()
                .map(this::convertToBrandResponse)
                .collect(Collectors.toList());
        log.info("Fetched All Brand...");
        return response;
    }

    private BrandResponse convertToBrandResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .createdAt(brand.getCreatedDate())
                .updatedAt(brand.getUpdatedDate())
                .build();

    }
}
