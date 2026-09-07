package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloude.shop.mapper.entity.OmsOrder;
import com.cloude.shop.mapper.mapper.OmsOrderMapper;
import com.cloude.shop.mapper.mapper.PmsProductMapper;
import com.cloude.shop.mapper.mapper.UmsMemberMapper;
import com.cloude.shop.mapper.entity.PmsProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台 Dashboard 聚合统计服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OmsOrderMapper orderMapper;
    private final PmsProductMapper productMapper;
    private final UmsMemberMapper memberMapper;

    /**
     * 聚合首页运营指标，单次请求返回全部数据
     */
    public Map<String, Object> dashboard() {
        Map<String, Object> stats = new HashMap<>();

        // 商品统计
        stats.put("productTotal", productMapper.selectCount(null));
        stats.put("productOnSale", productMapper.selectCount(new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getStatus, 1)));
        stats.put("productOffSale", productMapper.selectCount(new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getStatus, 0)));

        // 订单状态分布
        stats.put("orderTotal", orderMapper.selectCount(null));
        stats.put("orderPending", orderMapper.selectCount(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, 0)));
        stats.put("orderToShip", orderMapper.selectCount(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, 1)));
        stats.put("orderShipped", orderMapper.selectCount(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, 2)));
        stats.put("orderDone", orderMapper.selectCount(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, 3)));
        stats.put("orderClosed", orderMapper.selectCount(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, 4)));

        // 会员统计
        stats.put("memberTotal", memberMapper.selectCount(null));

        // 销售额
        stats.put("totalSales", orderMapper.sumPaidAmount());
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.put("todaySales", orderMapper.sumPaidAmountSince(todayStart));
        stats.put("todayOrders", orderMapper.selectCount(new LambdaQueryWrapper<OmsOrder>()
                .ge(OmsOrder::getCreateTime, todayStart)));

        // 最近 5 笔订单
        List<OmsOrder> latest = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                .orderByDesc(OmsOrder::getCreateTime).last("LIMIT 5"));
        stats.put("latestOrders", latest);

        return stats;
    }
}
