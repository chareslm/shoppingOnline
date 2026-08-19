package com.chareslm.shopping.product.service;

import com.chareslm.shopping.product.dto.response.ProductMediaResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ProductMediaService {
    ProductMediaResponse upload(Long operatorId, MultipartFile file);

    ResponseEntity<Resource> download(Long mediaId);
}
