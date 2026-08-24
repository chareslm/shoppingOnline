package com.chareslm.shopping.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 资质文件元数据；二进制内容保存在私有存储中，{@code storageKey} 不对客户端暴露。
 */
@Getter
@Setter
@TableName("merchant_qualification_file")
public class MerchantQualificationFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private String originalName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
}
