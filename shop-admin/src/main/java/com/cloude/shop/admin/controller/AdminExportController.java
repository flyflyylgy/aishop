package com.cloude.shop.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloude.shop.admin.util.ExcelExporter;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.constant.PermissionCode;
import com.cloude.shop.mapper.entity.PmsProduct;
import com.cloude.shop.mapper.entity.SmsCoupon;
import com.cloude.shop.service.dto.OrderDetailVO;
import com.cloude.shop.service.dto.ProductQuery;
import com.cloude.shop.service.service.CouponService;
import com.cloude.shop.service.service.MemberService;
import com.cloude.shop.service.service.OrderService;
import com.cloude.shop.service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 后台数据导出（Excel）—— 订单/商品/会员/优惠券
 * 复用列表查询条件，导出全部匹配记录（同步导出，万级以内）。
 */
@Slf4j
@Tag(name = "AdminExportController", description = "数据导出")
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class AdminExportController {

    private static final int MAX_EXPORT = 100_000;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderService orderService;
    private final ProductService productService;
    private final MemberService memberService;
    private final CouponService couponService;

    @Operation(summary = "导出订单")
    @RequirePermission(PermissionCode.ORDER_LIST)
    @GetMapping("/order")
    public void order(@RequestParam(required = false) Integer status,
                      @RequestParam(required = false) String keyword,
                      HttpServletResponse response) throws Exception {
        Page<OrderDetailVO> page = orderService.adminPage(status, keyword, 1, MAX_EXPORT);
        String[] headers = {"订单号", "会员ID", "状态", "商品总额", "优惠金额", "应付金额", "优惠券",
                "收货人", "手机号", "收货地址", "物流公司", "运单号", "下单时间", "支付时间", "备注"};
        List<Object[]> rows = new ArrayList<>();
        for (OrderDetailVO o : page.getRecords()) {
            rows.add(new Object[]{
                    o.getOrderNo(), o.getMemberId(), orderStatusText(o.getStatus()),
                    o.getTotalAmount(), nz(o.getCouponAmount()), o.getPayAmount(), nz(o.getCouponName()),
                    o.getReceiverName(), o.getReceiverPhone(), o.getReceiverAddr(),
                    nz(o.getExpressCompany()), nz(o.getExpressNo()),
                    fmt(o.getCreateTime()), fmt(o.getPayTime()), nz(o.getNote())
            });
        }
        ExcelExporter.write(response, "订单", headers, rows, "订单报表_" + ts());
    }

    @Operation(summary = "导出商品")
    @RequirePermission(PermissionCode.PRODUCT_LIST)
    @GetMapping("/product")
    public void product(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Integer status,
                        @RequestParam(required = false) Long categoryId,
                        HttpServletResponse response) throws Exception {
        ProductQuery query = new ProductQuery();
        query.setKeyword(keyword);
        query.setStatus(status);
        query.setCategoryId(categoryId);
        query.setPageNum(1);
        query.setPageSize(MAX_EXPORT);
        Page<PmsProduct> page = productService.adminPage(query);
        String[] headers = {"商品ID", "商品名称", "分类ID", "价格", "原价", "可售库存", "锁定", "已售",
                "多规格", "状态", "创建时间"};
        List<Object[]> rows = new ArrayList<>();
        for (PmsProduct p : page.getRecords()) {
            rows.add(new Object[]{
                    p.getId(), p.getName(), p.getCategoryId(), p.getPrice(), p.getOriginalPrice(),
                    p.getAvailableStock(), p.getLockedStock(), p.getSoldStock(),
                    Integer.valueOf(1).equals(p.getHasSku()) ? "是" : "否",
                    Integer.valueOf(1).equals(p.getStatus()) ? "上架" : "下架",
                    fmt(p.getCreateTime())
            });
        }
        ExcelExporter.write(response, "商品", headers, rows, "商品报表_" + ts());
    }

    @Operation(summary = "导出会员")
    @RequirePermission(PermissionCode.MEMBER_LIST)
    @GetMapping("/member")
    public void member(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws Exception {
        Page<Map<String, Object>> page = memberService.adminPage(keyword, status, 1, MAX_EXPORT);
        String[] headers = {"会员ID", "用户名", "昵称", "手机号", "状态", "订单数", "累计消费", "注册时间"};
        List<Object[]> rows = new ArrayList<>();
        for (Map<String, Object> m : page.getRecords()) {
            rows.add(new Object[]{
                    m.get("id"), m.get("username"), m.get("nickname"), m.get("phone"),
                    Integer.valueOf(1).equals(m.get("status")) ? "正常" : "封禁",
                    m.get("orderCount"), m.get("totalAmount"), fmt(m.get("createTime"))
            });
        }
        ExcelExporter.write(response, "会员", headers, rows, "会员报表_" + ts());
    }

    @Operation(summary = "导出优惠券")
    @RequirePermission(PermissionCode.COUPON_LIST)
    @GetMapping("/coupon")
    public void coupon(@RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Integer type,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws Exception {
        Page<SmsCoupon> page = couponService.adminPage(status, type, keyword, 1, MAX_EXPORT);
        String[] headers = {"ID", "券名称", "类型", "面额/折扣", "门槛", "发行总量", "已领取", "已使用",
                "每人限领", "领取开始", "领取结束", "领后有效天数", "状态"};
        List<Object[]> rows = new ArrayList<>();
        for (SmsCoupon c : page.getRecords()) {
            String rule = c.getType() != null && c.getType() == 2
                    ? ((c.getDiscount() == null ? "" : c.getDiscount()) + "折(如85=8.5折)")
                    : ("¥" + nz(c.getFaceValue()));
            rows.add(new Object[]{
                    c.getId(), c.getName(), couponTypeText(c.getType()), rule, c.getMinPoint(),
                    c.getTotalCount(), c.getReceivedCount(), c.getUsedCount(), c.getPerLimit(),
                    fmt(c.getStartTime()), fmt(c.getEndTime()), c.getValidDays(),
                    Integer.valueOf(1).equals(c.getStatus()) ? "上架" : "下架"
            });
        }
        ExcelExporter.write(response, "优惠券", headers, rows, "优惠券报表_" + ts());
    }

    private static String orderStatusText(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "待付款";
            case 1 -> "已付款";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已关闭";
            default -> String.valueOf(s);
        };
    }

    private static String couponTypeText(Integer t) {
        if (t == null) return "";
        return switch (t) {
            case 1 -> "满减券";
            case 2 -> "折扣券";
            case 3 -> "现金券";
            default -> String.valueOf(t);
        };
    }

    private static String fmt(Object o) {
        if (o == null) return "";
        if (o instanceof LocalDateTime ldt) return ldt.format(DT);
        return String.valueOf(o);
    }

    private static String nz(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String ts() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}
