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

    /**
     * Detects shop from HTTP request using multiple strategies:
     * 1. Custom domain (from Host header)
     * 2. Path-based slug (from URL path /shop/{slug}/...)
     * 3. Fallback to shopNumber parameter (deprecated, for backward compatibility)
     */
    public Shop detectShopFromRequest(HttpServletRequest request) {
        // Strategy 1: Try X-Shop-Domain header first (most specific)
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
            // Remove port if present
            host = host.split(":")[0];
            Shop shop = resolveShopFromDomain(host);
            if (shop != null) {
                return shop;
            }
        }

        // Strategy 3: Try path-based slug (/shop/{slug}/...)
        String path = request.getRequestURI();
        String slug = extractSlugFromPath(path);
        if (slug != null) {
            Optional<Shop> shop = shopRepository.findBySlug(slug);
            if (shop.isPresent() && shop.get().isActive()) {
                return shop.get();
            }
        }

        // Strategy 4: Fallback to shopNumber (deprecated)
        String shopNumber = request.getParameter("shopNumber");
        if (shopNumber != null) {
            Optional<Shop> shop = shopRepository.findByShopNumber(shopNumber);
            if (shop.isPresent() && shop.get().isActive()) {
                return shop.get();
            }
        }

        return null;
    }

    private Shop resolveShopFromDomain(String domain) {
        // 1. Direct custom domain lookup
        Optional<Shop> shop = shopRepository.findByCustomDomain(domain);
        if (shop.isPresent() && shop.get().isActive()) {
            return shop.get();
        }

        // 2. Localhost subdomain lookup (e.g., slug.localhost)
        if (domain.contains(".localhost")) {
            String slug = domain.substring(0, domain.indexOf(".localhost"));
            return shopRepository.findBySlug(slug)
                    .filter(Shop::isActive)
                    .orElse(null);
        }

        return null;
    }

    /**
     * Extracts slug from path like /shop/{slug}/... or /shop/{slug}
     * Returns null if path doesn't match pattern
     */
    private String extractSlugFromPath(String path) {
        if (path == null || !path.startsWith("/shop/")) {
            return null;
        }

        // Remove /shop/ prefix
        String remaining = path.substring(6); // "/shop/".length() = 6

        // Extract slug (everything before next / or end of string)
        int nextSlash = remaining.indexOf('/');
        if (nextSlash > 0) {
            return remaining.substring(0, nextSlash);
        } else if (!remaining.isEmpty()) {
            return remaining;
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
