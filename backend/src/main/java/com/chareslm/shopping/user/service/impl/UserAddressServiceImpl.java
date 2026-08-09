package com.chareslm.shopping.user.service.impl;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.user.dto.request.SaveUserAddressRequest;
import com.chareslm.shopping.user.dto.response.UserAddressResponse;
import com.chareslm.shopping.user.entity.UserAddress;
import com.chareslm.shopping.user.mapper.UserAddressMapper;
import com.chareslm.shopping.user.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAddressServiceImpl implements UserAddressService {
    private final UserAddressMapper userAddressMapper;

    public UserAddressServiceImpl(UserAddressMapper userAddressMapper) { this.userAddressMapper = userAddressMapper; }

    @Override
    public List<UserAddressResponse> listAddresses(Long userId) {
        return userAddressMapper.selectByUserId(userId).stream().map(UserAddressServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public UserAddressResponse createAddress(Long userId, SaveUserAddressRequest request) {
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || userAddressMapper.countByUserId(userId) == 0;
        if (makeDefault) userAddressMapper.clearDefaultByUserId(userId);
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        apply(address, request);
        address.setIsDefault(makeDefault);
        userAddressMapper.insert(address);
        return toResponse(address);
    }

    @Override
    @Transactional
    public UserAddressResponse updateAddress(Long userId, Long addressId, SaveUserAddressRequest request) {
        UserAddress address = requireOwnedAddress(userId, addressId);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) userAddressMapper.clearDefaultByUserId(userId);
        apply(address, request);
        address.setIsDefault(makeDefault || Boolean.TRUE.equals(address.getIsDefault()));
        userAddressMapper.updateById(address);
        return toResponse(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        requireOwnedAddress(userId, addressId);
        userAddressMapper.clearDefaultByUserId(userId);
        userAddressMapper.markDefault(addressId, userId);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = requireOwnedAddress(userId, addressId);
        userAddressMapper.deleteById(addressId);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            userAddressMapper.selectByUserId(userId).stream().findFirst().ifPresent(next -> userAddressMapper.markDefault(next.getId(), userId));
        }
    }

    private UserAddress requireOwnedAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectByIdAndUserId(addressId, userId);
        if (address == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return address;
    }

    private static void apply(UserAddress address, SaveUserAddressRequest request) {
        address.setRecipientName(request.recipientName().trim());
        address.setRecipientPhone(request.recipientPhone().trim());
        address.setProvinceCode(trimToNull(request.provinceCode()));
        address.setProvinceName(request.provinceName().trim());
        address.setCityCode(trimToNull(request.cityCode()));
        address.setCityName(request.cityName().trim());
        address.setDistrictCode(trimToNull(request.districtCode()));
        address.setDistrictName(request.districtName().trim());
        address.setDetailAddress(request.detailAddress().trim());
        address.setPostalCode(trimToNull(request.postalCode()));
    }

    private static UserAddressResponse toResponse(UserAddress address) {
        return new UserAddressResponse(address.getId(), address.getRecipientName(), address.getRecipientPhone(), address.getProvinceCode(), address.getProvinceName(), address.getCityCode(), address.getCityName(), address.getDistrictCode(), address.getDistrictName(), address.getDetailAddress(), address.getPostalCode(), Boolean.TRUE.equals(address.getIsDefault()));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
