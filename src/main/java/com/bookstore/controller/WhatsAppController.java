package com.bookstore.controller;

import com.bookstore.context.ShopContext;
import com.bookstore.entity.Shop;
import com.bookstore.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    @Autowired
    private WhatsAppService whatsappService;

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification(@RequestBody Map<String, String> request) {
        Shop currentShop = ShopContext.getCurrentShop();
        if (currentShop == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Shop not found in context"));
        }

        String phoneNumber = request.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        try {
            whatsappService.sendTestNotification(currentShop, phoneNumber);
            return ResponseEntity.ok(Map.of("message", "Test notification sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to send test notification: " + e.getMessage()));
        }
    }
}
