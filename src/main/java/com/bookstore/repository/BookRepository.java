package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByShopIdAndCategory(Long shopId, String category, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Book b WHERE b.shop.id = :shopId AND (LOWER(b.title) LIKE LOWER(CONCAT('%',:searchTerm,'%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%',:searchTerm,'%')))")
    Page<Book> searchBooksByShop(@org.springframework.data.repository.query.Param("shopId") Long shopId,
            @org.springframework.data.repository.query.Param("searchTerm") String searchTerm, Pageable pageable);

    Page<Book> findByShopIdAndAgeGroup(Long shopId, com.bookstore.enums.AgeGroup ageGroup, Pageable pageable);

    Page<Book> findByShopIdAndCategoryAndAgeGroup(Long shopId, String category, com.bookstore.enums.AgeGroup ageGroup,
            Pageable pageable);

    Page<Book> findByShopId(Long shopId, Pageable pageable);

    Page<Book> findByCategory(String category, Pageable pageable);

    Page<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author,
            Pageable pageable);

    Page<Book> findByAgeGroup(com.bookstore.enums.AgeGroup ageGroup, Pageable pageable);

    Page<Book> findByCategoryAndAgeGroup(String category, com.bookstore.enums.AgeGroup ageGroup, Pageable pageable);
}
