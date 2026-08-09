package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.UserDevice;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserDeviceMapper extends BaseMapper<UserDevice> {
    @Select("SELECT * FROM user_device WHERE user_id = #{userId} AND device_id = #{deviceId} LIMIT 1")
    UserDevice selectByUserAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Update("""
            UPDATE user_device SET device_type = #{deviceType}, device_name = #{deviceName},
                app_version = #{appVersion}, last_ip = #{lastIp}, last_active_at = CURRENT_TIMESTAMP(3), status = 'ACTIVE'
            WHERE id = #{id}
            """)
    void markActive(@Param("id") Long id, @Param("deviceType") String deviceType,
                    @Param("deviceName") String deviceName, @Param("appVersion") String appVersion,
                    @Param("lastIp") String lastIp);
}
