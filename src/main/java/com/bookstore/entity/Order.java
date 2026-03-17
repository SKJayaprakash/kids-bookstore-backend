package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private User user;

    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("userEmail")
    public String provideUserEmail() {
        return user != null ? user.getEmail() : null;
    }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "address_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Address shippingAddress;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Shop shop;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
