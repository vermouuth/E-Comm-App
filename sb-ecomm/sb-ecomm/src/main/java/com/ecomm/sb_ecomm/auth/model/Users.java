package com.ecomm.sb_ecomm.auth.model;

import com.ecomm.sb_ecomm.address.model.Address;
import com.ecomm.sb_ecomm.cart.model.Cart;
import com.ecomm.sb_ecomm.common.persistence.BaseEntity;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.payment.model.Payment;
import com.ecomm.sb_ecomm.product.model.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
@AttributeOverride(name = "id"
        , column = @Column(name = "user_id")
)
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

    @OneToMany(mappedBy = "users",cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    private List<Address> addresses;

    @ToString.Exclude
    @OneToOne(mappedBy = "uniqueUser" , cascade = {CascadeType.PERSIST ,CascadeType.MERGE,CascadeType.REMOVE} , fetch = FetchType.EAGER , orphanRemoval = true)
    private Cart cart;

    @OneToMany(mappedBy = "user")
    private List<Orders> orders = new LinkedList<>();

    @OneToMany(mappedBy = "user")
    private List<Payment> payments = new LinkedList<>();



}
