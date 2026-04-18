package com.bookstore.service;

import com.bookstore.entity.Order;
import com.bookstore.entity.Shop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v21.0";

    @Autowired
    private RestTemplate restTemplate;

    public void sendOrderNotification(Order order) {
        if (order == null || order.getShop() == null) {
            return;
        }

        Shop shop = order.getShop();
        String accessToken = shop.getWhatsappAccessToken();
        String phoneNumberId = shop.getWhatsappPhoneNumberId();
        String templateName = shop.getWhatsappOrderTemplateName();

        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            logger.info("WhatsApp notification skipped for Order ID {} - Shop {} has not configured WhatsApp.", 
                order.getId(), shop.getId());
            return;
        }

        String rawPhone = getRecipientPhone(order);
        if (rawPhone == null || rawPhone.isBlank()) {
            logger.warn("WhatsApp notification skipped for Order ID {} - User has no phone number.", order.getId());
            return;
        }

        String e164Phone = formatPhoneForWhatsApp(rawPhone);

        if (templateName == null || templateName.isBlank()) {
            templateName = "order_confirmation";
        }

        try {
            logger.info("Attempting to send WhatsApp notification to {} via Shop {} (Order ID {})", 
                e164Phone, shop.getId(), order.getId());
            
            sendWhatsAppTemplate(accessToken, phoneNumberId, e164Phone, templateName);
            logger.info("WhatsApp notification sent successfully for Order {}.", order.getId());
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp notification for Order {}: {}", order.getId(), e.getMessage());
        }
    }

    public void sendTestNotification(Shop shop, String recipientPhone) {
        String accessToken = shop.getWhatsappAccessToken();
        String phoneNumberId = shop.getWhatsappPhoneNumberId();
        String templateName = shop.getWhatsappOrderTemplateName();

        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new RuntimeException("WhatsApp is not configured for this shop.");
        }

        String e164Phone = formatPhoneForWhatsApp(recipientPhone);
        if (templateName == null || templateName.isBlank()) {
            templateName = "order_confirmation";
        }

        logger.info("Sending test WhatsApp notification to {} for Shop {}", e164Phone, shop.getId());
        sendWhatsAppTemplate(accessToken, phoneNumberId, e164Phone, templateName);
    }

    private void sendWhatsAppTemplate(String accessToken, String phoneNumberId, String recipientPhone, String templateName) {
        String url = GRAPH_API_BASE + "/" + phoneNumberId + "/messages";

        // Build Payload (Basic template without parameters for now, matching most 'order_confirmation' defaults)
        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to", recipientPhone,
            "type", "template",
            "template", Map.of(
                "name", templateName,
                "language", Map.of("code", "en_US")
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("WhatsApp API error: " + response.getBody());
        }
    }

    private String getRecipientPhone(Order order) {
        if (order.getShippingAddress() != null && order.getShippingAddress().getUser() != null) {
            return order.getShippingAddress().getUser().getPhoneNumber();
        }
        if (order.getUser() != null) {
            return order.getUser().getPhoneNumber();
        }
        return null;
    }

    private String formatPhoneForWhatsApp(String phone) {
        String clean = phone.replaceAll("[^0-9]", "");
        return clean;
    }
}
