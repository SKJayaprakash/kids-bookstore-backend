package com.bookstore.service;

import com.bookstore.entity.Shop;
import com.bookstore.entity.User;
import com.bookstore.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopService {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ShopDetectionService shopDetectionService;

    @Autowired
    private DockerService dockerService;

    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    public Shop getShopById(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found with id: " + id));
    }

    public Shop getShopByShopNumber(String shopNumber) {
        return shopRepository.findByShopNumber(shopNumber)
                .orElseThrow(() -> new RuntimeException("Shop not found with shop number: " + shopNumber));
    }

    public Shop getShopBySlug(String slug) {
        return shopRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Shop not found with slug: " + slug));
    }

    @Transactional
    public Shop createShop(Shop shop) {
        // Validate shop number
        if (shopRepository.existsByShopNumber(shop.getShopNumber())) {
            throw new RuntimeException("Shop number already exists: " + shop.getShopNumber());
        }

        // Auto-generate slug if not provided
        if (shop.getSlug() == null || shop.getSlug().isEmpty()) {
            shop.setSlug(shopDetectionService.generateSlugFromName(shop.getName()));
        }

        // Validate slug format
        if (!shopDetectionService.isValidSlug(shop.getSlug())) {
            throw new RuntimeException(
                    "Invalid slug format. Use lowercase alphanumeric with hyphens only: " + shop.getSlug());
        }

        // Check slug uniqueness
        if (shopRepository.existsBySlug(shop.getSlug())) {
            throw new RuntimeException("Slug already exists: " + shop.getSlug());
        }

        // Validate custom domain if provided
        if (shop.getCustomDomain() != null && !shop.getCustomDomain().isEmpty()) {
            if (!shopDetectionService.isValidDomain(shop.getCustomDomain())) {
                throw new RuntimeException("Invalid custom domain format: " + shop.getCustomDomain());
            }
            if (shopRepository.existsByCustomDomain(shop.getCustomDomain())) {
                throw new RuntimeException("Custom domain already exists: " + shop.getCustomDomain());
            }
        }

        return shopRepository.save(shop);
    }

    /**
     * Trigger async Docker container provisioning after shop is persisted.
     * Called separately from createShop so the transaction commits first.
     */
    public void triggerContainerProvisioning(Shop shop) {
        dockerService.provisionShopContainer(shop);
    }

    @Transactional
    public Shop updateShop(Long id, Shop shopDetails) {
        Shop shop = getShopById(id);
        shop.setName(shopDetails.getName());
        shop.setDescription(shopDetails.getDescription());
        shop.setAddress(shopDetails.getAddress());
        shop.setPhoneNumber(shopDetails.getPhoneNumber());
        shop.setEmail(shopDetails.getEmail());
        shop.setActive(shopDetails.isActive());

        // Update slug if changed
        if (shopDetails.getSlug() != null && !shopDetails.getSlug().equals(shop.getSlug())) {
            if (!shopDetectionService.isValidSlug(shopDetails.getSlug())) {
                throw new RuntimeException("Invalid slug format: " + shopDetails.getSlug());
            }
            if (shopRepository.existsBySlug(shopDetails.getSlug())) {
                throw new RuntimeException("Slug already exists: " + shopDetails.getSlug());
            }
            shop.setSlug(shopDetails.getSlug());
        }

        // Update custom domain if changed
        if (shopDetails.getCustomDomain() != null && !shopDetails.getCustomDomain().equals(shop.getCustomDomain())) {
            if (!shopDetectionService.isValidDomain(shopDetails.getCustomDomain())) {
                throw new RuntimeException("Invalid custom domain format: " + shopDetails.getCustomDomain());
            }
            if (shopRepository.existsByCustomDomain(shopDetails.getCustomDomain())) {
                throw new RuntimeException("Custom domain already exists: " + shopDetails.getCustomDomain());
            }
            shop.setCustomDomain(shopDetails.getCustomDomain());
        }

        return shopRepository.save(shop);
    }

    @Transactional
    public void deactivateShop(Long id) {
        Shop shop = getShopById(id);
        shop.setActive(false);
        shopRepository.save(shop);
        // Stop and remove the shop's Docker container (if one exists)
        if (shop.getContainerId() != null || "RUNNING".equals(shop.getContainerStatus())
                || "BUILDING".equals(shop.getContainerStatus())) {
            dockerService.stopShopContainer(shop);
        }
    }

    /**
     * Permanently removes a shop and its Docker container from the system.
     */
    @Transactional
    public void deleteShopPermanently(Long id) {
        Shop shop = getShopById(id);
        // Stop container first (best-effort)
        try {
            dockerService.stopShopContainer(shop);
        } catch (Exception e) {
            // Log but don't block deletion
        }
        shopRepository.deleteById(id);
    }

    @Transactional
    public Shop updateShopOwnerSettings(Long id, String name, String description, String primaryColor, String logoUrl,
                                        String whatsappAccessToken, String whatsappPhoneNumberId, String whatsappOrderTemplateName) {
        Shop shop = getShopById(id);
        if (name != null && !name.trim().isEmpty()) {
            shop.setName(name.trim());
        }
        shop.setDescription(description);
        shop.setPrimaryColor(primaryColor);
        shop.setLogoUrl(logoUrl);
        
        shop.setWhatsappAccessToken(whatsappAccessToken);
        shop.setWhatsappPhoneNumberId(whatsappPhoneNumberId);
        if (whatsappOrderTemplateName != null && !whatsappOrderTemplateName.trim().isEmpty()) {
            shop.setWhatsappOrderTemplateName(whatsappOrderTemplateName.trim());
        }
        
        return shopRepository.save(shop);
    }

    @Transactional
    public void assignOwner(Long shopId, User owner) {
        Shop shop = getShopById(shopId);
        shop.setOwner(owner);
        shopRepository.save(shop);
    }

    public boolean isShopActive(String shopNumber) {
        return shopRepository.findByShopNumber(shopNumber)
                .map(Shop::isActive)
                .orElse(false);
    }

    @Autowired
    private com.bookstore.repository.UserRepository userRepository;

    public java.util.Map<String, Object> getAdminStats() {
        long totalShops = shopRepository.count();
        long activeShops = shopRepository.findAll().stream().filter(Shop::isActive).count();

        long totalShopOwners = userRepository.findByRoleName(com.bookstore.enums.UserRole.SHOP_OWNER).size();
        long totalCustomers = userRepository.findByRoleName(com.bookstore.enums.UserRole.CUSTOMER).size();

        return java.util.Map.of(
                "totalShops", totalShops,
                "activeShops", activeShops,
                "totalShopOwners", totalShopOwners,
                "totalCustomers", totalCustomers);
    }
}
