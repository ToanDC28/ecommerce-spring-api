package com.ecommerce.sportcenter.module.workorder.service.impl;

import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Upload ảnh nghiệm thu lên Zipline tự host (giữ API key ở server),
 * DB chỉ lưu URL trả về. FE hiển thị <img src=url> trực tiếp.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZiplineClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.zipline.base-url:http://localhost:3001}")
    private String baseUrl;

    @Value("${app.zipline.api-key:}")
    private String apiKey;

    /**
     * @return absolute URL ảnh (baseUrl + path Zipline trả về).
     */
    public String upload(byte[] bytes, String fileName, String contentType) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("change-me")) {
            throw new IllegalStateException("Zipline API key chưa cấu hình (app.zipline.api-key)");
        }
        try {
            ByteArrayResource body = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("file", body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("authorization", apiKey);
            // format=json để Zipline luôn trả JSON kể cả khi gọi API thuần
            String url = baseUrl.replaceAll("/+$", "") + "/api/upload?format=json";

            var response = new RestTemplate().postForEntity(url, new HttpEntity<>(form, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode files = root.path("files");
            if (!files.isArray() || files.isEmpty() || !files.get(0).has("url")) {
                throw new IllegalStateException("Zipline response thiếu files[].url: " + response.getBody());
            }
            String path = files.get(0).path("url").asText();
            String absolute = path.startsWith("http") ? path : baseUrl.replaceAll("/+$", "") + path;
            log.info("Zipline uploaded - file={}, url={}", fileName, absolute);
            return absolute;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot parse Zipline response: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Image host unreachable: " + e.getMessage(), e);
        }
    }
}
