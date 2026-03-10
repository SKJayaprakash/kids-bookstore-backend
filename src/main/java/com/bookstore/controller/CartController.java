package com.bookstore.controller;

import com.bookstore.entity.Cart;
import com.bookstore.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public List<Cart> getUserCart(Authentication authentication) {
        return cartService.getUserCart(authentication.getName());
    }

    @GetMapping("/all")
    public List<Cart> getAllCarts() {
        return cartService.getCartsForCurrentShop();
    }

    @PostMapping("/add/{bookId}")
    public ResponseEntity<Cart> addToCart(Authentication authentication,
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        return ResponseEntity.ok(cartService.addToCart(authentication.getName(), bookId, quantity));
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<Void> removeFromCart(Authentication authentication, @PathVariable Long bookId) {
        cartService.removeFromCart(authentication.getName(), bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
