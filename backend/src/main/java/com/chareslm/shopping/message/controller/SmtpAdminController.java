package com.chareslm.shopping.message.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.message.dto.request.TestSmtpRequest;
import com.chareslm.shopping.message.dto.request.UpdateSmtpSettingRequest;
import com.chareslm.shopping.message.dto.response.SmtpSettingResponse;
import com.chareslm.shopping.message.service.MailService;
import com.chareslm.shopping.message.service.SmtpSettingService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system/smtp")
public class SmtpAdminController {
    private final SmtpSettingService smtpSettingService;
    private final MailService mailService;

    public SmtpAdminController(SmtpSettingService smtpSettingService, MailService mailService) {
        this.smtpSettingService = smtpSettingService;
        this.mailService = mailService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:smtp:view')")
    public ApiResponse<SmtpSettingResponse> current() {
        return ApiResponse.success(smtpSettingService.current());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:smtp:update')")
    public ApiResponse<SmtpSettingResponse> update(@Valid @RequestBody UpdateSmtpSettingRequest request) {
        return ApiResponse.success(smtpSettingService.update(CurrentUser.require().userId(), request));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('system:smtp:update')")
    public ApiResponse<Void> test(@Valid @RequestBody TestSmtpRequest request) {
        Long operatorId = CurrentUser.require().userId();
        smtpSettingService.requireOperatorPassword(operatorId, request.currentPassword());
        SmtpSettingResponse current = smtpSettingService.current();
        String to = hasText(request.to()) ? request.to().trim()
                : hasText(current.fromAddress()) ? current.fromAddress()
                : current.username();
        if (!hasText(to) || !to.contains("@")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.code(),
                    "请填写收件人，或先保存发件人邮箱");
        }
        if (!mailService.isEnabled()) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED.code(),
                    "SMTP 已关闭。新建账号不会发信，初始密码为 123456QWERqwer!@");
        }
        try {
            mailService.sendTestMessage(to);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED.code(), exception.getMessage());
        }
        return ApiResponse.success(null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
