package com.bookstore.service;

import com.bookstore.entity.Shop;
import com.bookstore.entity.User;
import com.bookstore.enums.UserRole;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopOwnerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopService shopService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllShopOwners() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(UserRole.SHOP_OWNER))
                .toList();
    }

    public User getShopOwnerById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop owner not found with id: " + id));
        if (!user.getRoles().contains(UserRole.SHOP_OWNER)) {
            throw new RuntimeException("User is not a shop owner");
        }
        return user;
    }

    @Transactional
    public User createShopOwner(User shopOwner, String password) {
        if (userRepository.findByEmail(shopOwner.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + shopOwner.getEmail());
        }
        shopOwner.setPassword(passwordEncoder.encode(password));
        shopOwner.setRoles(List.of(UserRole.SHOP_OWNER));
        shopOwner.setProvider("local");
        return userRepository.save(shopOwner);
    }

    @Transactional
    public void assignShopToOwner(Long ownerId, Long shopId) {
        User owner = getShopOwnerById(ownerId);
        Shop shop = shopService.getShopById(shopId);

        owner.setShop(shop);
        shop.setOwner(owner);

        userRepository.save(owner);
        shopService.assignOwner(shopId, owner);
    }

    @Transactional
    public void deactivateShopOwner(Long id) {
        User owner = getShopOwnerById(id);
        owner.getRoles().remove(UserRole.SHOP_OWNER);
        userRepository.save(owner);
    }

    public List<User> getShopCustomers(String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Shop owner not found"));

        if (owner.getShop() == null) {
            throw new RuntimeException("Shop owner has no assigned shop");
        }

        return userRepository.findByShopIdAndRoleName(owner.getShop().getId(), UserRole.CUSTOMER);
    }
}
