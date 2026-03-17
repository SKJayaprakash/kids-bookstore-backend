package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String phoneNumber;

    private boolean emailVerified = false;
    private boolean phoneVerified = false;

    private String firstName;
    private String lastName;

    private String provider; // "local", "google", etc.

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<com.bookstore.enums.UserRole> roles = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "shop_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Shop shop; 

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<Address> addresses = new ArrayList<>();
}
