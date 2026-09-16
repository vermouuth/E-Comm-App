package com.ecomm.sb_ecomm.order.model;

import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.common.persistence.BaseEntity;
import com.ecomm.sb_ecomm.payment.model.Payment;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders"
        ,uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_orders_user_idempotency",
                columnNames = {
                        "user_id",
                        "idempotency_key"
                }
        )
}
)
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@AttributeOverride(name = "id"
        , column = @Column(name = "order_id")
)
public class Orders extends BaseEntity {

    @Email
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @OneToOne
    private Payment payment;

    @OneToMany(mappedBy = "orders" , cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    private List<OrderItem> orderItems;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private String idempotencyKey;

    @ManyToOne
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private Users user;

    private String street;
    private String state;
    private String city;
    private String zip;
    private String country;


}
