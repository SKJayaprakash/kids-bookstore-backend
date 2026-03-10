package com.bookstore.service;

import com.bookstore.entity.Address;
import com.bookstore.entity.User;
import com.bookstore.repository.AddressRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    public Address addAddress(String email, Address address) {
        User user = userRepository.findByEmail(email).orElseThrow();
        address.setUser(user);
        return addressRepository.save(address);
    }

    public List<Address> getUserAddresses(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getAddresses();
    }

    public void deleteAddress(Long id) {
        addressRepository.deleteById(id);
    }

    public Address updateAddress(Long id, Address updatedAddress, String userEmail) {
        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!existingAddress.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access to address");
        }

        existingAddress.setStreet(updatedAddress.getStreet());
        existingAddress.setCity(updatedAddress.getCity());
        existingAddress.setState(updatedAddress.getState());
        existingAddress.setZipCode(updatedAddress.getZipCode());
        existingAddress.setCountry(updatedAddress.getCountry());

        return addressRepository.save(existingAddress);
    }
}
