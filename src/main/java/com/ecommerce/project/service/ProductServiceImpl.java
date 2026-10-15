package com.ecommerce.project.service;

import com.ecommerce.project.exception.APIException;
import com.ecommerce.project.exception.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositries.CartRepository;
import com.ecommerce.project.repositries.CategoryRepositry;
import com.ecommerce.project.repositries.ProductRepositry;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class                                                                                                                            ProductServiceImpl implements ProductService {
    @Autowired
    private CategoryRepositry  categoryRepositry;

    @Autowired
    private ProductRepositry productRepositry;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.images}")
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private AuthUtil authUtil;


    @Override
    public ProductDTO addProduct(ProductDTO productDTO, Long categoryId) {
        Category category = categoryRepositry.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        boolean isProductNotPresent = true;
        List<Product> products = category.getProduct();
        for(Product value : products){
            if(value.getProductName().equals(productDTO.getProductName())){
                isProductNotPresent = false;
                break;
            }
        }
        if(isProductNotPresent){
            Product product = modelMapper.map(productDTO,Product.class);
            product.setCategory(category);
            product.setImage("Default png");
            product.setUser(authUtil.loggerInUser());
            double specialPrice =product.getPrice() - (product.getDiscount()*0.01) * product.getPrice();
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepositry.save(product);
            return modelMapper.map(savedProduct,ProductDTO.class);
        }else{
            throw new APIException("Product is already exists");
        }
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Specification<Product> spec = Specification.unrestricted();
        if(keyword != null && !keyword.isEmpty()){
            spec = spec.and((root,query,criteriaBuilder) ->
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%")
                    );
        }
        if(category != null && !category.isEmpty()){
            spec = spec.and((root,query,criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category)
            );
        }

        Page<Product> productPage = productRepositry.findAll(spec,pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream().map( product -> {
            ProductDTO productDTO = modelMapper.map(product,ProductDTO.class);
            productDTO.setImage(constructImageUrl(product.getImage()));
            return productDTO;
        }).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;

    }
    private String constructImageUrl(String imageName){
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName :imageBaseUrl+ "/"+ imageName;
    }

    @Override
    public ProductResponse getProductByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepositry.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepositry.findByCategoryOrderByPriceAsc(category,pageDetails);
        List<Product> products = productPage.getContent();
        if(products.isEmpty()){
            throw new APIException("product not found with category "+categoryId);
        }
        //List<Product> productsInCategory = productRepositry.findByCategoryOrderByPriceAsc(category);
        List<ProductDTO> productDTOS = products.stream().map( product -> modelMapper.map(product,ProductDTO.class)).toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse getProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepositry.findByProductNameLikeIgnoreCase('%'+keyword+'%',pageDetails);
        List<Product> products = productPage.getContent();
        if(products.isEmpty()){
            throw new APIException("product not found with keyword "+keyword);
        }
       // List<Product> productsInCategory = productRepositry.findByProductNameLikeIgnoreCase('%'+keyword+'%');
        List<ProductDTO> productDTOS = products.stream().map( product -> modelMapper.map(product,ProductDTO.class)).toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(ProductDTO productDTO, Long productId) {
        Product productDB = productRepositry.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        Product product = modelMapper.map(productDTO,Product.class);
        productDB.setProductName(product.getProductName());
        productDB.setDescription(product.getDescription());
        productDB.setQuantity(product.getQuantity());
        productDB.setPrice(product.getPrice());
        productDB.setDiscount(product.getDiscount());
        productDB.setSpecialPrice(product.getSpecialPrice());

        Product savedProduct = productRepositry.save(productDB);
        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
            List<ProductDTO> products = cart.getCartItems().stream().map(cartItem -> modelMapper.map(cartItem,ProductDTO.class)).toList();
            cartDTO.setProducts(products);
            return cartDTO;
        }).collect(Collectors.toList());

        cartDTOS.forEach(cart -> cartService.updateProductQuantityInCarts(cart.getCartId(),productId));

        ProductDTO savedproductDTO = modelMapper.map(savedProduct,ProductDTO.class);
        return savedproductDTO;
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product productDB = productRepositry.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(),productId));

        productRepositry.delete(productDB);
        return modelMapper.map(productDB,ProductDTO.class);
    }

    @Override
    public ProductDTO saveImage(Long productId, MultipartFile image) throws IOException {
        Product productDB = productRepositry.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));


        String fileName = fileService.uploadImage(path,image);

        productDB.setImage(fileName);

        Product updatedImage = productRepositry.save(productDB);

        return modelMapper.map(updatedImage,ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Product> productPage = productRepositry.findAll(pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream().map( product -> {
            ProductDTO productDTO = modelMapper.map(product,ProductDTO.class);
            productDTO.setImage(constructImageUrl(product.getImage()));
            return productDTO;
        }).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;

    }

    @Override
    public ProductResponse getAllProductsForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        User user = authUtil.loggerInUser();
        Page<Product> pageProducts = productRepositry.findByUser(user, pageDetails);

        List<Product> products = pageProducts.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDb = productRepositry.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        String fileName = fileService.uploadImage(path, image);
        productFromDb.setImage(fileName);

        Product updatedProduct = productRepositry.save(productFromDb);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }


}
