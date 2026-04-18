package com.bookstore.service;

import com.bookstore.entity.Shop;
import com.bookstore.repository.ShopRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShopDetectionService {

    @Autowired
    private ShopRepository shopRepository;

    @org.springframework.beans.factory.annotation.Value("${app.base.domain:localhost}")
    private String baseDomain;

    /**
     * Detects shop from HTTP request using multiple strategies:
     * 1. Custom domain (from Host header)
     * 2. Path-based slug (from URL path /shop/{slug}/...)
     * 3. Fallback to shopNumber parameter (deprecated, for backward compatibility)
     */
    public Shop detectShopFromRequest(HttpServletRequest request) {
        // Strategy 1: Try X-Shop-Domain header (host override)
        String xShopDomain = request.getHeader("X-Shop-Domain");
        if (xShopDomain != null && !xShopDomain.isEmpty()) {
            Shop shop = resolveShopFromDomain(xShopDomain);
            if (shop != null) {
                return shop;
            }
        }

        // Strategy 2: Try custom domain from Host header
        String host = request.getHeader("Host");
        if (host != null) {
            Shop shop = resolveShopFromDomain(host);
            if (shop != null) {
                return shop;
            }
        }

        // Strategy 3: Fallback to shopNumber (deprecated)
        String shopNumber = request.getParameter("shopNumber");
        if (shopNumber != null) {
            Optional<Shop> shop = shopRepository.findByShopNumber(shopNumber);
            if (shop.isPresent() && shop.get().isActive()) {
                return shop.get();
            }
        }

        // Strategy 4: Port-based discovery from Origin/Referer (Robust Local Dev Fallback)
        String origin = request.getHeader("Origin");
        if (origin == null) origin = request.getHeader("Referer");
        
        if (origin != null) {
            // Only use port-based fallback if we are on raw 'localhost' without subdomains
            boolean isStrictLocalhost = origin.contains("://localhost:") || origin.contains("://localhost/");
            
            if (isStrictLocalhost) {
                // Default to shop1 for the consolidated port 4000 on strict localhost
                if (origin.contains(":4000")) return shopRepository.findBySlug("shop1").filter(Shop::isActive).orElse(null);
                
                // Backward compatibility for old ports
                if (origin.contains(":5173")) return shopRepository.findBySlug("shop1").filter(Shop::isActive).orElse(null);
                if (origin.contains(":5174")) return shopRepository.findBySlug("shop2").filter(Shop::isActive).orElse(null);
            }
        }

        return null;
    }

    private Shop resolveShopFromDomain(String domain) {
        if (domain == null) return null;
        
        // Strip port if present
        domain = domain.split(":")[0];

        // 1. Direct custom domain lookup
        Optional<Shop> shop = shopRepository.findByCustomDomain(domain);
        if (shop.isPresent() && shop.get().isActive()) {
            return shop.get();
        }

        // 2. Subdomain lookup (e.g. slug.baseDomain)
        if (domain.endsWith("." + baseDomain)) {
            String slug = domain.substring(0, domain.lastIndexOf("." + baseDomain));
            return shopRepository.findBySlug(slug)
                    .filter(Shop::isActive)
                    .orElse(null);
        }

        return null;
    }

    /**
     * Validates slug format: lowercase alphanumeric with hyphens only
     */
    public boolean isValidSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return false;
        }
        return slug.matches("^[a-z0-9]+(-[a-z0-9]+)*$");
    }

    /**
     * Generates a URL-friendly slug from shop name
     */
    public String generateSlugFromName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    /**
     * Validates custom domain format
     */
    public boolean isValidDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return true; // Optional field
        }
        // Basic domain validation
        return domain.matches("^([a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,}$");
    }
}
