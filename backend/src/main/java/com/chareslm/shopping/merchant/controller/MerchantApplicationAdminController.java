package com.chareslm.shopping.merchant.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationDetailResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationSummaryResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.AuditRequest;
import com.chareslm.shopping.merchant.service.MerchantApplicationService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 管理端商家双阶段审核与私有资质文件访问 API。
 */
@Validated
@RestController
@RequestMapping("/api/admin/merchant/applications")
@PreAuthorize("hasAuthority('merchant:qualification:audit')")
public class MerchantApplicationAdminController {
    private final MerchantApplicationService service;

    public MerchantApplicationAdminController(MerchantApplicationService service) {
        this.service = service;
    }

    /**
     * 查询指定状态的审核队列。
     */
    @GetMapping
    public ApiResponse<PageResponse<ApplicationSummaryResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.success(service.list(status, page, pageSize));
    }

    /** 获取单个申请的审核详情。 */
    @GetMapping("/{id}")
    public ApiResponse<ApplicationDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    /** 提交资质阶段审核结论，审核人取自当前认证上下文。 */
    @PostMapping("/{id}/qualification-audit")
    public ApiResponse<Void> auditQualification(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        service.auditQualification(id, request, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 提交账号阶段审核结论，审核人取自当前认证上下文。 */
    @PostMapping("/{id}/account-audit")
    public ApiResponse<Void> auditAccount(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        service.auditAccount(id, request, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 重试此前投递失败的账号开通邮件。 */
    @PostMapping("/{id}/credential-email/retry")
    public ApiResponse<Void> retryCredentialEmail(@PathVariable Long id) {
        service.retryCredentialEmail(id, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 下载属于指定申请的私有资质文件。 */
    @GetMapping("/{applicationId}/files/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long applicationId, @PathVariable Long fileId) {
        MerchantApplicationService.DownloadedFile file = service.download(applicationId, fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}
