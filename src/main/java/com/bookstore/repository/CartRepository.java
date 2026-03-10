package com.bookstore.repository;

import com.bookstore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUserEmail(String email);

    List<Cart> findByShopId(Long shopId);

    Optional<Cart> findByUserEmailAndBookId(String email, Long bookId);
}
