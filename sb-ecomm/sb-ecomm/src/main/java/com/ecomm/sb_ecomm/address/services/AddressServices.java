package com.ecomm.sb_ecomm.address.services;

import com.ecomm.sb_ecomm.address.payload.AddressDto;

import java.util.List;

public interface AddressServices {

    AddressDto addAddress(AddressDto address);

    List<AddressDto> getAddresses();

    AddressDto getAddressById(Long addressId);

    List<AddressDto> getUserAddress();

    AddressDto updateAddress(AddressDto address, Long addressId);

    AddressDto deleteAddress(Long addressId);

    List<AddressDto> getAddressesByUserId(Long userId);

    AddressDto setAddressAsDefault(Long addressId);

    AddressDto getDefaultAddress (Long userId);

}
