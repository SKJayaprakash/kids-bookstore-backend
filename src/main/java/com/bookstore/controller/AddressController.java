package com.bookstore.controller;

import com.bookstore.entity.Address;
import com.bookstore.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public List<Address> getUserAddresses(Authentication authentication) {
        return addressService.getUserAddresses(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<Address> addAddress(Authentication authentication, @RequestBody Address address) {
        return ResponseEntity.ok(addressService.addAddress(authentication.getName(), address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Address> updateAddress(Authentication authentication, @PathVariable Long id,
            @RequestBody Address address) {
        return ResponseEntity.ok(addressService.updateAddress(id, address, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok().build();
    }
}
