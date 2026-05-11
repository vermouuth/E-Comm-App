package com.ecomm.sb_ecomm.controllers;

import com.ecomm.sb_ecomm.models.Address;
import com.ecomm.sb_ecomm.models.Users;
import com.ecomm.sb_ecomm.payload.dto.AddressDto;
import com.ecomm.sb_ecomm.services.AddressServices;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AddressController {

    private AddressServices addressServices;

    public AddressController(AddressServices addressServices )
    {
        this.addressServices = addressServices;
    }


    @PostMapping("/user/addresses")
    public ResponseEntity<?> addAddress(@Valid @RequestBody AddressDto address){
        return new ResponseEntity<>(this.addressServices.addAddress(address), HttpStatus.CREATED);
    }

    @GetMapping("/admin/addresses")
    public ResponseEntity<?> getAllAddresses(){
        return new ResponseEntity<>(this.addressServices.getAddresses(),HttpStatus.OK);
    }

    @GetMapping("/admin/addresses/{addressId}")
    public ResponseEntity<?> getAddressById(@PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.getAddressById(addressId),HttpStatus.OK);
    }


    @GetMapping("/user/addresses")
    public ResponseEntity<?> getUserAddress(){
        return new ResponseEntity<>(this.addressServices.getUserAddress(),HttpStatus.OK);
    }

    @GetMapping("/admin/addresses/{userId}")
    public ResponseEntity<?> getAddressesByUserId(@PathVariable Long userId){
        return new ResponseEntity<>(this.addressServices.getAddressesByUserId(userId),HttpStatus.OK);
    }

    @PutMapping("/admin/addresses/{addressId}")
    public ResponseEntity<?> updateAddress(@Valid @RequestBody AddressDto address, @PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.updateAddress(address,addressId),HttpStatus.OK);
    }

    @DeleteMapping("/admin/addresses/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.deleteAddress(addressId),HttpStatus.OK);
    }


}
