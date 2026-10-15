package com.ecommerce.project.service;

import com.ecommerce.project.exception.APIException;
import com.ecommerce.project.exception.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositries.CategoryRepositry;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
//    public List<Category> categories = new ArrayList<>();
//    private long nextId = 1l;
    @Autowired
    private CategoryRepositry categoryRepositry;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pagenumber, Integer pagesize,String sortby,String sortorder) {
        Sort sortByAndOrder = sortorder.equalsIgnoreCase("asd")?Sort.by(sortby).ascending():Sort.by(sortby).descending();
        Pageable pagedetails = PageRequest.of(pagenumber,pagesize,sortByAndOrder);
        Page<Category> categoryPage = categoryRepositry.findAll(pagedetails);
        List<Category>  categories = categoryPage.getContent();
        if(categories.size()==0){
            throw new APIException("No category created till now");
        }
        List<CategoryDTO> categoryDTOS = categories.stream().map(category -> modelMapper.map(category,CategoryDTO.class)).toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setLastPage(categoryPage.isLast());
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category savedCategory = categoryRepositry.findByCategoryName(category.getCategoryName());
        if(savedCategory != null) {
            throw new APIException("Category with this category "+category.getCategoryName()+" is already exists");
        }
        Category savedCategory2 = categoryRepositry.save(category);
        CategoryDTO savedCategoryDTO = modelMapper.map(savedCategory2,CategoryDTO.class);
        return savedCategoryDTO;
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Category category = categoryRepositry.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        categoryRepositry.delete(category);
        return modelMapper.map(category,CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
        Category savedCategory = categoryRepositry.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));
        Category category = modelMapper.map(categoryDTO, Category.class);

        category.setCategoryId(categoryId);
        savedCategory = categoryRepositry.save(category);
        CategoryDTO savedCategoryDTO = modelMapper.map(savedCategory,CategoryDTO.class);
        return savedCategoryDTO;
    }
}
