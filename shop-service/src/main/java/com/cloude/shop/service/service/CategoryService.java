package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.common.constant.RedisKeyConstant;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.PmsCategory;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.mapper.PmsCategoryMapper;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务（含分类树缓存）
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final PmsCategoryMapper categoryMapper;
    private final PmsProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 分类节点
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CategoryNode extends PmsCategory {
        private List<CategoryNode> children = new ArrayList<>();
    }

    /**
     * 前台分类树（缓存 1 小时）
     */
    public List<CategoryNode> tree() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstant.CATEGORY_TREE_CACHE);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {
                });
            }
        } catch (Exception ignore) {
        }
        List<CategoryNode> tree = buildTree(null);
        try {
            stringRedisTemplate.opsForValue().set(RedisKeyConstant.CATEGORY_TREE_CACHE,
                    objectMapper.writeValueAsString(tree), Duration.ofHours(1));
        } catch (Exception ignore) {
        }
        return tree;
    }

    private List<CategoryNode> buildTree(Long parentId) {
        List<PmsCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<PmsCategory>()
                .eq(PmsCategory::getShowFlag, 1)
                .orderByAsc(PmsCategory::getSort));
        List<CategoryNode> roots = all.stream()
                .filter(c -> parentId == null ? c.getParentId() == 0 : c.getParentId().equals(parentId))
                .map(this::toNode).collect(Collectors.toList());
        fillChildren(roots, all);
        return roots;
    }

    private void fillChildren(List<CategoryNode> nodes, List<PmsCategory> all) {
        Map<Long, List<PmsCategory>> byParent = all.stream()
                .collect(Collectors.groupingBy(PmsCategory::getParentId));
        for (CategoryNode node : nodes) {
            List<PmsCategory> children = byParent.getOrDefault(node.getId(), List.of());
            node.setChildren(children.stream().map(this::toNode).collect(Collectors.toList()));
            fillChildren(node.getChildren(), all);
        }
    }

    private CategoryNode toNode(PmsCategory c) {
        CategoryNode node = new CategoryNode();
        node.setId(c.getId());
        node.setParentId(c.getParentId());
        node.setName(c.getName());
        node.setSort(c.getSort());
        node.setShowFlag(c.getShowFlag());
        node.setCreateTime(c.getCreateTime());
        return node;
    }

    /**
     * 后台分类列表（平铺）
     */
    public List<PmsCategory> adminList() {
        return categoryMapper.selectList(new LambdaQueryWrapper<PmsCategory>().orderByAsc(PmsCategory::getSort));
    }

    public void create(PmsCategory category) {
        category.setId(null);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        categoryMapper.insert(category);
        evictCache();
    }

    public void update(PmsCategory category) {
        PmsCategory db = categoryMapper.selectById(category.getId());
        if (db == null) {
            throw new BusinessException("分类不存在");
        }
        categoryMapper.updateById(category);
        evictCache();
    }

    public void delete(Long id) {
        long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<PmsCategory>()
                .eq(PmsCategory::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在子分类，无法删除");
        }
        long productCount = productMapper.selectCount(new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException("分类下存在商品，无法删除");
        }
        categoryMapper.deleteById(id);
        evictCache();
    }

    private void evictCache() {
        stringRedisTemplate.delete(RedisKeyConstant.CATEGORY_TREE_CACHE);
    }
}
