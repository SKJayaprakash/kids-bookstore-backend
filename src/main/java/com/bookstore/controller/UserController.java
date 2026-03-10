package com.bookstore.controller;

import com.bookstore.dto.AuthDtos.UserProfileUpdateDto;
import com.bookstore.entity.User;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("emailVerified", user.isEmailVerified());
        response.put("phoneVerified", user.isPhoneVerified());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfileUpdateDto request) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) {
            // If phone number changes, verify status should arguably reset, but sticking to
            // simple update for now
            if (!request.getPhoneNumber().equals(user.getPhoneNumber())) {
                user.setPhoneVerified(false);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // Email update is sensitive, usually requires re-verification.
        // For now, if email changes, we update it and reset verification.
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already in use");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }

        userRepository.save(user);

        return ResponseEntity.ok("Profile updated successfully");
    }
}
