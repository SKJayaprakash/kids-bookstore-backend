package com.bookstore.service;

import com.bookstore.dto.ReviewDto;
import com.bookstore.entity.Book;
import com.bookstore.entity.Review;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.ReviewRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ReviewDto> getReviewsByBookId(Long bookId) {
        return reviewRepository.findByBookId(bookId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ReviewDto addReview(String userEmail, Long bookId, Integer rating, String comment) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Review review = new Review();
        review.setUser(user);
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment);

        Review savedReview = reviewRepository.save(review);
        return mapToDto(savedReview);
    }

    private ReviewDto mapToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setBookId(review.getBook().getId());
        String name = review.getUser().getFirstName();
        if (review.getUser().getLastName() != null) {
            name += " " + review.getUser().getLastName();
        }
        dto.setUserName(name != null ? name : "Anonymous");
        return dto;
    }
}
