package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users" , uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)

public class Users extends BaseEntity {

    @Column(unique = true)
    @Size(min = 5 , max = 20)
    String username;

    @NotBlank
    @Size(min = 8, max = 150)
    String password;

    @NotBlank
    @Email
    @Column(name = "email")
    String email;

    public Users(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST,CascadeType.MERGE},fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn (name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "uniqueUser",
    cascade = {CascadeType.PERSIST,CascadeType.MERGE},
    fetch = FetchType.EAGER,
    orphanRemoval = true)
    private Set<Product> products = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST,CascadeType.MERGE},
    fetch = FetchType.EAGER)
    @JoinTable(name = "users_addresses" ,
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "address_id"))
    private Set<Address> addresses = new HashSet<>();

    @ToString.Exclude
    @OneToOne(mappedBy = "uniqueUser" , cascade = {CascadeType.PERSIST ,CascadeType.MERGE,CascadeType.REMOVE} , fetch = FetchType.EAGER , orphanRemoval = true)
    private Cart cart;



}
