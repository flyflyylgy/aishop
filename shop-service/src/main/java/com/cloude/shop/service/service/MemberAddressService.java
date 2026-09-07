package com.cloude.shop.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cloude.shop.common.exception.BusinessException;
import com.cloude.shop.mapper.entity.UmsMemberAddress;
import com.cloude.shop.mapper.mapper.UmsMemberAddressMapper;
import com.cloude.shop.service.dto.AddressParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会员收货地址服务（每人上限 20 条，唯一默认地址）
 */
@Service
@RequiredArgsConstructor
public class MemberAddressService {

    /** 每个会员最多保存的地址数量 */
    private static final long MAX_ADDRESS_COUNT = 20;

    private final UmsMemberAddressMapper addressMapper;

    public List<UmsMemberAddress> list(Long memberId) {
        // 默认地址排最前，其余按更新时间倒序
        return addressMapper.selectList(new LambdaQueryWrapper<UmsMemberAddress>()
                .eq(UmsMemberAddress::getMemberId, memberId)
                .orderByDesc(UmsMemberAddress::getIsDefault)
                .orderByDesc(UmsMemberAddress::getUpdateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long add(Long memberId, AddressParam param) {
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<UmsMemberAddress>()
                .eq(UmsMemberAddress::getMemberId, memberId));
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException("收货地址最多保存 " + MAX_ADDRESS_COUNT + " 条");
        }
        boolean makeDefault = Boolean.TRUE.equals(param.getIsDefault()) || count == 0;
        if (makeDefault) {
            clearDefault(memberId);
        }
        UmsMemberAddress address = new UmsMemberAddress();
        address.setMemberId(memberId);
        address.setReceiverName(param.getReceiverName());
        address.setReceiverPhone(param.getReceiverPhone());
        address.setReceiverAddr(param.getReceiverAddr());
        address.setIsDefault(makeDefault ? 1 : 0);
        addressMapper.insert(address);
        return address.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long memberId, Long addressId, AddressParam param) {
        UmsMemberAddress address = getOwned(memberId, addressId);
        if (Boolean.TRUE.equals(param.getIsDefault())) {
            clearDefault(memberId);
            address.setIsDefault(1);
        }
        address.setReceiverName(param.getReceiverName());
        address.setReceiverPhone(param.getReceiverPhone());
        address.setReceiverAddr(param.getReceiverAddr());
        addressMapper.updateById(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long addressId) {
        UmsMemberAddress address = getOwned(memberId, addressId);
        addressMapper.deleteById(addressId);
        // 删除默认地址后自动提升最近更新的一条为默认
        if (address.getIsDefault() == 1) {
            UmsMemberAddress latest = addressMapper.selectOne(new LambdaQueryWrapper<UmsMemberAddress>()
                    .eq(UmsMemberAddress::getMemberId, memberId)
                    .orderByDesc(UmsMemberAddress::getUpdateTime)
                    .orderByDesc(UmsMemberAddress::getId)
                    .last("LIMIT 1"));
            if (latest != null) {
                latest.setIsDefault(1);
                addressMapper.updateById(latest);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long memberId, Long addressId) {
        UmsMemberAddress address = getOwned(memberId, addressId);
        clearDefault(memberId);
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    /**
     * 校验归属并返回（下单时填充收货人使用）
     */
    public UmsMemberAddress getOwned(Long memberId, Long addressId) {
        UmsMemberAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getMemberId().equals(memberId)) {
            throw new BusinessException("收货地址不存在");
        }
        return address;
    }

    private void clearDefault(Long memberId) {
        addressMapper.update(null, new LambdaUpdateWrapper<UmsMemberAddress>()
                .eq(UmsMemberAddress::getMemberId, memberId)
                .eq(UmsMemberAddress::getIsDefault, 1)
                .set(UmsMemberAddress::getIsDefault, 0));
    }
}
