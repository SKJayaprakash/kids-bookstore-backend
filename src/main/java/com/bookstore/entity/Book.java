package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;

    @Column(length = 1000)
    private String description;

    private BigDecimal price;
    private Integer stock;
    private String category;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private com.bookstore.enums.AgeGroup ageGroup;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @ElementCollection
    private List<String> additionalImages = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();
}
