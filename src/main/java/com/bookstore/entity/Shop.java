package com.bookstore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shops")
public class Shop {
    
    public Shop() {}

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

    // Instagram Integration
    private String instagramAppId;
    @Column(length = 500)
    private String instagramAppSecret;
    @Column(length = 1000)
    private String instagramAccessToken;
    private String instagramUserId;
    private String instagramUsername;
    private LocalDateTime instagramTokenExpiry;

    // WhatsApp Integration
    @Column(length = 1000)
    private String whatsappAccessToken;
    private String whatsappPhoneNumberId;
    private String whatsappOrderTemplateName = "order_confirmation"; // Default template

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner; 

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Book> books = new ArrayList<>();

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShopNumber() { return shopNumber; }
    public void setShopNumber(String shopNumber) { this.shopNumber = shopNumber; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public Integer getContainerPort() { return containerPort; }
    public void setContainerPort(Integer containerPort) { this.containerPort = containerPort; }

    public String getContainerStatus() { return containerStatus; }
    public void setContainerStatus(String containerStatus) { this.containerStatus = containerStatus; }

    public String getInstagramAppId() { return instagramAppId; }
    public void setInstagramAppId(String instagramAppId) { this.instagramAppId = instagramAppId; }

    public String getInstagramAppSecret() { return instagramAppSecret; }
    public void setInstagramAppSecret(String instagramAppSecret) { this.instagramAppSecret = instagramAppSecret; }

    public String getInstagramAccessToken() { return instagramAccessToken; }
    public void setInstagramAccessToken(String instagramAccessToken) { this.instagramAccessToken = instagramAccessToken; }

    public String getInstagramUserId() { return instagramUserId; }
    public void setInstagramUserId(String instagramUserId) { this.instagramUserId = instagramUserId; }

    public String getInstagramUsername() { return instagramUsername; }
    public void setInstagramUsername(String instagramUsername) { this.instagramUsername = instagramUsername; }

    public LocalDateTime getInstagramTokenExpiry() { return instagramTokenExpiry; }
    public void setInstagramTokenExpiry(LocalDateTime instagramTokenExpiry) { this.instagramTokenExpiry = instagramTokenExpiry; }

    public String getWhatsappAccessToken() { return whatsappAccessToken; }
    public void setWhatsappAccessToken(String whatsappAccessToken) { this.whatsappAccessToken = whatsappAccessToken; }

    public String getWhatsappPhoneNumberId() { return whatsappPhoneNumberId; }
    public void setWhatsappPhoneNumberId(String whatsappPhoneNumberId) { this.whatsappPhoneNumberId = whatsappPhoneNumberId; }

    public String getWhatsappOrderTemplateName() { return whatsappOrderTemplateName; }
    public void setWhatsappOrderTemplateName(String whatsappOrderTemplateName) { this.whatsappOrderTemplateName = whatsappOrderTemplateName; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<Book> getBooks() { return books; }
    public void setBooks(List<Book> books) { this.books = books; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }

    public List<User> getCustomers() { return customers; }
    public void setCustomers(List<User> customers) { this.customers = customers; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
