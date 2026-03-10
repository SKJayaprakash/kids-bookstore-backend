package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    public Page<Book> getAllBooks(Pageable pageable) {
        com.bookstore.entity.Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop != null) {
            return bookRepository.findByShopId(currentShop.getId(), pageable);
        }
        return bookRepository.findAll(pageable);
    }

    public Page<Book> getAllBooks(String category, String search, com.bookstore.enums.AgeGroup ageGroup,
            Pageable pageable) {
        com.bookstore.entity.Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop == null) {
            // Fallback if no context (e.g., tests or Super Admin)
            if (search != null && !search.isEmpty()) {
                return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(search, search,
                        pageable);
            }
            if (category != null && !category.isEmpty() && ageGroup != null) {
                return bookRepository.findByCategoryAndAgeGroup(category, ageGroup, pageable);
            }
            if (category != null && !category.isEmpty()) {
                return bookRepository.findByCategory(category, pageable);
            }
            if (ageGroup != null) {
                return bookRepository.findByAgeGroup(ageGroup, pageable);
            }
            return bookRepository.findAll(pageable);
        }

        Long shopId = currentShop.getId();
        if (search != null && !search.isEmpty()) {
            return bookRepository.searchBooksByShop(shopId, search, pageable);
        }
        if (category != null && !category.isEmpty() && ageGroup != null) {
            return bookRepository.findByShopIdAndCategoryAndAgeGroup(shopId, category, ageGroup, pageable);
        }
        if (category != null && !category.isEmpty()) {
            return bookRepository.findByShopIdAndCategory(shopId, category, pageable);
        }
        if (ageGroup != null) {
            return bookRepository.findByShopIdAndAgeGroup(shopId, ageGroup, pageable);
        }
        return bookRepository.findByShopId(shopId, pageable);
    }

    public Book getBookById(Long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
        com.bookstore.entity.Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop != null && book.getShop() != null && !book.getShop().getId().equals(currentShop.getId())) {
            throw new RuntimeException("Book not found in this shop");
        }
        return book;
    }

    public Book saveBook(Book book) {
        com.bookstore.entity.Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop != null && book.getShop() == null) {
            book.setShop(currentShop);
        }
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        Book book = getBookById(id); // Ensures it belongs to the current shop
        bookRepository.deleteById(book.getId());
    }
}
