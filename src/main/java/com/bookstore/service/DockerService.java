package com.bookstore.service;

import com.bookstore.entity.Shop;
import com.bookstore.repository.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Manages per-shop frontend containers using docker compose.
 *
 * Strategy:
 * 1. Build the shared base image (kids-bookstore-shop-frontend) once.
 * 2. For each shop, append a new service to docker-compose.shops.yml.
 * 3. Run `docker compose up -d shop-<slug>` so all shops appear in the
 * same compose stack alongside postgres, backend, traefik, etc.
 */
@Service
public class DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    @Value("${app.api.url:http://localhost:8081/api}")
    private String apiUrl;


    @Autowired
    private ShopRepository shopRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Triggered asynchronously after a shop is created.
     * Hardcoded server separated model — No-Op dynamic provisioning.
     */
    @Async
    public void provisionShopContainer(Shop shop) {
        log.info("Provisioning for shop '{}' is handled explicitly via separated Docker compose services.",
                shop.getSlug());
        updateContainerStatus(shop.getId(), "RUNNING", null, null);
    }

    /**
     * Stops and removes a shop's compose service.
     * Hardcoded server separated model — No-Op dynamic stop.
     */
    public void stopShopContainer(Shop shop) {
        log.info("Stopping shop '{}' container is handled manually.", shop.getSlug());
        updateContainerStatus(shop.getId(), "STOPPED", null, null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------




    private void updateContainerStatus(Long shopId, String status, String containerId, Integer port) {
        shopRepository.findById(shopId).ifPresent(s -> {
            s.setContainerStatus(status);
            if (containerId != null)
                s.setContainerId(containerId);
            if (port != null)
                s.setContainerPort(port);
            shopRepository.save(s);
        });
    }
}
