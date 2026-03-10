package com.bookstore.controller;

import com.bookstore.entity.Book;
import com.bookstore.service.BookService;
import com.bookstore.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private BookService bookService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public Page<Book> getAllBooks(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.bookstore.enums.AgeGroup ageGroup) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return bookService.getAllBooks(category, search, ageGroup, PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Book> createBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("category") String category,
            @RequestParam("ageGroup") com.bookstore.enums.AgeGroup ageGroup,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setPrice(price);
        book.setStock(stock);
        book.setCategory(category);
        book.setAgeGroup(ageGroup);

        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(image);
            book.setImageUrl(imageUrl);
        }

        return ResponseEntity.ok(bookService.saveBook(book));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("category") String category,
            @RequestParam("ageGroup") com.bookstore.enums.AgeGroup ageGroup,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        Book existingBook = bookService.getBookById(id);
        if (existingBook == null) {
            return ResponseEntity.notFound().build();
        }

        existingBook.setTitle(title);
        existingBook.setAuthor(author);
        existingBook.setDescription(description);
        existingBook.setPrice(price);
        existingBook.setStock(stock);
        existingBook.setCategory(category);
        existingBook.setAgeGroup(ageGroup);

        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(image);
            existingBook.setImageUrl(imageUrl);
        }
        // If image is null or empty, we assume the existing image URL should be kept
        // unless a specific mechanism for removal is implemented.

        return ResponseEntity.ok(bookService.saveBook(existingBook));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok().build();
    }
}
