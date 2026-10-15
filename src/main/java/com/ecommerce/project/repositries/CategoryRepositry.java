package com.ecommerce.project.repositries;

import com.ecommerce.project.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepositry extends JpaRepository<Category, Long> {
    Category findByCategoryName(@NotBlank @Size(min = 5, message = "the name should be at least 5") String categoryName);
}
