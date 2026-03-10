package com.bookstore.dto;

import lombok.Data;

public class ShopDtos {

    @Data
    public static class CreateShopRequest {
        private String shopNumber;
        private String slug;
        private String customDomain;
        private String name;
        private String description;
        private String address;
        private String phoneNumber;
        private String email;
        private String primaryColor; // e.g. "#e91e63"
        private String logoUrl;
    }

    @Data
    public static class UpdateShopRequest {
        private String name;
        private String slug;
        private String customDomain;
        private String description;
        private String address;
        private String phoneNumber;
        private String email;
        private Boolean active;
        private String primaryColor;
        private String logoUrl;
    }

    @Data
    public static class ShopResponse {
        private Long id;
        private String shopNumber;
        private String slug;
        private String customDomain;
        private String name;
        private String description;
        private String address;
        private String phoneNumber;
        private String email;
        private boolean active;
        private String ownerEmail;
        private String ownerName;
        private String primaryColor;
        private String logoUrl;
        private String containerStatus;
        private Integer containerPort;
    }

    @Data
    public static class CreateShopOwnerRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private Long shopId; // Optional: assign shop during creation
    }
}
