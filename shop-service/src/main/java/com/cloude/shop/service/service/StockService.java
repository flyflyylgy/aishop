package com.cloude.shop.service.service;

import com.cloude.shop.common.api.ResultCode;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import com.cloude.shop.mapper.mapper.PmsSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务 —— 库存三态分离（available / locked / sold）
 * 所有变更基于条件更新（CAS），affected=0 即失败，从根本上防止超卖
 * has_sku=0 商品走 product 级库存；has_sku=1 商品走 sku 级库存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final PmsProductMapper productMapper;
    private final PmsSkuMapper skuMapper;

    /**
     * 下单锁库存（商品级，has_sku=0）：available - n，locked + n
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void lockStock(Long productId, Integer quantity) {
        int affected = productMapper.lockStock(productId, quantity);
        if (affected == 0) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                    "商品库存不足(商品ID:" + productId + ")");
        }
    }

    /**
     * 下单锁 SKU 库存（has_sku=1）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void lockSkuStock(Long skuId, Integer quantity) {
        int affected = skuMapper.lockSkuStock(skuId, quantity);
        if (affected == 0) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                    "SKU库存不足(SKU ID:" + skuId + ")");
        }
    }

    /**
     * 支付确认（商品级）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmSold(Long productId, Integer quantity) {
        productMapper.confirmSold(productId, quantity);
    }

    /**
     * 支付确认（SKU 级）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmSkuSold(Long skuId, Integer quantity) {
        skuMapper.confirmSkuSold(skuId, quantity);
    }

    /**
     * 关单释放（商品级）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void releaseStock(Long productId, Integer quantity) {
        productMapper.releaseStock(productId, quantity);
    }

    /**
     * 关单释放（SKU 级）
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void releaseSkuStock(Long skuId, Integer quantity) {
        skuMapper.releaseSkuStock(skuId, quantity);
    }
}
