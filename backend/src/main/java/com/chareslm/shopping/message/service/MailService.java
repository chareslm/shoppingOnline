package com.chareslm.shopping.message.service;

/**
 * 平台事务邮件发送端口。
 *
 * <p>实现以运行时异常报告投递失败，由业务调用方决定重试或降级语义。</p>
 */
public interface MailService {
    /**
     * 向新建商家账号发送一次性临时密码。
     *
     * @throws RuntimeException SMTP 配置无效或邮件服务器未接受投递时抛出
     */
    void sendMerchantCredential(String email, String shopName, String temporaryPassword);

    /**
     * 通知已有账号其商家权限已启用。
     *
     * @throws RuntimeException SMTP 配置无效或邮件服务器未接受投递时抛出
     */
    void sendMerchantEnabledNotice(String email, String shopName);
}
