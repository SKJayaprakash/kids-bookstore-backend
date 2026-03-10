package com.bookstore.dto;

import lombok.Data;

public class AuthDtos {

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class SignupRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phoneNumber;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String email;
        private String firstName;
        private String lastName;
        private String role;

        public AuthResponse(String token, String email, String firstName, String lastName, String role) {
            this.token = token;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
        }
    }

    @Data
    public static class UserProfileUpdateDto {
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String email;
    }

    @Data
    public static class GoogleAuthRequest {
        private String token;
    }
}
