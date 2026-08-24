package com.chareslm.shopping.merchant.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationCreatedResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationRequest;
import com.chareslm.shopping.merchant.service.MerchantApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 面向访客的商家入驻申请 API。
 */
@RestController
@RequestMapping("/api/merchant/applications")
public class MerchantApplicationController {
    private final MerchantApplicationService service;

    public MerchantApplicationController(MerchantApplicationService service) {
        this.service = service;
    }

    /**
     * 接收 JSON 申请表和资质附件组成的 multipart 请求。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ApplicationCreatedResponse> submit(
            @Valid @RequestPart("application") ApplicationRequest application,
            @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.success(service.submit(application, files));
    }
}
