package com.ecomm.sb_ecomm.payment.repository;

import com.ecomm.sb_ecomm.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPgPaymentId(String pgPaymentId);

}
