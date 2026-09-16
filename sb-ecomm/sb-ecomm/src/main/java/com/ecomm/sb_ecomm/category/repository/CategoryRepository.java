package com.ecomm.sb_ecomm.category.repository;

import com.ecomm.sb_ecomm.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category , Long> {

     Category findByCategoryName(String categoryName);
}
