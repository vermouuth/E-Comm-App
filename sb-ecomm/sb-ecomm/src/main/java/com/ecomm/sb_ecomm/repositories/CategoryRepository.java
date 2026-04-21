package com.ecomm.sb_ecomm.repositories;

import com.ecomm.sb_ecomm.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category , Long> {

     Category findByCategoryName(String categoryName);
}
