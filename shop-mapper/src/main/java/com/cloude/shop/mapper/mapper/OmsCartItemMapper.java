package com.cloude.shop.mapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloude.shop.mapper.entity.OmsCartItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface OmsCartItemMapper extends BaseMapper<OmsCartItem> {

    @Select("SELECT * FROM oms_cart_item WHERE member_id = #{memberId} AND product_id = #{productId} AND delete_flag = 1 LIMIT 1")
    OmsCartItem selectSoftDeleted(@Param("memberId") Long memberId, @Param("productId") Long productId);

    @Update("UPDATE oms_cart_item SET delete_flag = 0, quantity = #{quantity}, price = #{price}, selected = 1, update_time = NOW() WHERE member_id = #{memberId} AND product_id = #{productId} AND delete_flag = 1")
    int restoreSoftDeleted(@Param("memberId") Long memberId, @Param("productId") Long productId, @Param("quantity") Integer quantity, @Param("price") BigDecimal price);
}
