package com.ecomm.sb_ecomm.payment.model;

import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.common.persistence.BaseEntity;
import com.ecomm.sb_ecomm.order.model.Orders;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Payment  extends BaseEntity {

    @Version
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private long version = 0L;

    @OneToOne(mappedBy = "payment")
    private Orders order;


    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(unique = true)
    private String pgPaymentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String pgStatus;
    private String pgResponseMessage;
    private String pgName;


    public Payment (String pgPaymentId , String pgStatus, String pgResponseMessage, String pgName, PaymentMethod paymentMethod)
    {
        this.pgPaymentId = pgPaymentId;
        this.pgStatus = pgStatus;
        this.pgResponseMessage = pgResponseMessage;
        this.pgName = pgName;
        this.paymentMethod = paymentMethod;

    }

    @ManyToOne
    private Users user;

}
