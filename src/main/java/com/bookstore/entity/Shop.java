package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shops")
@Data
@NoArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String shopNumber; // Unique identifier for customer login (deprecated, use slug)

    @Column(unique = true, nullable = false)
    private String slug; // URL-friendly identifier (e.g., "johns-books")

    @Column(unique = true)
    private String customDomain; // Optional custom domain (e.g., "www.johnsbooks.com")

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private String address;
    private String phoneNumber;
    private String email;

    private boolean active = true;

    // Branding
    private String primaryColor; // e.g. "#e91e63"
    private String logoUrl;

    // Container lifecycle
    private String containerId;
    private Integer containerPort;
    private String containerStatus; // BUILDING, RUNNING, STOPPED, FAILED

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private User owner; 

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<Book> books = new ArrayList<>();

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<User> customers = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
