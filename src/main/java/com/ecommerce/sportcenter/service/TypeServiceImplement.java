package com.ecommerce.sportcenter.service;

import com.ecommerce.sportcenter.entity.Type;
import com.ecommerce.sportcenter.repository.TypeRepository;
import com.ecommerce.sportcenter.response.TypeResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class TypeServiceImplement implements TypeService{
    private final TypeRepository _repository;

    public TypeServiceImplement(TypeRepository repository) {
        _repository = repository;
    }

    @Override
    public List<TypeResponse> GetAll() {
        log.info("Fetching All Type...");
        List<Type> result = _repository.findAll();
        List<TypeResponse> response = result.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        log.info("Fetched All Type...");
        return response;
    }

    private TypeResponse convertToResponse(Type type) {
        return TypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .updatedAt(type.getUpdatedDate())
                .createdAt(type.getCreatedDate())
                .build();
    }
}
