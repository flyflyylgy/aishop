package com.cloude.shop.mapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloude.shop.mapper.entity.PmsSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * SKU Mapper —— SKU 库存 CAS 流转（防超卖）
 */
public interface PmsSkuMapper extends BaseMapper<PmsSku> {

    /**
     * 下单锁 SKU 库存：available - n，locked + n
     */
    @Update("UPDATE pms_sku SET available_stock = available_stock - #{quantity}, "
            + "locked_stock = locked_stock + #{quantity} "
            + "WHERE id = #{skuId} AND available_stock >= #{quantity} AND delete_flag = 0 AND status = 1")
    int lockSkuStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 支付确认：locked - n，sold + n
     */
    @Update("UPDATE pms_sku SET locked_stock = locked_stock - #{quantity}, "
            + "sold_stock = sold_stock + #{quantity} "
            + "WHERE id = #{skuId}")
    int confirmSkuSold(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 关单释放：locked - n，available + n
     */
    @Update("UPDATE pms_sku SET locked_stock = locked_stock - #{quantity}, "
            + "available_stock = available_stock + #{quantity} "
            + "WHERE id = #{skuId}")
    int releaseSkuStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
