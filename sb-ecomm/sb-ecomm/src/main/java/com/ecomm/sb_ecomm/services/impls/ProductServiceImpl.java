package com.ecomm.sb_ecomm.services.impls;

import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.ecomm.sb_ecomm.models.Cart;
import com.ecomm.sb_ecomm.models.Category;
import com.ecomm.sb_ecomm.models.Product;
import com.ecomm.sb_ecomm.payload.dto.ProductDto;
import com.ecomm.sb_ecomm.payload.responses.ProductResponse;
import com.ecomm.sb_ecomm.repositories.CartRepository;
import com.ecomm.sb_ecomm.repositories.CategoryRepository;
import com.ecomm.sb_ecomm.repositories.ProductRepository;
import com.ecomm.sb_ecomm.services.CartServices;
import com.ecomm.sb_ecomm.services.FileService;
import com.ecomm.sb_ecomm.services.ProductService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {


    private final ProductRepository productRepository;

    private final CartRepository cartRepository;

    private final ModelMapper modelMapper;

    private final CategoryRepository categoryRepository;

    private final FileService fileService;

    private  final CartServices cartServices;

    private Double recalculateSpecialPrice(double price , double specialOffer) {
        return price - (price * (specialOffer / 100)) ;
    }

    @Value("${project.images}")
    private String path;

    public ProductServiceImpl(ProductRepository productRepository ,CategoryRepository categoryRepository , CartServices cartServices ,
                              ModelMapper modelMapper , FileService fileService , CartRepository cartRepository ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
        this.fileService = fileService;
        this.cartRepository = cartRepository;
        this.cartServices = cartServices;
    }

    @Override
    public ProductDto createProduct(Long categoryId,ProductDto productDto) {

        Category categoryFromDb = this.categoryRepository.findById(categoryId).
                orElseThrow(()-> new ResourceNotFoundException("Category" , "CategoryId",categoryId));

        Optional<Product> product = this.productRepository.findByProductName(productDto.getProductName());

        if(product.isPresent()){
            throw new ApiException(String.format("Product with name %s is already exists", productDto.getProductName()));
        }

        Product productEntity = this.modelMapper.map(productDto, Product.class);
        productEntity.setCategory(categoryFromDb);
        productEntity.setImage("default.png");
        productEntity.setSpecialPrice(recalculateSpecialPrice(productEntity.getPrice(), productEntity.getDiscount()));


        productEntity = this.productRepository.save(productEntity);
        return this.modelMapper.map(productEntity, ProductDto.class);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageSize, Integer pageNumber, String sortBy, String sortOrder) {
        Sort sortByAnyOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(Sort.Direction.ASC, sortBy)
                : Sort.by(Sort.Direction.DESC, sortBy);

        Pageable pageDetails = PageRequest.of(pageNumber - 1, pageSize, sortByAnyOrder);
        Page<Product> productPage = this.productRepository.findAll(pageDetails);
        List<Product> productList = productPage.getContent();
        List<ProductDto> productsFromDB  = productList
                .stream()
                .map((product -> this.modelMapper.map(product,ProductDto.class)))
                .toList();

        return new ProductResponse(
                productsFromDB,
                productPage.getNumber() + 1,
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements(),
                productPage.isLast()
        );
    }

    @Override
    public ProductDto deleteProduct(Long productId) {
        Product product = this.productRepository.findById(productId).orElseThrow(()-> new ApiException("Product with id " + productId + " does not exist"));

        List<Cart> carts = this.cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> this.cartServices.deleteProductFromCart(cart.getCartId(), productId));

        this.productRepository.delete(product);

        return this.modelMapper.map(product, ProductDto.class);
    }

    @Override
    public ProductDto updateProduct(Long productId, ProductDto productDto) {

        Product productFromDatabase = this.productRepository.findById(productId).orElseThrow(()-> new ApiException("Product with id " + productId + " does not exist"));

        productFromDatabase.setProductName(productDto.getProductName());
        productFromDatabase.setDescription(productDto.getDescription());
        productFromDatabase.setQuantity(productDto.getQuantity());
        productFromDatabase.setPrice(productDto.getPrice());
        productFromDatabase.setDiscount(productDto.getDiscount());
        productFromDatabase.setSpecialPrice(this.recalculateSpecialPrice(productFromDatabase.getPrice(),productFromDatabase.getDiscount()));

        productFromDatabase =  this.productRepository.save(productFromDatabase);

        List<Cart> carts = this.cartRepository.findCartsByProductId(productId);


        carts.forEach((cart -> {this.cartServices.updateProductInCarts(cart.getCartId() , productId);}));

        return this.modelMapper.map(productFromDatabase, ProductDto.class);

    }

    @Override
    public ProductResponse getProductsByCategory(Long categoryId, Integer pageSize, Integer pageNumber, String sortBy, String sortOrder) {

        Category categoryFromDatabase = this.categoryRepository
                .findById(categoryId)
                .orElseThrow(()-> new ApiException("Category with id " + categoryId + " does not exist"));

        Sort sortByAnyOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(Sort.Direction.ASC,sortBy)
                : Sort.by(Sort.Direction.DESC,sortBy);

        Pageable pageDetails = PageRequest.of(pageNumber - 1, pageSize, sortByAnyOrder);
        Page<Product> productPage = this.productRepository.findByCategory(categoryFromDatabase,pageDetails);
        List<Product> productList = productPage.getContent();

        if(productList.isEmpty()){
            throw new ApiException("No Products exists with given categoryId : " +  categoryId);
        }

        List<ProductDto> productsDto = productList.stream().map((product -> this.modelMapper.map(product,ProductDto.class))).toList();

        return new ProductResponse(
                productsDto,
                productPage.getNumber() + 1,
                productPage.getSize(),
                productPage.getTotalPages(),
                productPage.getTotalElements(),
                productPage.isLast()
        );

    }

    public ProductResponse getProductsByKeyword(String keyword, Integer pageSize, Integer pageNumber, String sortBy, String sortOrder) {

        Sort sortByAnyOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(Sort.Direction.ASC,sortBy)
                : Sort.by(Sort.Direction.DESC,sortBy);

        Pageable pageDetails = PageRequest.of(pageNumber - 1, pageSize, sortByAnyOrder);
        Page<Product> pageOfProducts = this.productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%',pageDetails);
        List<Product> productList = pageOfProducts.getContent();

        if(productList.isEmpty()){
            throw new ApiException("No Products exists with given keyword : " +  keyword);
        }

        List<ProductDto> productDtoList = productList.stream().map((product -> this.modelMapper.map(product,ProductDto.class))).toList();
        return new ProductResponse(
                productDtoList,
                pageOfProducts.getNumber() + 1,
                pageOfProducts.getSize(),
                pageOfProducts.getTotalPages(),
                pageOfProducts.getTotalElements(),
                pageOfProducts.isLast()
        );
    }

    @Override
    public Long countAllProducts() {
        long countProducts = this.productRepository.count();

        if(countProducts <= 0){
            throw new ApiException("No Products exists yet!");
        }
        return countProducts;
    }

    @Override
    public ProductDto updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDatabase = this.productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product" , "ProductId", productId));

        String newFileName = this.fileService.uploadImage(this.path,image);
        productFromDatabase.setImage(newFileName);
        productFromDatabase = this.productRepository.save(productFromDatabase);

        return  this.modelMapper.map(productFromDatabase, ProductDto.class);
    }
}
