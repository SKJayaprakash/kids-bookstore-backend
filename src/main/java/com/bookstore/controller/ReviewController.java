package com.bookstore.controller;

import com.bookstore.dto.ReviewDto;
import com.bookstore.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/book/{bookId}")
    public List<ReviewDto> getReviewsByBookId(@PathVariable Long bookId) {
        return reviewService.getReviewsByBookId(bookId);
    }

    @PostMapping
    public ResponseEntity<ReviewDto> addReview(Authentication authentication,
            @RequestBody Map<String, Object> payload) {
        Long bookId = Long.valueOf(payload.get("bookId").toString());
        Integer rating = Integer.valueOf(payload.get("rating").toString());
        String comment = (String) payload.get("comment");

        return ResponseEntity.ok(reviewService.addReview(authentication.getName(), bookId, rating, comment));
    }
}
