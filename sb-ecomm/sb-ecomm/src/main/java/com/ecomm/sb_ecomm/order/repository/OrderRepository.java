package com.ecomm.sb_ecomm.order.repository;

import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {

   List<Orders> getOrdersByUserId(Long userId);

    @Query("select o from Orders o left join fetch o.orderItems oi left join fetch oi.product " +
            "where o.idempotencyKey = :key and o.user.id = :userId")
    Optional<Orders> findByIdempotencyKeyAndUserIdFetchItems(@Param("key") String key,
                                                             @Param("userId") Long userId);

    @Query("""
       SELECT o 
       FROM Orders o 
       WHERE o.orderStatus = :orderStatus 
       AND o.expirationDate < :now
       """)
    List<Orders> getExpiredOrders(
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("now") LocalDateTime now
    );

    Optional<Orders> findByIdAndUserId(Long id, Long userId);
}
