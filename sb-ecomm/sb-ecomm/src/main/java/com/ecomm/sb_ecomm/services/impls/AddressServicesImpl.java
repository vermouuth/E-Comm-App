package com.ecomm.sb_ecomm.services.impls;

import com.ecomm.sb_ecomm.config.AuthUtils;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.ecomm.sb_ecomm.models.Address;
import com.ecomm.sb_ecomm.models.Users;
import com.ecomm.sb_ecomm.payload.dto.AddressDto;
import com.ecomm.sb_ecomm.repositories.AddressRepository;
import com.ecomm.sb_ecomm.repositories.UserRepository;
import com.ecomm.sb_ecomm.services.AddressServices;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AddressServicesImpl implements AddressServices {


    private final AddressRepository addressRepository;
    private final AuthUtils authUtils;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    public AddressServicesImpl(AddressRepository addressRepository , UserRepository userRepository, AuthUtils authUtils , ModelMapper modelMapper) {
        this.addressRepository = addressRepository;
        this.authUtils = authUtils;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
    }

    @Override
    public AddressDto addAddress(AddressDto address) {

        Users loggedInUser = this.authUtils.loggedInUser();
        Address newAddress = modelMapper.map(address, Address.class);
        newAddress.setUsers(loggedInUser);
        newAddress = this.addressRepository.save(newAddress);
        return  modelMapper.map(newAddress,AddressDto.class);

    }

    @Override
    public List<AddressDto> getAddresses() {

        List<Address> addresses = addressRepository.findAll();

        return addresses.stream()
                .map(address -> modelMapper.map(address,AddressDto.class))
                .toList();
    }

    @Override
    public AddressDto getAddressById(Long addressId) {

        Address address = addressRepository.findById(addressId).
                orElseThrow(() -> new ResourceNotFoundException("Address","AddressId",addressId));
        return this.modelMapper.map(address,AddressDto.class);
    }

    @Override
    public List<AddressDto> getUserAddress() {
        Users currentUser = this.authUtils.loggedInUser();

        return currentUser.getAddresses().stream()
                .map(address -> modelMapper.map(address,AddressDto.class))
                .toList();
    }

    @Override
    public AddressDto updateAddress(AddressDto address, Long addressId) {
        Address address1 = this.addressRepository.findById(addressId).
                orElseThrow(() -> new ResourceNotFoundException("Address","AddressId",addressId));

        address1.setStreet(address.getStreet());
        address1.setCity(address.getCity());
        address1.setState(address.getState());
        address1.setZip(address.getZip());
        address1.setCountry(address.getCountry());
        address1 = this.addressRepository.save(address1);
        return this.modelMapper.map(address1,AddressDto.class);
    }

    @Override
    public AddressDto deleteAddress(Long addressId) {
        Address address = this.addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","AddressId",addressId));
        this.addressRepository.delete(address);
        return this.modelMapper.map(address,AddressDto.class);
    }

    @Override
    public List<AddressDto> getAddressesByUserId(Long userId) {
        Users user = this.userRepository.findById(userId).orElseThrow(()->
                new ResourceNotFoundException("User","UserId", userId));

        return user.getAddresses().stream().
        map(address-> this.modelMapper.map(address,AddressDto.class))
                .toList();
    }
}
