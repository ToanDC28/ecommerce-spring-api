package com.ecommerce.sportcenter.module.workorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderAttachmentResponse {
    private int id;
    private String fileName;
    private String url;
    private String contentType;
    private long sizeBytes;
    private String uploadedBy;
}
