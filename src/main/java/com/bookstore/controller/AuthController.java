package com.bookstore.controller;

import com.bookstore.dto.AuthDtos.LoginRequest;
import com.bookstore.dto.AuthDtos.SignupRequest;
import com.bookstore.entity.Shop;
import com.bookstore.entity.User;
import com.bookstore.enums.UserRole;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtUtils;
import com.bookstore.service.ShopDetectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UserDetails;

import com.bookstore.dto.AuthDtos.GoogleAuthRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;
import java.util.Optional;
import com.bookstore.security.UserDetailsServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ShopDetectionService shopDetectionService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Value("${bookstore.google.clientId:YOUR_GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        // Detect shop from URL (custom domain or path-based slug)
        Shop shop = shopDetectionService.detectShopFromRequest(httpRequest);

        if (shop == null) {
            return ResponseEntity.badRequest().body(
                    "Unable to detect shop from URL. Please access via shop-specific URL (e.g., /shop/your-shop or custom domain)");
        }

        if (!shop.isActive()) {
            return ResponseEntity.badRequest().body("Shop is not active");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRoles(List.of(UserRole.CUSTOMER));
        user.setProvider("local");
        user.setShop(shop);

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleAuthRequest request, HttpServletRequest httpRequest) {
        // Detect shop from URL
        Shop shop = shopDetectionService.detectShopFromRequest(httpRequest);
        if (shop == null) {
            return ResponseEntity.badRequest().body(
                    "Unable to detect shop from URL. Please access via shop-specific URL.");
        }
        if (!shop.isActive()) {
            return ResponseEntity.badRequest().body("Shop is not active");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // When testing locally with placeholder ID, token verifier will throw/fail.
            // In a real app, googleClientId must match the real Client ID.
            GoogleIdToken idToken = verifier.verify(request.getToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");

                Optional<User> userOpt = userRepository.findByEmail(email);
                User user;

                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    if (user.getShop() == null || !user.getShop().getId().equals(shop.getId())) {
                        return ResponseEntity.badRequest().body("User belongs to a different shop");
                    }
                } else {
                    user = new User();
                    user.setEmail(email);
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setPhoneNumber(null);
                    user.setProvider("google");
                    user.setRoles(List.of(UserRole.CUSTOMER));
                    user.setShop(shop);
                    // generate a random password for OAuth users to satisfy schema constraints
                    user.setPassword(encoder.encode(java.util.UUID.randomUUID().toString()));
                    userRepository.save(user);
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                        userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtils.generateJwtToken(authentication);

                Map<String, Object> response = new HashMap<>();
                response.put("token", jwt);
                response.put("id", user.getId());
                response.put("email", user.getEmail());
                response.put("roles", user.getRoles());
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
                response.put("shopNumber", user.getShop().getShopNumber());
                response.put("shopName", user.getShop().getName());
                response.put("shopSlug", user.getShop().getSlug());
                response.put("shopCustomDomain", user.getShop().getCustomDomain());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("Invalid Google token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error verifying Google token: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // First, authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        // Check if user is SUPER_ADMIN - they don't need shop association
        if (user.getRoles().contains(UserRole.SUPER_ADMIN)) {
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("shopNumber", null);
            response.put("shopName", null);
            response.put("shopSlug", null);
            response.put("shopCustomDomain", null);
            return ResponseEntity.ok(response);
        }

        // SHOP_OWNER: use the shop already assigned to the user — no URL detection
        // needed.
        // This lets shop owners log in from the dedicated shop-owner portal
        // (localhost:5175)
        // without requiring a shop-specific subdomain URL.
        if (user.getRoles().contains(UserRole.SHOP_OWNER)) {
            if (user.getShop() == null) {
                return ResponseEntity.badRequest()
                        .body("Your account has not been assigned to a shop yet. Please contact the Super Admin.");
            }
            if (!user.getShop().isActive()) {
                return ResponseEntity.badRequest().body("Your shop is currently inactive.");
            }
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("shopNumber", user.getShop().getShopNumber());
            response.put("shopName", user.getShop().getName());
            response.put("shopSlug", user.getShop().getSlug());
            response.put("shopCustomDomain", user.getShop().getCustomDomain());
            return ResponseEntity.ok(response);
        }

        // For CUSTOMER users: detect shop from URL (custom domain or path-based slug)
        Shop shop = shopDetectionService.detectShopFromRequest(httpRequest);

        if (shop == null) {
            return ResponseEntity.badRequest().body(
                    "Unable to detect shop from URL. Please access via shop-specific URL (e.g., /shop/your-shop or custom domain)");
        }

        if (!shop.isActive()) {
            return ResponseEntity.badRequest().body("Shop is not active");
        }

        // Verify customer belongs to the detected shop
        if (user.getShop() == null || !user.getShop().getId().equals(shop.getId())) {
            return ResponseEntity.badRequest().body("User does not belong to this shop");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("roles", user.getRoles());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("shopNumber", user.getShop().getShopNumber());
        response.put("shopName", user.getShop().getName());
        response.put("shopSlug", user.getShop().getSlug());
        response.put("shopCustomDomain", user.getShop().getCustomDomain());
        return ResponseEntity.ok(response);
    }
}
