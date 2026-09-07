package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import com.cloude.shop.service.dto.ProductQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务（详情缓存 + 分页搜索 + 后台管理）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final PmsProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 商品详情（Redis 缓存 1 小时 + 空值缓存 60s 防穿透）
     */
    public PmsProduct detail(Long id) {
        String key = RedisKeyConstant.PRODUCT_CACHE + id;
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                if (RedisKeyConstant.NULL_PLACEHOLDER.equals(cached)) {
                    // 空值占位：不存在的商品直接拒绝，防止穿透到 DB
                    throw new BusinessException("商品不存在或已下架");
                }
                return objectMapper.readValue(cached, PmsProduct.class);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignore) {
        }
        PmsProduct product = productMapper.selectById(id);
        if (product == null || product.getStatus() != 1) {
            stringRedisTemplate.opsForValue().set(key, RedisKeyConstant.NULL_PLACEHOLDER, Duration.ofSeconds(60));
            throw new BusinessException("商品不存在或已下架");
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(product),
                    Duration.ofSeconds(RedisKeyConstant.PRODUCT_EXPIRE_SECONDS));
        } catch (Exception ignore) {
        }
        return product;
    }

    /**
     * 前台商品分页（分类/关键字搜索，LIKE 实现；生产可扩展 ES）
     */
    public Page<PmsProduct> portalPage(Long categoryId, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getStatus, 1)
                .eq(categoryId != null, PmsProduct::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), PmsProduct::getName, keyword)
                .orderByDesc(PmsProduct::getSale);
        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 管理端商品详情：不过滤上下架状态、不走前台空值占位缓存，
     * 保证管理员可以查看/编辑下架商品。
     */
    public PmsProduct detailForAdmin(Long id) {
        PmsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    /**
     * 热销推荐（缓存商品 ID 列表 10 分钟，商品数据实时批量回查，兼顾热度与新鲜度）
     */
    public List<PmsProduct> hot(int limit) {
        String key = RedisKeyConstant.HOT_PRODUCT_CACHE + limit;
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                List<Long> ids = objectMapper.readValue(cached, new TypeReference<List<Long>>() {
                });
                return enrichByIds(ids);
            }
        } catch (Exception ignore) {
        }
        List<Long> ids = productMapper.selectList(new LambdaQueryWrapper<PmsProduct>()
                        .select(PmsProduct::getId)
                        .eq(PmsProduct::getStatus, 1)
                        .orderByDesc(PmsProduct::getSale)
                        .last("limit " + limit)).stream()
                .map(PmsProduct::getId)
                .toList();
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ids), Duration.ofMinutes(10));
        } catch (Exception ignore) {
        }
        return enrichByIds(ids);
    }

    private List<PmsProduct> enrichByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, PmsProduct> productMap = productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(PmsProduct::getId, Function.identity()));
        return ids.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() == 1)
                .collect(Collectors.toList());
    }

    /**
     * 后台商品分页
     */
    public Page<PmsProduct> adminPage(ProductQuery query) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<PmsProduct>()
                .like(StringUtils.hasText(query.getKeyword()), PmsProduct::getName, query.getKeyword())
                .eq(query.getCategoryId() != null, PmsProduct::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, PmsProduct::getStatus, query.getStatus())
                .orderByDesc(PmsProduct::getId);
        return productMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    public void create(PmsProduct product) {
        product.setId(null);
        if (product.getAvailableStock() == null) {
            product.setAvailableStock(0);
        }
        product.setLockedStock(0);
        product.setSoldStock(0);
        if (product.getSale() == null) {
            product.setSale(0);
        }
        productMapper.insert(product);
    }

    public void update(PmsProduct product) {
        PmsProduct db = productMapper.selectById(product.getId());
        if (db == null) {
            throw new BusinessException("商品不存在");
        }
        // 库存三态字段不允许通过后台直接修改，防止破坏一致性
        product.setAvailableStock(null);
        product.setLockedStock(null);
        product.setSoldStock(null);
        productMapper.updateById(product);
        evictCache(product.getId());
    }

    /**
     * 上架/下架
     */
    public void changeStatus(Long id, Integer status) {
        PmsProduct db = productMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("商品不存在");
        }
        PmsProduct update = new PmsProduct();
        update.setId(id);
        update.setStatus(status);
        productMapper.updateById(update);
        evictCache(id);
    }

    /**
     * 后台删除商品（仅允许删除无锁定库存的商品）
     */
    public void delete(Long id) {
        PmsProduct db = productMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("商品不存在");
        }
        if (db.getLockedStock() > 0) {
            throw new BusinessException("存在未支付订单锁定库存，无法删除");
        }
        productMapper.deleteById(id);
        evictCache(id);
    }

    private void evictCache(Long productId) {
        stringRedisTemplate.delete(RedisKeyConstant.PRODUCT_CACHE + productId);
    }
}
