package com.bookstore.service;

import com.bookstore.entity.*;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.AddressRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Transactional
    public Order createOrder(String email, com.bookstore.dto.OrderRequest request) {
        User user = userRepository.findByEmail(email).orElse(null);

        Order order = new Order();
        if (user != null) {
            order.setUser(user);
        }
        order.setStatus(OrderStatus.CREATED);

        // Associate with current shop if present
        order.setShop(com.bookstore.context.ShopContext.getCurrentShop());

        // Handle Address
        Address address = request.getAddress();
        if (address != null) {
            if (address.getId() != null && address.getId() > 0) {
                address = addressRepository.findById(address.getId())
                        .orElseThrow(() -> new RuntimeException("Address not found"));
            } else {
                if (user != null) {
                    address.setUser(user);
                }
                address = addressRepository.save(address);
            }
            order.setShippingAddress(address);
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (com.bookstore.dto.OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Book book = bookRepository.findById(itemRequest.getBookId())
                    .orElseThrow(() -> new RuntimeException("Book not found: " + itemRequest.getBookId()));

            OrderItem item = new OrderItem();
            item.setBook(book);
            item.setOrder(order);
            item.setQuantity(itemRequest.getQuantity());

            BigDecimal price = book.getPrice();
            item.setPrice(price);

            order.getItems().add(item);

            totalPrice = totalPrice.add(price.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalPrice(totalPrice);

        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return orderRepository.findByUserId(user.getId());
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getOrdersForCurrentShop() {
        Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop == null) {
            throw new RuntimeException("Shop context not found");
        }
        return orderRepository.findByShopId(currentShop.getId());
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(OrderStatus.valueOf(newStatus.toUpperCase()));
        return orderRepository.save(order);
    }

    public Map<String, Object> getShopStats() {
        Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop == null) {
            throw new RuntimeException("Shop context not found");
        }

        Long shopId = currentShop.getId();

        long totalBooks = bookRepository.findByShopId(shopId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().size();
        List<Order> orders = orderRepository.findByShopId(shopId);
        long totalOrders = orders.size();
        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CREATED || o.getStatus() == OrderStatus.PAYMENT_PENDING
                        || o.getStatus() == OrderStatus.ACCEPTED)
                .count();

        // Calculate unique customers from orders
        long totalCustomers = orders.stream()
                .map(o -> o.getUser().getId())
                .distinct()
                .count();

        return Map.of(
                "totalBooks", totalBooks,
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "totalCustomers", totalCustomers);
    }
}
