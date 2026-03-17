package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Order order;

    @ManyToOne
    @JoinColumn(name = "book_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Book book;

    private Integer quantity;
    private BigDecimal price; // Price at time of purchase
}
