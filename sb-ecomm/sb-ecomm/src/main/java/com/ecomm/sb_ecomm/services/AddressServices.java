package com.ecomm.sb_ecomm.services;

import com.ecomm.sb_ecomm.models.Users;
import com.ecomm.sb_ecomm.payload.dto.AddressDto;

import java.util.List;

public interface AddressServices {

    AddressDto addAddress(AddressDto address);

    List<AddressDto> getAddresses();

    AddressDto getAddressById(Long addressId);

    List<AddressDto> getUserAddress();

    AddressDto updateAddress(AddressDto address, Long addressId);

    AddressDto deleteAddress(Long addressId);

    List<AddressDto> getAddressesByUserId(Long userId);
}
