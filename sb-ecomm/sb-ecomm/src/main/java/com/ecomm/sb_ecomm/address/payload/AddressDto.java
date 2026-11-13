package com.ecomm.sb_ecomm.address.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {

    private Long id;
    @NotBlank(message = "street is required")
    @Size(min = 5, max = 100, message = "street must be 5-100 characters")
    private String street;
    @NotBlank(message = "city is required")
    @Size(min = 4, max = 60, message = "city must be 4-60 characters")
    private String city;
    @NotBlank(message = "state is required")
    @Size(min = 2, max = 60, message = "state must be 2-60 characters")
    private String state;
    @NotBlank(message = "zip is required")
    @Size(min = 6, max = 12, message = "zip must be 6-12 characters")
    private String zip;
    @NotBlank(message = "country is required")
    @Size(min = 2, max = 60, message = "country must be 2-60 characters")
    private String country;
    private Boolean isDefault;

}
