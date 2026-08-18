package com.chareslm.shopping.message.service;

/**
 * 平台事务邮件发送端口。
 *
 * <p>实现以运行时异常报告投递失败，由业务调用方决定重试或降级语义。</p>
 */
public interface MailService {
    /**
     * @return true when a host and from-address are available from saved SMTP settings or environment fallback
     */
    boolean isConfigured();

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

    /**
     * 通知商家其店铺经营权限已被撤销。
     *
     * @throws RuntimeException SMTP 配置无效或邮件服务器未接受投递时抛出
     */
    void sendMerchantRevokedNotice(String email, String shopName);

    /**
     * 通知商家其店铺经营权限已重新授予。
     *
     * @throws RuntimeException SMTP 配置无效或邮件服务器未接受投递时抛出
     */
    void sendMerchantRestoredNotice(String email, String shopName);

    /**
     * 向系统管理员手动创建的账号发送登录标识和一次性临时密码。
     *
     * @throws RuntimeException SMTP 配置无效或邮件服务器未接受投递时抛出
     */
    void sendAccountCredential(String email, String loginHint, String temporaryPassword);

    /**
     * 向指定地址发送一封测试邮件，用于管理端校验运行时 SMTP。
     *
     * @throws RuntimeException SMTP 配置无效或邮件服务器未接受投递时抛出
     */
    void sendTestMessage(String email);
}
