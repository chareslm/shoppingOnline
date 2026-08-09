package com.chareslm.shopping.user.service;

import com.chareslm.shopping.user.dto.request.SaveUserAddressRequest;
import com.chareslm.shopping.user.dto.response.UserAddressResponse;

import java.util.List;

public interface UserAddressService {
    List<UserAddressResponse> listAddresses(Long userId);

    UserAddressResponse createAddress(Long userId, SaveUserAddressRequest request);

    UserAddressResponse updateAddress(Long userId, Long addressId, SaveUserAddressRequest request);

    void setDefaultAddress(Long userId, Long addressId);

    void deleteAddress(Long userId, Long addressId);
}
