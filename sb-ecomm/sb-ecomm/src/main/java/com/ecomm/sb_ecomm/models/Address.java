package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Size(min = 5, message = "Street must contain minimum 5 characters ")
    private String street;

    @Size(min = 4  ,message = "City must contain minimum 5 characters ")
    private String city;

    @Size(min = 2  ,message = "State must contain minimum 5 characters ")
    private String state;

    @Size(min = 6  ,message = "Zip must contain minimum 5 characters ")
    private String zip;

    @Size(min = 2  ,message = "Country must contain minimum 5 characters ")
    private String country;

    @ToString.Exclude
    @ManyToMany(mappedBy = "addresses")
    private Set<User> users = new HashSet<>();


}
