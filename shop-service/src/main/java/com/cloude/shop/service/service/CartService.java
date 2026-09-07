package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.OmsCartItem;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.entity.PmsSku;
import com.cloude.shop.mapper.mapper.OmsCartItemMapper;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import com.cloude.shop.service.dto.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final OmsCartItemMapper cartItemMapper;
    private final PmsProductMapper productMapper;
    private final com.cloude.shop.mapper.mapper.PmsSkuMapper skuMapper;

    public List<CartVO> list(Long memberId) {
        List<OmsCartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<OmsCartItem>()
                .eq(OmsCartItem::getMemberId, memberId)
                .orderByDesc(OmsCartItem::getUpdateTime));
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, PmsProduct> productMap = productMapper.selectBatchIds(
                        items.stream().map(OmsCartItem::getProductId).toList()).stream()
                .collect(Collectors.toMap(PmsProduct::getId, Function.identity()));
        List<Long> skuIds = items.stream().map(OmsCartItem::getSkuId)
                .filter(java.util.Objects::nonNull).toList();
        Map<Long, PmsSku> skuMap = skuIds.isEmpty() ? Map.of()
                : skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(PmsSku::getId, Function.identity()));
        return items.stream()
                .map(item -> CartVO.of(item, productMap.get(item.getProductId()),
                        item.getSkuId() != null ? skuMap.get(item.getSkuId()) : null))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(Long memberId, Long productId, Long skuId, Integer quantity) {
        PmsProduct product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        BigDecimal price = product.getPrice();
        String specValues = null;
        if (skuId != null) {
            PmsSku sku = skuMapper.selectById(skuId);
            if (sku == null || !sku.getProductId().equals(productId) || sku.getStatus() != 1) {
                throw new BusinessException("SKU 不存在或已禁用");
            }
            price = sku.getPrice();
            specValues = sku.getSpecValues();
        }
        OmsCartItem existing;
        if (skuId != null) {
            existing = cartItemMapper.selectOne(new LambdaQueryWrapper<OmsCartItem>()
                    .eq(OmsCartItem::getMemberId, memberId)
                    .eq(OmsCartItem::getSkuId, skuId));
        } else {
            existing = cartItemMapper.selectOne(new LambdaQueryWrapper<OmsCartItem>()
                    .eq(OmsCartItem::getMemberId, memberId)
                    .eq(OmsCartItem::getProductId, productId)
                    .isNull(OmsCartItem::getSkuId));
        }
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemMapper.updateById(existing);
        } else {
            // 可能存在逻辑删除的记录，恢复而非新增
            OmsCartItem deleted;
            if (skuId != null) {
                deleted = cartItemMapper.selectSoftDeletedBySku(memberId, skuId);
                if (deleted != null) {
                    cartItemMapper.restoreSoftDeletedBySku(memberId, skuId, quantity, price);
                } else {
                    insertNewCart(memberId, productId, skuId, quantity, price);
                }
            } else {
                deleted = cartItemMapper.selectSoftDeleted(memberId, productId);
                if (deleted != null) {
                    cartItemMapper.restoreSoftDeleted(memberId, productId, quantity, price);
                } else {
                    insertNewCart(memberId, productId, null, quantity, price);
                }
            }
        }
    }

    private void insertNewCart(Long memberId, Long productId, Long skuId, Integer quantity, BigDecimal price) {
        OmsCartItem item = new OmsCartItem();
        item.setMemberId(memberId);
        item.setProductId(productId);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setPrice(price);
        item.setSelected(1);
        cartItemMapper.insert(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long memberId, Long itemId, Integer quantity) {
        OmsCartItem item = getOwnedItem(memberId, itemId);
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSelected(Long memberId, Long itemId, Integer selected) {
        OmsCartItem item = getOwnedItem(memberId, itemId);
        item.setSelected(selected);
        cartItemMapper.updateById(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long itemId) {
        getOwnedItem(memberId, itemId);
        cartItemMapper.deleteById(itemId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void clear(Long memberId) {
        cartItemMapper.delete(new LambdaQueryWrapper<OmsCartItem>()
                .eq(OmsCartItem::getMemberId, memberId));
    }

    private OmsCartItem getOwnedItem(Long memberId, Long itemId) {
        OmsCartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getMemberId().equals(memberId)) {
            throw new BusinessException("购物车条目不存在");
        }
        return item;
    }
}
