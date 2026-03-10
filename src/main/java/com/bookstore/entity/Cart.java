package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("userEmail")
    public String provideUserEmail() {
        return user != null ? user.getEmail() : null;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id")
    private Book book;

    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop;
}
