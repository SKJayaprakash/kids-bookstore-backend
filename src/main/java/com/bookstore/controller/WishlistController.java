package com.bookstore.controller;

import com.bookstore.entity.Wishlist;
import com.bookstore.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public List<Wishlist> getUserWishlist(Authentication authentication) {
        return wishlistService.getUserWishlist(authentication.getName());
    }

    @GetMapping("/all")
    public List<Wishlist> getAllWishlists() {
        return wishlistService.getAllWishlists();
    }

    @PostMapping("/add/{bookId}")
    public ResponseEntity<Wishlist> addToWishlist(Authentication authentication, @PathVariable Long bookId) {
        return ResponseEntity.ok(wishlistService.addToWishlist(authentication.getName(), bookId));
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<Void> removeFromWishlist(Authentication authentication, @PathVariable Long bookId) {
        wishlistService.removeFromWishlist(authentication.getName(), bookId);
        return ResponseEntity.ok().build();
    }
}
