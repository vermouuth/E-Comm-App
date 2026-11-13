package com.ecomm.sb_ecomm.address.controllers;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

import com.ecomm.sb_ecomm.address.payload.AddressDto;
import com.ecomm.sb_ecomm.address.services.AddressServices;
import com.ecomm.sb_ecomm.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Addresses", description = "Manage shipping addresses. All endpoints require authentication.")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@Validated
@RestController
@RequestMapping("/api")
public class AddressController {

    private AddressServices addressServices;

    public AddressController(AddressServices addressServices )
    {
        this.addressServices = addressServices;
    }


    @Operation(summary = "Add an address for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created",
                    content = @Content(schema = @Schema(implementation = AddressDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/user/addresses")
    public ResponseEntity<?> addAddress(@Valid @RequestBody AddressDto address){
        return new ResponseEntity<>(this.addressServices.addAddress(address), HttpStatus.CREATED);
    }

    @Operation(summary = "List all addresses", description = "Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "All addresses",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressDto.class))))
    @GetMapping("/admin/addresses")
    public ResponseEntity<?> getAllAddresses(){
        return new ResponseEntity<>(this.addressServices.getAddresses(),HttpStatus.OK);
    }

    @Operation(summary = "Get an address by id", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address found",
                    content = @Content(schema = @Schema(implementation = AddressDto.class))),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("/admin/addresses/{addressId}")
    public ResponseEntity<?> getAddressById(@Parameter(description = "Address id") @Positive(message = "addressId must be a positive number") @PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.getAddressById(addressId),HttpStatus.OK);
    }


    @Operation(summary = "List the current user's addresses")
    @ApiResponse(responseCode = "200", description = "Current user's addresses",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressDto.class))))
    @GetMapping("/user/addresses")
    public ResponseEntity<?> getUserAddress(){
        return new ResponseEntity<>(this.addressServices.getUserAddress(),HttpStatus.OK);
    }

    @Operation(summary = "List addresses for a given user", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses for the user",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressDto.class)))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @GetMapping("/admin/addresses/user/{userId}")
    public ResponseEntity<?> getAddressesByUserId(@Parameter(description = "User id") @Positive(message = "userId must be a positive number") @PathVariable Long userId){
        return new ResponseEntity<>(this.addressServices.getAddressesByUserId(userId),HttpStatus.OK);
    }

    @Operation(summary = "Update an address", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated",
                    content = @Content(schema = @Schema(implementation = AddressDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })

    @PutMapping("/admin/addresses/{addressId}")
    public ResponseEntity<?> updateAddress(@Valid @RequestBody AddressDto address, @Parameter(description = "Address id") @Positive(message = "addressId must be a positive number") @PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.updateAddress(address,addressId),HttpStatus.OK);
    }

    @Operation(summary = "Set an address as the current user's default")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address set as default",
                    content = @Content(schema = @Schema(implementation = AddressDto.class))),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @PutMapping("/user/addresses/{addressId}")
    public ResponseEntity<?> setAddressAsDefault(@Parameter(description = "Address id") @Positive(message = "addressId must be a positive number") @PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.setAddressAsDefault(addressId),HttpStatus.OK);
    }

    @Operation(summary = "Update one of the current user's addresses",
            description = "Edits the address fields only; use PUT on the same path to make it the default.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated",
                    content = @Content(schema = @Schema(implementation = AddressDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Address not found (or not the current user's)",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @PatchMapping("/user/addresses/{addressId}")
    public ResponseEntity<?> updateUserAddress(@Valid @RequestBody AddressDto address, @Parameter(description = "Address id") @Positive(message = "addressId must be a positive number") @PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.updateUserAddress(address,addressId),HttpStatus.OK);
    }

    @Operation(summary = "Delete one of the current user's addresses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address deleted",
                    content = @Content(schema = @Schema(implementation = AddressDto.class))),
            @ApiResponse(responseCode = "404", description = "Address not found (or not the current user's)",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @DeleteMapping("/user/addresses/{addressId}")
    public ResponseEntity<?> deleteUserAddress(@Parameter(description = "Address id") @Positive(message = "addressId must be a positive number") @PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.deleteUserAddress(addressId),HttpStatus.OK);
    }

    @Operation(summary = "Delete an address", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address deleted",
                    content = @Content(schema = @Schema(implementation = AddressDto.class))),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = com.ecomm.sb_ecomm.common.responses.ApiResponse.class)))
    })
    @DeleteMapping("/admin/addresses/{addressId}")
    public ResponseEntity<?> deleteAddress(@Parameter(description = "Address id") @Positive(message = "addressId must be a positive number") @PathVariable Long addressId){
        return new ResponseEntity<>(this.addressServices.deleteAddress(addressId),HttpStatus.OK);
    }

}
