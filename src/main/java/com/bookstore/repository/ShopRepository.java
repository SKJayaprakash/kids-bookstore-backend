package com.bookstore.repository;

import com.bookstore.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByShopNumber(String shopNumber);

    boolean existsByShopNumber(String shopNumber);

    // New methods for hybrid multi-tenancy
    Optional<Shop> findBySlug(String slug);

    Optional<Shop> findByCustomDomain(String customDomain);

    boolean existsBySlug(String slug);

    boolean existsByCustomDomain(String customDomain);
}
