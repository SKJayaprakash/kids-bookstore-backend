package com.bookstore.repository;

import com.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r = :roleName")
    java.util.List<User> findByRoleName(
            @org.springframework.data.repository.query.Param("roleName") com.bookstore.enums.UserRole roleName);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE u.shop.id = :shopId AND r = :roleName")
    java.util.List<User> findByShopIdAndRoleName(
            @org.springframework.data.repository.query.Param("shopId") Long shopId,
            @org.springframework.data.repository.query.Param("roleName") com.bookstore.enums.UserRole roleName);
}
