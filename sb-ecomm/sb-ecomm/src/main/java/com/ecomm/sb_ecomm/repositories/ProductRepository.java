package com.ecomm.sb_ecomm.repositories;


import com.ecomm.sb_ecomm.models.Category;
import com.ecomm.sb_ecomm.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    Optional<Product> findByProductName(String productName);
    Page<Product> findByCategory(Category category, Pageable pageable);
    Page<Product> findByProductNameLikeIgnoreCase(String keyword ,  Pageable pageable);

}
