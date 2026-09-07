package com.cloude.shop.service.service;

import com.cloude.shop.common.api.ResultCode;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存服务 CAS 语义测试（Mockito，无需数据库）
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private PmsProductMapper productMapper;

    @InjectMocks
    private StockService stockService;

    @Test
    void lockStockSuccessWhenAffected() {
        when(productMapper.lockStock(1L, 2)).thenReturn(1);
        assertDoesNotThrow(() -> stockService.lockStock(1L, 2));
        verify(productMapper).lockStock(1L, 2);
    }

    @Test
    void lockStockThrowsWhenInsufficient() {
        // CAS affected=0 表示库存不足或并发竞争
        when(productMapper.lockStock(1L, 999)).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.lockStock(1L, 999));
        assertEquals(ResultCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void confirmSoldDelegatesToMapper() {
        when(productMapper.confirmSold(1L, 3)).thenReturn(1);
        stockService.confirmSold(1L, 3);
        verify(productMapper).confirmSold(1L, 3);
    }

    @Test
    void releaseStockDelegatesToMapper() {
        when(productMapper.releaseStock(1L, 3)).thenReturn(1);
        stockService.releaseStock(1L, 3);
        verify(productMapper).releaseStock(1L, 3);
    }
}
