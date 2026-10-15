package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
public class CategoryController {
    @Autowired
    public CategoryService categoryService;

    @GetMapping("/api/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories(
            @RequestParam(name = "pagenumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pagenumber,
            @RequestParam(name = "pagesize",defaultValue = AppConstants.PAGE_SIZE, required = false) Integer  pagesize,
            @RequestParam(name = "sortby" ,defaultValue = AppConstants.SORT_CATEGORIES_BY, required = false)String sortby,
            @RequestParam(name = "sortorder",defaultValue = AppConstants.SORT_DIR, required = false)String sortorder
    ) {
        CategoryResponse categoryResponse = categoryService.getAllCategories(pagenumber,pagesize,sortby,sortorder);
        return new ResponseEntity<>(categoryResponse,HttpStatus.OK);
    }

    @PostMapping("/api/admin/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO category) {
        CategoryDTO categoryDTO = categoryService.createCategory(category);
        return new ResponseEntity<>(categoryDTO,HttpStatus.OK);
    }
    @DeleteMapping("/api/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable long categoryId) {
        CategoryDTO status = categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @PutMapping("/api/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO category, @PathVariable long categoryId) {
        CategoryDTO updateCategory = categoryService.updateCategory(category,categoryId);
        return new ResponseEntity<>(updateCategory,HttpStatus.OK);
    }
}
