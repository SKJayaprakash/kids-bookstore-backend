package com.bookstore.controller;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderStatus;
import com.bookstore.service.OrderService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${razorpay.key_id}")
    private String keyId;

    @Value("${razorpay.key_secret}")
    private String keySecret;

    @Autowired
    private OrderService orderService;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(@RequestBody Map<String, Object> data) throws RazorpayException {
        Long orderId = Long.parseLong(data.get("orderId").toString());
        Order order = orderService.getOrderById(orderId);

        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        // Razorpay takes amount in paise (1 INR = 100 Paise)
        orderRequest.put("amount", order.getTotalPrice().multiply(new BigDecimal("100")).intValue());
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + orderId);

        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("id", razorpayOrder.get("id"));
        response.put("amount", razorpayOrder.get("amount"));
        response.put("currency", razorpayOrder.get("currency"));
        response.put("keyId", keyId);

        // Update internal order status to PAYMENT_PENDING
        orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING.name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody Map<String, String> data) {
        String razorpayOrderId = data.get("razorpay_order_id");
        String razorpayPaymentId = data.get("razorpay_payment_id");
        String razorpaySignature = data.get("razorpay_signature");
        Long internalOrderId = Long.parseLong(data.get("internal_order_id"));

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                orderService.updateOrderStatus(internalOrderId, OrderStatus.PAYMENT_COMPLETED.name());
                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Invalid signature"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
