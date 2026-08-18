package com.chareslm.shopping.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.merchant.entity.MerchantQualificationFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家资质文件元数据查询；下载查询必须同时匹配申请 ID 与文件 ID。
 */
public interface MerchantQualificationFileMapper extends BaseMapper<MerchantQualificationFile> {
    @Select("SELECT * FROM merchant_qualification_file WHERE application_id = #{applicationId} ORDER BY id")
    List<MerchantQualificationFile> selectByApplicationId(@Param("applicationId") Long applicationId);

    @Select("""
            SELECT * FROM merchant_qualification_file
            WHERE id = #{fileId} AND application_id = #{applicationId}
            """)
    MerchantQualificationFile selectOwnedFile(@Param("applicationId") Long applicationId,
                                               @Param("fileId") Long fileId);
}
