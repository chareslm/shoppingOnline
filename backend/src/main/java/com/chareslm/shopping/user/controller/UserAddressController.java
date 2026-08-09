package com.chareslm.shopping.user.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import com.chareslm.shopping.user.dto.request.SaveUserAddressRequest;
import com.chareslm.shopping.user.dto.response.UserAddressResponse;
import com.chareslm.shopping.user.service.UserAddressService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/addresses")
public class UserAddressController {
    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) { this.userAddressService = userAddressService; }

    @GetMapping
    public ApiResponse<List<UserAddressResponse>> listAddresses() {
        return ApiResponse.success(userAddressService.listAddresses(CurrentUser.require().userId()));
    }

    @PostMapping
    public ApiResponse<UserAddressResponse> createAddress(@Valid @RequestBody SaveUserAddressRequest request) {
        return ApiResponse.success(userAddressService.createAddress(CurrentUser.require().userId(), request));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<UserAddressResponse> updateAddress(@PathVariable Long addressId, @Valid @RequestBody SaveUserAddressRequest request) {
        return ApiResponse.success(userAddressService.updateAddress(CurrentUser.require().userId(), addressId, request));
    }

    @PutMapping("/{addressId}/default")
    public ApiResponse<Void> setDefaultAddress(@PathVariable Long addressId) {
        userAddressService.setDefaultAddress(CurrentUser.require().userId(), addressId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long addressId) {
        userAddressService.deleteAddress(CurrentUser.require().userId(), addressId);
        return ApiResponse.success(null);
    }
}
