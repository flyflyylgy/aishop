package com.cloude.shop.mapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloude.shop.mapper.entity.PmsProduct;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper —— 库存三态分离（available / locked / sold）
 * 所有库存变更均使用条件更新（CAS），affected=0 即视为失败，防止超卖
 */
public interface PmsProductMapper extends BaseMapper<PmsProduct> {

    /**
     * 下单锁库存：available - n，locked + n；available 不足时更新 0 行
     */
    @Update("UPDATE pms_product SET available_stock = available_stock - #{quantity}, "
            + "locked_stock = locked_stock + #{quantity} "
            + "WHERE id = #{productId} AND available_stock >= #{quantity} AND delete_flag = 0")
    int lockStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 支付成功：locked - n，sold + n（与订单状态 CAS 同一事务）
     */
    @Update("UPDATE pms_product SET locked_stock = locked_stock - #{quantity}, "
            + "sold_stock = sold_stock + #{quantity}, sale = sale + #{quantity} "
            + "WHERE id = #{productId}")
    int confirmSold(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 关单释放库存：locked - n，available + n
     */
    @Update("UPDATE pms_product SET locked_stock = locked_stock - #{quantity}, "
            + "available_stock = available_stock + #{quantity} "
            + "WHERE id = #{productId}")
    int releaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
