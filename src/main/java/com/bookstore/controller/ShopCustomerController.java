package com.bookstore.controller;

import com.bookstore.entity.User;
import com.bookstore.service.ShopOwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shop-owner/customers")
@PreAuthorize("hasRole('SHOP_OWNER')")
public class ShopCustomerController {

    @Autowired
    private ShopOwnerService shopOwnerService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getShopCustomers(Authentication authentication) {
        String email = authentication.getName();
        List<User> customers = shopOwnerService.getShopCustomers(email);

        List<Map<String, Object>> response = customers.stream().map(customer -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", customer.getId());
            map.put("firstName", customer.getFirstName());
            map.put("lastName", customer.getLastName());
            map.put("email", customer.getEmail());
            map.put("phoneNumber", customer.getPhoneNumber());
            map.put("emailVerified", customer.isEmailVerified());
            map.put("phoneVerified", customer.isPhoneVerified());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
