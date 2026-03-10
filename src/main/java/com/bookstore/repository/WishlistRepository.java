package com.bookstore.repository;

import com.bookstore.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserEmail(String email);

    Optional<Wishlist> findByUserEmailAndBookId(String email, Long bookId);

    List<Wishlist> findByShopId(Long shopId);
}
