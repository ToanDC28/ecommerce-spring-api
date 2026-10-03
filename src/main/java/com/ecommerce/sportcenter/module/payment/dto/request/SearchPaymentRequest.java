package com.ecommerce.sportcenter.module.payment.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import com.ecommerce.sportcenter.module.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPaymentRequest extends BaseFilterRequest {

    @Schema(description = "Filter by invoice id")
    private Integer invoiceId;

    @Schema(description = "Filter by method")
    private PaymentMethod method;

    @Schema(description = "Filter by status")
    private PaymentStatus status;

    @Schema(description = "From date")
    private LocalDate from;

    @Schema(description = "To date")
    private LocalDate to;

    private static final Set<String> ALLOWED = Set.of("id", "code", "paymentDate", "amount", "createdDate");

    @Override
    public Sort toSort() {
        String property = getSortBy();
        if (property == null || !ALLOWED.contains(property)) {
            property = "id";
        }
        Sort.Direction direction = "DESC".equalsIgnoreCase(getSortDir())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
