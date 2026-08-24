package com.chareslm.shopping.merchant.service;

import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationCreatedResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationDetailResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationRequest;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationSummaryResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.AuditRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 编排商家入驻申请、资质审核、账号开通和私有资质文件访问。
 *
 * <p>实现必须在服务端确定审核人和文件归属，不能信任客户端传入的数据范围。</p>
 */
public interface MerchantApplicationService {
    /**
     * 提交入驻资料及至少一份资质文件。
     */
    ApplicationCreatedResponse submit(ApplicationRequest request, List<MultipartFile> files);

    /**
     * 按状态分页查询审核队列；状态为空时查询全部申请。
     */
    PageResponse<ApplicationSummaryResponse> list(String status, int page, int pageSize);

    /**
     * 获取申请详情，敏感证件号码以脱敏形式返回。
     */
    ApplicationDetailResponse detail(Long id);

    /**
     * 完成资质审核。通过时同时开通或复用商家账号并建档店铺，无需再走账号审核。
     */
    void auditQualification(Long id, AuditRequest request, Long auditorId);

    /**
     * 兼容尚未开通的 {@code QUALIFICATION_APPROVED} 存量申请；通过时创建或复用账号并建立店铺。
     */
    void auditAccount(Long id, AuditRequest request, Long auditorId);

    /**
     * 对已开通但邮件投递失败的申请重新发送通知。
     */
    void retryCredentialEmail(Long id, Long auditorId);

    /**
     * 撤销已开通商家：店铺置为暂停、收回商家角色并通知邮箱。
     */
    void revokeMerchant(Long id, Long auditorId);

    /**
     * 重新授予已撤销商家：店铺恢复营业、补回商家角色并通知邮箱。
     */
    void restoreMerchant(Long id, Long auditorId);

    /**
     * 下载属于指定申请的资质文件，避免仅凭文件 ID 越权读取。
     */
    DownloadedFile download(Long applicationId, Long fileId);

    record DownloadedFile(Resource resource, String originalName, String contentType) {
    }
}
