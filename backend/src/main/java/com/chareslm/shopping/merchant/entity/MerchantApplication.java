package com.chareslm.shopping.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 商家入驻申请及其双阶段审核、账号开通和邮件投递状态。
 */
@Getter
@Setter
@TableName("merchant_application")
public class MerchantApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantType;
    private String shopName;
    private String subjectName;
    private String unifiedSocialCreditCode;
    private String responsiblePersonName;
    private String identityDocumentType;
    private String identityDocumentNumber;
    private String contactPhone;
    private String contactEmail;
    private String status;
    private String rejectionReason;
    private Long accountUserId;
    private Boolean accountReused;
    private String emailDeliveryStatus;
    private Long qualificationAuditedBy;
    private LocalDateTime qualificationAuditedAt;
    private Long accountAuditedBy;
    private LocalDateTime accountAuditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
