package com.ecomm.sb_ecomm.product.model;

import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.cart.model.CartItem;
import com.ecomm.sb_ecomm.category.model.Category;
import com.ecomm.sb_ecomm.common.persistence.BaseEntity;
import com.ecomm.sb_ecomm.order.model.OrderItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Product  extends BaseEntity {


    @NotBlank
    @Size(min = 3 , max = 160 , message = "Product name must contain at least 3 letters")
    private String productName;

    @NotBlank
    @Size(min = 3 , max = 400 , message = "Product description must contain at least 3 letters")
    private String description;

    private String image;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true , message = "Discount must not be negative.")
    @DecimalMax(value = "100.0" , inclusive = true  , message = "Discount must not be more than 100%")
    private Double discount;

    @NotNull
    @DecimalMin(value = "0.0" , inclusive = false , message = "Price must not be negative")
    private Double price;


    private Double specialPrice;

    @NotNull
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(nullable = false)
    private Integer stockQuantity;

    @NotNull
    @Min(value = 0)
    @Column(nullable = false)
    private Integer reservedQuantity;



    @ManyToOne()
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne()
    @JoinColumn(name = "user_id" )
    private Users uniqueUser;

    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "product" , cascade = {CascadeType.PERSIST , CascadeType.MERGE} , fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();

}
