package com.ecomm.sb_ecomm.services.impls;

import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.ecomm.sb_ecomm.models.Category;
import com.ecomm.sb_ecomm.payload.dto.CategoryDto;
import com.ecomm.sb_ecomm.payload.responses.CategoryResponse;
import com.ecomm.sb_ecomm.repositories.CategoryRepository;
import com.ecomm.sb_ecomm.services.CategoryService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final ModelMapper modelMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository ,  ModelMapper modelMapper) {
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
    }


    @Override
    public CategoryDto getCategoryById(Long categoryId) {
        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category","CategoryId", categoryId));
        return this.modelMapper.map(category, CategoryDto.class);
    }

    @Override
    public CategoryDto updateCategory(Long id , CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category","CategoryId" , categoryDto.getCategoryId()));
        category.setCategoryName(categoryDto.getCategoryName());
        category =  categoryRepository.save(category);
        return this.modelMapper.map(category, CategoryDto.class);
    }

    @Override
    public CategoryDto addCategory(CategoryDto categoryDto) {


        Category category = this.categoryRepository.findByCategoryName(categoryDto.getCategoryName());

        if(category != null){
            throw new ApiException("Category already exists");
        }

        category = this.modelMapper.map(categoryDto, Category.class);
        this.categoryRepository.save(category);
        return this.modelMapper.map(category, CategoryDto.class);
    }

    @Override
    public String deleteCategory(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category","CategoryId", id));
        categoryRepository.delete(category);
        return String.format("Category with id : %d has been deleted", id);
    }

    @Override
    public CategoryResponse getCategories(Integer pageNumber, Integer pageSize, String sortBy , String sortDirection) {

            Sort sortByAnyOrder = sortDirection.equalsIgnoreCase("asc")
                    ? Sort.by(Sort.Direction.ASC, sortBy)
                    : Sort.by(Sort.Direction.DESC, sortBy);

            Pageable pageDetails = PageRequest.of(pageNumber - 1, pageSize, sortByAnyOrder);
            Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
            List<Category> categoryList = categoryPage.getContent();

            if(categoryList.isEmpty()){
                throw new ApiException("No Categories found!");
            }

            List<CategoryDto> categoryDtoList = categoryList
                    .stream()
                    .map(category -> modelMapper.map(category,CategoryDto.class))
                    .toList();

            return new CategoryResponse(
                    categoryDtoList,
                    categoryPage.getNumber() + 1,
                    categoryPage.getSize(),
                    categoryPage.getTotalPages(),
                    categoryPage.getTotalElements(),
                    categoryPage.getNumber() ==  categoryList.size());

        }



}
