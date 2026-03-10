package com.bookstore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDto {
    private Long id;
    private Integer rating;
    private String comment;
    private String userName;
    private Long bookId;
    private LocalDateTime createdAt;
}
