package com.cloude.shop.mapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloude.shop.mapper.entity.UmsMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 站内信 Mapper
 */
public interface UmsMessageMapper extends BaseMapper<UmsMessage> {

    /**
     * 单条已读（限定本人，防越权）
     */
    @Update("UPDATE ums_message SET is_read = 1 WHERE id = #{id} AND member_id = #{memberId} AND is_read = 0")
    int markRead(@Param("id") Long id, @Param("memberId") Long memberId);

    /**
     * 全部已读（本人未读）
     */
    @Update("UPDATE ums_message SET is_read = 1 WHERE member_id = #{memberId} AND is_read = 0")
    int markAllRead(@Param("memberId") Long memberId);
}
