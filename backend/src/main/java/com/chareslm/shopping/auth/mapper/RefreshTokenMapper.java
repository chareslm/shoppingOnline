package com.chareslm.shopping.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.auth.entity.RefreshToken;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
    @Select("SELECT * FROM refresh_token WHERE token_id = #{tokenId} LIMIT 1")
    RefreshToken selectByTokenId(@Param("tokenId") String tokenId);

    @Update("""
            UPDATE refresh_token
            SET revoked_at = CURRENT_TIMESTAMP(3), revoke_reason = #{reason}, replaced_by_token_id = #{replacementTokenId}
            WHERE token_id = #{tokenId} AND revoked_at IS NULL
            """)
    int revokeIfActive(@Param("tokenId") String tokenId, @Param("reason") String reason,
                       @Param("replacementTokenId") String replacementTokenId);

    @Update("""
            UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP(3), revoke_reason = #{reason}
            WHERE user_id = #{userId} AND device_id = #{deviceId} AND revoked_at IS NULL
            """)
    int revokeActiveByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") Long deviceId,
                                    @Param("reason") String reason);

    @Update("""
            UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP(3), revoke_reason = #{reason}
            WHERE user_id = #{userId} AND revoked_at IS NULL
            """)
    int revokeActiveByUserId(@Param("userId") Long userId, @Param("reason") String reason);
}
