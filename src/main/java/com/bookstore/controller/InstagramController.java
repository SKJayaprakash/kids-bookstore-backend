package com.bookstore.controller;

import com.bookstore.context.ShopContext;
import com.bookstore.entity.Book;
import com.bookstore.entity.Shop;
import com.bookstore.service.BookService;
import com.bookstore.service.InstagramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/instagram")
public class InstagramController {

    private static final Logger logger = LoggerFactory.getLogger(InstagramController.class);

    @Autowired
    private InstagramService instagramService;

    @Autowired
    private BookService bookService;

    /**
     * Save Instagram App credentials (App ID + App Secret) for the shop
     */
    @PostMapping("/config")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'SUPER_ADMIN')")
    public ResponseEntity<?> saveConfig(@RequestBody Map<String, String> payload) {
        System.out.println("DIAG - Controller: SAVING CONFIG ATTEMPT REACHED");
        try {
            String user = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("DIAG - Controller: User: " + user);

            Shop shop = ShopContext.getCurrentShop();
            if (shop == null) {
                System.err.println("DIAG - Controller: SHOP CONTEXT IS NULL");
                return ResponseEntity.badRequest().body(Map.of("error", "Shop context not found for user: " + user));
            }

            String appId = payload.get("appId");
            String appSecret = payload.get("appSecret");

            System.out.println("DIAG - Controller: Attempting save for shop " + shop.getName() + " (ID: " + shop.getId() + ")");

            if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "App ID and App Secret are required"));
            }

            instagramService.saveAppCredentials(shop, appId, appSecret);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Instagram credentials saved successfully!",
                    "shopId", shop.getId(),
                    "appId", appId.substring(0, Math.min(appId.length(), 4))
            ));
        } catch (Exception e) {
            System.err.println("DIAG - Controller Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get the OAuth authorization URL for connecting Instagram
     */
    @GetMapping("/auth-url")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAuthUrl() {
        String user = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Shop shop = ShopContext.getCurrentShop();
        
        if (shop == null) {
            logger.error("DIAG - Controller: getAuthUrl failed. Shop context MISSING for user: {}", user);
            return ResponseEntity.badRequest().body(Map.of("error", "Shop context not found"));
        }

        logger.info("DIAG - Controller: Generating Auth URL for shop: {} (ID: {})", shop.getName(), shop.getId());
        
        if (!instagramService.isConfigured(shop)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please configure your Instagram App ID and App Secret first."
            ));
        }

        String url = instagramService.getAuthorizationUrl(shop);
        logger.info("DIAG - Controller: Generated URL starts with: {}", url.substring(0, Math.min(url.length(), 100)));

        return ResponseEntity.ok(Map.of("authUrl", url));
    }

    /**
     * Handle OAuth callback from Instagram
     */
    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        try {
            logger.info("DIAG - Instagram callback hit. code: {}, state: {}", code, state);
            if (error != null) {
                logger.error("DIAG - Instagram error from Meta: {} - {}", error, errorDescription);
                return ResponseEntity.badRequest().body(Map.of("error", errorDescription != null ? errorDescription : error));
            }

            if (code == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No authorization code received"));
            }

            Shop shop = ShopContext.getCurrentShop();
            Long shopId = shop != null ? shop.getId() : (state != null ? Long.parseLong(state) : null);

            if (shopId == null) {
                logger.error("DIAG - Could not determine shop for Instagram callback");
                return ResponseEntity.badRequest().body(Map.of("error", "Could not determine shop context"));
            }

            instagramService.handleOAuthCallback(code, shopId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Instagram connected successfully!"
            ));
        } catch (Exception e) {
            logger.error("DIAG - Instagram callback failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to connect Instagram: " + e.getMessage()
            ));
        }
    }

    /**
     * Get Instagram connection status for the current shop
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'SUPER_ADMIN')")
    public ResponseEntity<?> getStatus() {
        Shop shop = ShopContext.getCurrentShop();
        if (shop == null) {
            return ResponseEntity.ok(Map.of("connected", false, "configured", false));
        }
        return ResponseEntity.ok(instagramService.getConnectionStatus(shop));
    }

    /**
     * Manually post an existing book to Instagram
     */
    @PostMapping("/post/{bookId}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'SUPER_ADMIN')")
    public ResponseEntity<?> postBookToInstagram(@PathVariable Long bookId) {
        try {
            Book book = bookService.getBookById(bookId);
            Map<String, Object> result = instagramService.publishBookPost(book, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to post to Instagram: " + e.getMessage()
            ));
        }
    }

    /**
     * Disconnect Instagram from the current shop
     */
    @DeleteMapping("/disconnect")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'SUPER_ADMIN')")
    public ResponseEntity<?> disconnect() {
        Shop shop = ShopContext.getCurrentShop();
        if (shop == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Shop context not found"));
        }

        instagramService.disconnectAccount(shop);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Instagram disconnected successfully"
        ));
    }

    @PostMapping("/test-post")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<?> testPost() {
        Shop shop = ShopContext.getCurrentShop();
        if (shop == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Shop not found in context"));
        }

        try {
            return ResponseEntity.ok(instagramService.testPublish(shop));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
