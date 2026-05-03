package com.ecomm.sb_ecomm.repositories;

import com.ecomm.sb_ecomm.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c from Cart c where c.uniqueUser.email = ?1")
    Cart findByUserEmail(String userEmail);

    @Query("select c from Cart c join fetch c.cartItems ci join fetch ci.product p where p.id =?1")
        List<Cart> findCartsByProductId(Long productId);


}
