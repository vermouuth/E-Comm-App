package com.ecomm.sb_ecomm.repositories;

import com.ecomm.sb_ecomm.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c from Cart c where c.user.email = ?1")
    Cart findByUserEmail(String userEmail);


}
