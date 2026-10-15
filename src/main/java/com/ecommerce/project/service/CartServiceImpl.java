package com.ecommerce.project.service;

import com.ecommerce.project.exception.APIException;
import com.ecommerce.project.exception.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemsDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositries.CartItemRepository;
import com.ecommerce.project.repositries.CartRepository;
import com.ecommerce.project.repositries.ProductRepositry;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    AuthUtil authUtil;

    @Autowired
    ProductRepositry productRepositry;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        //find existing cart or create one
        Cart cart = createCart();

        //retrieve product details
        Product product = productRepositry.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","ProductId", productId));

        //perform validations
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(
                productId,
                cart.getCartId()
        );
        if(cartItem != null){
            throw new APIException("product "+product.getProductName()+" already exists");
        }
        if(product.getQuantity() == 0){
            throw new APIException("product "+product.getProductName()+" is not available");
        }
        if(product.getQuantity() < quantity){
            throw new APIException("please make an order of the "+product.getProductName()+" less than or equal to the quantity"+product.getQuantity()+".");
        }

        //create cart item
        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setCart(cart);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        //save cart item
        cartItemRepository.save(newCartItem);
//        cart = cartRepository.findById(cart.getCartId()).get();

       // product.setQuantity(product.getQuantity());

        cart.setTotalPrice(cart.getTotalPrice()+(product.getSpecialPrice()* quantity));
        cartRepository.save(cart);
        //return update cart
        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

        List<CartItem> cartItems = cartItemRepository.findByCartCartId(cart.getCartId());
        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });
        cartDTO.setProducts(productStream.toList());
        return  cartDTO;

    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if(carts.size() == 0){
            throw new APIException("No cart exists");
        }

        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream().map(cartItem -> {
                ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                productDTO.setQuantity(cartItem.getQuantity());
                return productDTO;
            }).collect(Collectors.toList());


            cartDTO.setProducts(products);
            return cartDTO;
        }).collect(Collectors.toList());

        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if(cart == null){
            throw new ResourceNotFoundException("Cart","CartId", cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
        cart.getCartItems().forEach(c -> {
            c.getProduct().setQuantity((c.getQuantity()));
        });
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p -> modelMapper.map(p.getProduct(),ProductDTO.class))
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {

        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","CartId", cartId));

        Product product = productRepositry.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","ProductId", productId));

        if(product.getQuantity() == 0){
            throw new APIException("product "+product.getProductName()+" is not available");
        }
        if(product.getQuantity() < quantity){
            throw new APIException("please make an order of the "+product.getProductName()+" less than or equal to the quantity"+product.getQuantity()+".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId,cartId);
        if(cartItem == null){
            throw new APIException("product "+product.getProductName()+" not available in the cart");
        }
        int newQuantity = cartItem.getQuantity() + quantity;
        //validation to prevent negative Quantities
        if(newQuantity < 0){
            throw new APIException("the resulting quantity cannot be negative");
        }
        if(newQuantity == 0){
            deleteProductFromCart(cartId, productId);
        }else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }
        CartItem updatedItem = cartItemRepository.save(cartItem);

        if(updatedItem.getQuantity() == 0){
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productStream = cartItems.stream().map(item ->{
            ProductDTO prd = modelMapper.map(item.getProduct(),ProductDTO.class);
            prd.setQuantity(item.getQuantity());
            return prd;
        });
        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","CartId", cartId));
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId,cartId);
        if(cartItem == null){
            throw new ResourceNotFoundException("product","ProductId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice()-(cartItem.getProductPrice()*cartItem.getQuantity()));
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);
        return "product "+ cartItem.getProduct().getProductName()+" removed from cart";
    }

    @Override
    public void updateProductQuantityInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","CartId", cartId));

        Product product = productRepositry.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","ProductId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId,cartId);

        if(cartItem == null){
            throw new APIException("Product "+ product.getProductName()+" not available in the cart");
        }
        double cartPrice = cart.getTotalPrice()-(cartItem.getProductPrice()*cartItem.getQuantity());
        cartItem.setProductPrice(product.getSpecialPrice());
        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItem =  cartItemRepository.save(cartItem);

    }

    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemsDTO> cartItems) {
        // Get user's email
        String emailId = authUtil.loggedInEmail();

        //check if an existing cart is available or create a new one
        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if(existingCart == null){
            existingCart = new Cart();
            existingCart.setTotalPrice(0.00);
            existingCart.setUser(authUtil.loggerInUser());
            existingCart = cartRepository.save(existingCart);
        }else{
            //clear all current items in the existing cart
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }
        double totalPrice = 0.00;
        //process each item in the request to add to the cart
        for(CartItemsDTO cartItemDTO : cartItems){
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            //find the product by id
            Product product = productRepositry.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

            //directly update product stock and total price
            // product.setQuantity(product.getQuantity()-quantity);
            totalPrice += product.getSpecialPrice()* quantity;
            //create and save cart item
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }
        //update the cart's total price and save
        existingCart.setTotalPrice(totalPrice);
        cartRepository.save(existingCart);
        return "Cart created/updated with new items successfully ";
    }

    private Cart createCart(){
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggerInUser());
        Cart newCart = cartRepository.save(cart);
        return newCart;
    }
}
