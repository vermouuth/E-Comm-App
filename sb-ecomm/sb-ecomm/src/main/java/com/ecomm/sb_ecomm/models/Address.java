package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "addresses")
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Address extends BaseEntity {

    @NotBlank
    @Size(min = 5, message = "Street must contain minimum 5 characters ")
    private String street;

    @NotBlank
    @Size(min = 4  ,message = "City must contain minimum 5 characters ")
    private String city;

    @NotBlank
    @Size(min = 2  ,message = "State must contain minimum 5 characters ")
    private String state;

    @NotBlank
    @Size(min = 6  ,message = "Zip must contain minimum 5 characters ")
    private String zip;

    @NotBlank
    @Size(min = 2  ,message = "Country must contain minimum 5 characters ")
    private String country;


    public Address() {

    }

    public Address(String street,String city,String state,String zip,String country ){
        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.country = country;
    }


    @ManyToOne()
    private Users users;



}
