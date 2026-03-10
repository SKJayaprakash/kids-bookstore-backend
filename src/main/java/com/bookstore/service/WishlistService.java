package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.entity.User;
import com.bookstore.entity.Wishlist;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    public List<Wishlist> getUserWishlist(String email) {
        return wishlistRepository.findByUserEmail(email);
    }

    public List<Wishlist> getAllWishlists() {
        com.bookstore.entity.Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop == null) {
            return wishlistRepository.findAll();
        }
        return wishlistRepository.findByShopId(currentShop.getId());
    }

    public Wishlist addToWishlist(String email, Long bookId) {
        if (wishlistRepository.findByUserEmailAndBookId(email, bookId).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Book already in wishlist");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setBook(book);
        wishlist.setShop(book.getShop()); // Link to book's shop

        return wishlistRepository.save(wishlist);
    }

    public void removeFromWishlist(String email, Long bookId) {
        Wishlist wishlist = wishlistRepository.findByUserEmailAndBookId(email, bookId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
        wishlistRepository.delete(wishlist);
    }
}
