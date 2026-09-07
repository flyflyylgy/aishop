package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.mapper.entity.PmsSku;
import com.cloude.shop.mapper.mapper.PmsSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SKU 服务 —— SKU CRUD + 库存 CAS（防超卖）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuService {

    private final PmsSkuMapper skuMapper;

    /**
     * 查商品的 SKU 列表（仅启用）
     */
    public List<PmsSku> listByProduct(Long productId) {
        return skuMapper.selectList(new LambdaQueryWrapper<PmsSku>()
                .eq(PmsSku::getProductId, productId)
                .eq(PmsSku::getStatus, 1)
                .orderByAsc(PmsSku::getId));
    }

    /**
     * 查商品的全部 SKU（含禁用，后台用）
     */
    public List<PmsSku> listAllByProduct(Long productId) {
        return skuMapper.selectList(new LambdaQueryWrapper<PmsSku>()
                .eq(PmsSku::getProductId, productId)
                .orderByAsc(PmsSku::getId));
    }

    public PmsSku getById(Long id) {
        return skuMapper.selectById(id);
    }

    /**
     * 后台批量保存商品 SKU（先删后插）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(Long productId, List<PmsSku> skus) {
        // 物理删除旧 SKU（逻辑删除的也会被清掉，避免脏数据）
        skuMapper.delete(new LambdaQueryWrapper<PmsSku>().eq(PmsSku::getProductId, productId));
        if (skus == null || skus.isEmpty()) {
            return;
        }
        for (PmsSku sku : skus) {
            sku.setId(null);
            sku.setProductId(productId);
            if (sku.getStatus() == null) {
                sku.setStatus(1);
            }
            if (sku.getLockedStock() == null) {
                sku.setLockedStock(0);
            }
            if (sku.getSoldStock() == null) {
                sku.setSoldStock(0);
            }
            skuMapper.insert(sku);
        }
    }
}
