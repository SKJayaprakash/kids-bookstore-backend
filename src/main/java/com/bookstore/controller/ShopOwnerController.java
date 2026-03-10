package com.bookstore.controller;

import com.bookstore.dto.ShopDtos.*;
import com.bookstore.entity.User;
import com.bookstore.service.ShopOwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/shop-owners")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ShopOwnerController {

    @Autowired
    private ShopOwnerService shopOwnerService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllShopOwners() {
        List<User> owners = shopOwnerService.getAllShopOwners();
        return ResponseEntity.ok(owners.stream()
                .map(this::toShopOwnerResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createShopOwner(@RequestBody CreateShopOwnerRequest request) {
        User owner = new User();
        owner.setEmail(request.getEmail());
        owner.setFirstName(request.getFirstName());
        owner.setLastName(request.getLastName());
        owner.setPhoneNumber(request.getPhoneNumber());

        User created = shopOwnerService.createShopOwner(owner, request.getPassword());

        // Optionally assign shop during creation
        if (request.getShopId() != null) {
            shopOwnerService.assignShopToOwner(created.getId(), request.getShopId());
        }

        return ResponseEntity.ok(toShopOwnerResponse(created));
    }

    @PutMapping("/{id}/assign-shop/{shopId}")
    public ResponseEntity<Void> assignShopToOwner(
            @PathVariable Long id,
            @PathVariable Long shopId) {
        shopOwnerService.assignShopToOwner(id, shopId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateShopOwner(@PathVariable Long id) {
        shopOwnerService.deactivateShopOwner(id);
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toShopOwnerResponse(User owner) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", owner.getId());
        response.put("email", owner.getEmail());
        response.put("firstName", owner.getFirstName());
        response.put("lastName", owner.getLastName());
        response.put("phoneNumber", owner.getPhoneNumber());
        response.put("roles", owner.getRoles());

        if (owner.getShop() != null) {
            Map<String, Object> shopInfo = new HashMap<>();
            shopInfo.put("id", owner.getShop().getId());
            shopInfo.put("shopNumber", owner.getShop().getShopNumber());
            shopInfo.put("name", owner.getShop().getName());
            response.put("shop", shopInfo);
        }

        return response;
    }
}
