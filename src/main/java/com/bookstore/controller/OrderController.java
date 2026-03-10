package com.bookstore.controller;

import com.bookstore.entity.Order;
import com.bookstore.service.OrderService;
import com.bookstore.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(Authentication authentication,
            @RequestBody com.bookstore.dto.OrderRequest request) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.createOrder(email, request));
    }

    @GetMapping
    public List<Order> getUserOrders(Authentication authentication) {
        return orderService.getUserOrders(authentication.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getOrdersForCurrentShop());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getShopStats() {
        return ResponseEntity.ok(orderService.getShopStats());
    }
}
