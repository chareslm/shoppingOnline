package com.chareslm.shopping.product.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.product.dto.response.ProductMediaResponse;
import com.chareslm.shopping.product.service.ProductMediaService;
import com.chareslm.shopping.security.context.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProductMediaController {
    private final ProductMediaService productMediaService;

    public ProductMediaController(ProductMediaService productMediaService) {
        this.productMediaService = productMediaService;
    }

    @PostMapping(value = "/merchant/product-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('product:create','product:update')")
    public ApiResponse<ProductMediaResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(productMediaService.upload(CurrentUser.require().userId(), file));
    }

    @GetMapping("/product-media/{mediaId}")
    public ResponseEntity<Resource> download(@PathVariable Long mediaId) {
        return productMediaService.download(mediaId);
    }
}
