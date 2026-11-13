package com.ecomm.sb_ecomm.category.repository;

import com.ecomm.sb_ecomm.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category , Long> {

     Optional<Category> findByCategoryName(String categoryName);
}
