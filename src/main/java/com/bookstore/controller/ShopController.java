package com.bookstore.controller;

import com.bookstore.dto.ShopDtos.*;
import com.bookstore.entity.Shop;
import com.bookstore.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private com.bookstore.service.OrderService orderService;

    // Public endpoint - get shop by shop number (for customer login - deprecated)
    @GetMapping("/shops/{shopNumber}")
    public ResponseEntity<ShopResponse> getShopByNumber(@PathVariable String shopNumber) {
        Shop shop = shopService.getShopByShopNumber(shopNumber);
        return ResponseEntity.ok(toShopResponse(shop));
    }

    // Public endpoint - get shop by slug (for path-based access)
    @GetMapping("/shops/by-slug/{slug}")
    public ResponseEntity<ShopResponse> getShopBySlug(@PathVariable String slug) {
        Shop shop = shopService.getShopBySlug(slug);
        return ResponseEntity.ok(toShopResponse(shop));
    }

    // Public endpoint - get metadata for the current shop (detected by URL/Headers)
    @GetMapping("/public/current")
    public ResponseEntity<ShopResponse> getCurrentShopMetadata() {
        Shop shop = com.bookstore.context.ShopContext.getCurrentShop();
        if (shop == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toShopResponse(shop));
    }

    // Super Admin endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/admin/shops")
    public ResponseEntity<List<ShopResponse>> getAllShops() {
        List<Shop> shops = shopService.getAllShops();
        return ResponseEntity.ok(shops.stream()
                .map(this::toShopResponse)
                .collect(Collectors.toList()));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(shopService.getAdminStats());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/admin/stats/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedAdminStats() {
        return ResponseEntity.ok(orderService.getAdvancedAdminStats());
    }

    @Autowired
    private com.bookstore.service.FileStorageService fileStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/repair-s3-acls")
    public ResponseEntity<Map<String, Object>> repairAcls() {
        return ResponseEntity.ok(fileStorageService.repairS3Acls());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/repair-db-constraints")
    public ResponseEntity<String> repairDbConstraints() {
        try {
            jdbcTemplate.execute("ALTER TABLE books DROP CONSTRAINT IF EXISTS books_age_group_check");
            jdbcTemplate.execute("ALTER TABLE books ADD CONSTRAINT books_age_group_check CHECK (age_group IN ('BABIES_TODDLERS', 'PRESCHOOL', 'EARLY_READERS', 'MIDDLE_GRADE', 'YOUNG_ADULT', 'ALL_AGES'))");
            return ResponseEntity.ok("Database constraints repaired successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error repairing database constraints: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/shops")
    public ResponseEntity<ShopResponse> createShop(@RequestBody CreateShopRequest request) {
        Shop shop = new Shop();
        shop.setShopNumber(request.getShopNumber());
        shop.setSlug(request.getSlug());
        shop.setCustomDomain(blankToNull(request.getCustomDomain()));
        shop.setName(request.getName());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress());
        shop.setPhoneNumber(request.getPhoneNumber());
        shop.setEmail(request.getEmail());
        shop.setPrimaryColor(blankToNull(request.getPrimaryColor()));
        shop.setLogoUrl(blankToNull(request.getLogoUrl()));

        Shop created = shopService.createShop(shop);
        // Container provisioning is now done manually — no auto-Docker on shop create
        return ResponseEntity.ok(toShopResponse(created));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/admin/shops/{id}")
    public ResponseEntity<ShopResponse> updateShop(
            @PathVariable Long id,
            @RequestBody UpdateShopRequest request) {
        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setSlug(request.getSlug());
        shop.setCustomDomain(blankToNull(request.getCustomDomain()));
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress());
        shop.setPhoneNumber(request.getPhoneNumber());
        shop.setEmail(request.getEmail());
        shop.setPrimaryColor(blankToNull(request.getPrimaryColor()));
        shop.setLogoUrl(blankToNull(request.getLogoUrl()));
        if (request.getActive() != null) {
            shop.setActive(request.getActive());
        }

        Shop updated = shopService.updateShop(id, shop);
        return ResponseEntity.ok(toShopResponse(updated));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/admin/shops/{id}")
    public ResponseEntity<Void> deactivateShop(@PathVariable Long id) {
        shopService.deactivateShop(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/admin/shops/{id}/permanent")
    public ResponseEntity<Void> deleteShopPermanently(@PathVariable Long id) {
        shopService.deleteShopPermanently(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping("/shop-owner/my-shop")
    public ResponseEntity<ShopResponse> getMyShop() {
        Shop shop = com.bookstore.context.ShopContext.getCurrentShop();
        if (shop == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toShopResponse(shop));
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PutMapping("/shop-owner/my-shop")
    public ResponseEntity<ShopResponse> updateMyShop(@RequestBody UpdateShopRequest request) {
        Shop shop = com.bookstore.context.ShopContext.getCurrentShop();
        if (shop == null) {
            return ResponseEntity.notFound().build();
        }
        
        Shop updated = shopService.updateShopOwnerSettings(
            shop.getId(),
            request.getName(),
            blankToNull(request.getDescription()),
            blankToNull(request.getPrimaryColor()),
            blankToNull(request.getLogoUrl())
        );
        return ResponseEntity.ok(toShopResponse(updated));
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping("/stats/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedShopStats() {
        return ResponseEntity.ok(orderService.getAdvancedShopStats());
    }

    // Container status polling endpoint
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/admin/shops/{id}/status")
    public ResponseEntity<Map<String, Object>> getShopContainerStatus(@PathVariable Long id) {
        Shop shop = shopService.getShopById(id);
        return ResponseEntity.ok(Map.of(
                "containerStatus", shop.getContainerStatus() != null ? shop.getContainerStatus() : "NOT_STARTED",
                "containerPort", shop.getContainerPort() != null ? shop.getContainerPort() : 0,
                "containerId", shop.getContainerId() != null ? shop.getContainerId() : ""));
    }

    private ShopResponse toShopResponse(Shop shop) {
        ShopResponse response = new ShopResponse();
        response.setId(shop.getId());
        response.setShopNumber(shop.getShopNumber());
        response.setSlug(shop.getSlug());
        response.setCustomDomain(shop.getCustomDomain());
        response.setName(shop.getName());
        response.setDescription(shop.getDescription());
        response.setAddress(shop.getAddress());
        response.setPhoneNumber(shop.getPhoneNumber());
        response.setEmail(shop.getEmail());
        response.setActive(shop.isActive());
        response.setPrimaryColor(shop.getPrimaryColor());
        response.setLogoUrl(shop.getLogoUrl());
        response.setContainerStatus(shop.getContainerStatus());
        response.setContainerPort(shop.getContainerPort());

        if (shop.getOwner() != null) {
            response.setOwnerEmail(shop.getOwner().getEmail());
            response.setOwnerName(shop.getOwner().getFirstName() + " " + shop.getOwner().getLastName());
        }

        return response;
    }

    /**
     * Converts blank/empty strings to null — prevents unique constraint violations
     * on nullable columns.
     */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
