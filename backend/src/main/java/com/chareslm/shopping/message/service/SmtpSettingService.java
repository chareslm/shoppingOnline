package com.chareslm.shopping.message.service;

import com.chareslm.shopping.message.dto.request.UpdateSmtpSettingRequest;
import com.chareslm.shopping.message.dto.response.SmtpSettingResponse;

public interface SmtpSettingService {
    SmtpSettingResponse current();

    SmtpSettingResponse update(Long operatorUserId, UpdateSmtpSettingRequest request);

    void requireOperatorPassword(Long operatorUserId, String currentPassword);
}
