package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.OmsCartItem;
import com.cloude.shop.mapper.entity.PmsProduct;
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
        return items.stream()
                .map(item -> CartVO.of(item, productMap.get(item.getProductId())))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(Long memberId, Long productId, Integer quantity) {
        PmsProduct product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        OmsCartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<OmsCartItem>()
                .eq(OmsCartItem::getMemberId, memberId)
                .eq(OmsCartItem::getProductId, productId));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemMapper.updateById(existing);
        } else {
            // 可能存在逻辑删除的记录（唯一约束 member_id+product_id），恢复而非新增
            OmsCartItem deleted = cartItemMapper.selectSoftDeleted(memberId, productId);
            if (deleted != null) {
                cartItemMapper.restoreSoftDeleted(memberId, productId, quantity, product.getPrice());
            } else {
                OmsCartItem item = new OmsCartItem();
                item.setMemberId(memberId);
                item.setProductId(productId);
                item.setQuantity(quantity);
                item.setPrice(product.getPrice());
                item.setSelected(1);
                cartItemMapper.insert(item);
            }
        }
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
