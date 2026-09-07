package com.cloude.shop.service.service;

import com.cloude.shop.common.api.ResultCode;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务 —— 库存三态分离（available / locked / sold）
 * 所有变更基于条件更新（CAS），affected=0 即失败，从根本上防止超卖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final PmsProductMapper productMapper;

    /**
     * 下单锁库存：available - n，locked + n
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
     * 支付确认：locked - n，sold + n
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmSold(Long productId, Integer quantity) {
        productMapper.confirmSold(productId, quantity);
    }

    /**
     * 关单释放：locked - n，available + n
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void releaseStock(Long productId, Integer quantity) {
        productMapper.releaseStock(productId, quantity);
    }
}
