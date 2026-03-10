package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.User;
import com.bookstore.entity.Shop;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.context.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    public List<Cart> getUserCart(String email) {
        return cartRepository.findByUserEmail(email);
    }

    public List<Cart> getCartsForCurrentShop() {
        Shop currentShop = ShopContext.getCurrentShop();
        if (currentShop == null) {
            throw new RuntimeException("Shop context not found");
        }
        return cartRepository.findByShopId(currentShop.getId());
    }

    @Transactional
    public Cart addToCart(String email, Long bookId, Integer quantity) {
        Optional<Cart> existing = cartRepository.findByUserEmailAndBookId(email, bookId);
        if (existing.isPresent()) {
            Cart cart = existing.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            return cartRepository.save(cart);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setBook(book);
        cart.setQuantity(quantity);
        cart.setShop(book.getShop());

        return cartRepository.save(cart);
    }

    @Transactional
    public void removeFromCart(String email, Long bookId) {
        Cart cart = cartRepository.findByUserEmailAndBookId(email, bookId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        cartRepository.delete(cart);
    }

    @Transactional
    public void clearCart(String email) {
        List<Cart> items = cartRepository.findByUserEmail(email);
        cartRepository.deleteAll(items);
    }
}
