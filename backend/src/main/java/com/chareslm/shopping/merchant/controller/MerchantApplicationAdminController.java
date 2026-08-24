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
 * 管理端商家资质审核、账号开通与私有资质文件访问 API。
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

    /** 提交资质审核结论；通过时同时开通商家账号。审核人取自当前认证上下文。 */
    @PostMapping("/{id}/qualification-audit")
    public ApiResponse<Void> auditQualification(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        service.auditQualification(id, request, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 兼容存量资质已通过、账号尚未开通的申请。 */
    @PostMapping("/{id}/account-audit")
    public ApiResponse<Void> auditAccount(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        service.auditAccount(id, request, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 重试此前投递失败的账号开通或权限变更邮件。 */
    @PostMapping("/{id}/credential-email/retry")
    public ApiResponse<Void> retryCredentialEmail(@PathVariable Long id) {
        service.retryCredentialEmail(id, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 撤销已开通商家经营权限并发送邮件通知。 */
    @PostMapping("/{id}/revoke")
    public ApiResponse<Void> revoke(@PathVariable Long id) {
        service.revokeMerchant(id, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 重新授予已撤销商家经营权限并发送邮件通知。 */
    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable Long id) {
        service.restoreMerchant(id, CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    /** 下载或预览属于指定申请的私有资质文件。图片与 PDF 使用 inline 以便审核页展示。 */
    @GetMapping("/{applicationId}/files/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long applicationId, @PathVariable Long fileId,
                                             @RequestParam(defaultValue = "false") boolean download) {
        MerchantApplicationService.DownloadedFile file = service.download(applicationId, fileId);
        boolean inline = !download && file.contentType() != null
                && (file.contentType().startsWith("image/") || "application/pdf".equals(file.contentType()));
        String contentType = file.contentType() == null || file.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType();
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}
