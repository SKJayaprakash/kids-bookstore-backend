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

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private WhatsAppService whatsappService;

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
            return List.of(); // Return empty list if no shop context
        }
        return orderRepository.findByShopId(currentShop.getId());
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setStatus(OrderStatus.valueOf(newStatus.toUpperCase()));
        
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            order.setTrackingNumber(trackingNumber.trim());
        }
        if (carrier != null && !carrier.trim().isEmpty()) {
            order.setCarrier(carrier.trim());
        }
        
        Order savedOrder = orderRepository.save(order);
        
        if (savedOrder.getStatus() == OrderStatus.PAYMENT_COMPLETED) {
            emailService.sendOrderReceipt(savedOrder);
            whatsappService.sendOrderNotification(savedOrder);
        }
        
        return savedOrder;
    }

    public Map<String, Object> getShopStats() {
        Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop == null) {
            return Map.of("totalBooks", 0L, "totalOrders", 0L, "pendingOrders", 0L, "totalCustomers", 0L);
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

        // Calculate unique customers from orders (skip guest orders with null user)
        long totalCustomers = orders.stream()
                .filter(o -> o.getUser() != null)
                .map(o -> o.getUser().getId())
                .distinct()
                .count();

        return Map.of(
                "totalBooks", totalBooks,
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "totalCustomers", totalCustomers);
    }

    public Map<String, Object> getAdvancedShopStats() {
        Shop currentShop = com.bookstore.context.ShopContext.getCurrentShop();
        if (currentShop == null) {
            return Map.of("salesTrends", List.of(), "topSellingBooks", List.of(), "lowStockBooks", List.of());
        }

        Long shopId = currentShop.getId();
        
        // 1. Low Stock Books
        List<Book> allBooks = bookRepository.findByShopId(shopId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<Map<String, Object>> lowStockBooks = allBooks.stream()
                .filter(b -> {
                    int threshold = (b.getLowStockThreshold() != null) ? b.getLowStockThreshold() : 10;
                    return b.getStock() < threshold;
                })
                .map(b -> {
                    int threshold = (b.getLowStockThreshold() != null) ? b.getLowStockThreshold() : 10;
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", b.getId());
                    m.put("title", b.getTitle());
                    m.put("stock", b.getStock());
                    m.put("threshold", threshold);
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        List<Order> orders = orderRepository.findByShopId(shopId);

        // 2. Sales Trends (Last 30 Days)
        java.time.LocalDate thirtyDaysAgo = java.time.LocalDate.now().minusDays(30);
        Map<String, double[]> dailyStats = new java.util.TreeMap<>();
        
        for (int i = 0; i <= 30; i++) {
            dailyStats.put(thirtyDaysAgo.plusDays(i).toString(), new double[]{0, 0}); // {orders, revenue}
        }

        orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo.minusDays(1)))
                .filter(o -> o.getStatus() == OrderStatus.PAYMENT_COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .forEach(o -> {
                    String date = o.getCreatedAt().toLocalDate().toString();
                    if (dailyStats.containsKey(date)) {
                        double[] stats = dailyStats.get(date);
                        stats[0] += 1; // Count orders
                        stats[1] += o.getTotalPrice().doubleValue(); // Sum revenue
                    }
                });

        List<Map<String, Object>> salesTrends = dailyStats.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("date", e.getKey());
                    m.put("sales", (int) e.getValue()[0]);
                    m.put("revenue", Math.round(e.getValue()[1] * 100.0) / 100.0);
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        // 3. Top Selling Books
        Map<String, Integer> bookSales = new java.util.HashMap<>();
        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAYMENT_COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    String title = item.getBook().getTitle();
                    bookSales.put(title, bookSales.getOrDefault(title, 0) + item.getQuantity());
                });

        List<Map<String, Object>> topSellingBooks = bookSales.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("title", e.getKey());
                    m.put("copiesSold", e.getValue());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        return Map.of(
                "salesTrends", salesTrends,
                "topSellingBooks", topSellingBooks,
                "lowStockBooks", lowStockBooks
        );
    }

    public Map<String, Object> getAdvancedAdminStats() {
        List<Order> orders = orderRepository.findAll();

        // 1. Total Platform Revenue
        BigDecimal totalPlatformRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAYMENT_COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Revenue By Shop (Top 5)
        Map<String, BigDecimal> shopRevenue = new java.util.HashMap<>();
        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAYMENT_COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .filter(o -> o.getShop() != null)
                .forEach(o -> {
                    String shopName = o.getShop().getName();
                    shopRevenue.put(shopName, shopRevenue.getOrDefault(shopName, BigDecimal.ZERO).add(o.getTotalPrice()));
                });

        List<Map<String, Object>> revenueByShop = shopRevenue.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("shopName", e.getKey());
                    m.put("revenue", e.getValue().doubleValue());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        // 3. Platform Sales Trend (30 days)
        java.time.LocalDate thirtyDaysAgo = java.time.LocalDate.now().minusDays(30);
        Map<String, double[]> dailyStats = new java.util.TreeMap<>();
        
        for (int i = 0; i <= 30; i++) {
            dailyStats.put(thirtyDaysAgo.plusDays(i).toString(), new double[]{0, 0});
        }

        orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo.minusDays(1)))
                .filter(o -> o.getStatus() == OrderStatus.PAYMENT_COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .forEach(o -> {
                    String date = o.getCreatedAt().toLocalDate().toString();
                    if (dailyStats.containsKey(date)) {
                        double[] stats = dailyStats.get(date);
                        stats[0] += 1;
                        stats[1] += o.getTotalPrice().doubleValue();
                    }
                });

        List<Map<String, Object>> platformSalesTrend = dailyStats.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("date", e.getKey());
                    m.put("orders", (int) e.getValue()[0]);
                    m.put("revenue", Math.round(e.getValue()[1] * 100.0) / 100.0);
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        // 4. Recent Orders (Top 5)
        List<Map<String, Object>> recentOrders = orders.stream()
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() == null) return 1;
                    if (o2.getCreatedAt() == null) return -1;
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                })
                .limit(5)
                .map(o -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", o.getId());
                    m.put("status", o.getStatus().name());
                    m.put("totalPrice", o.getTotalPrice());
                    m.put("date", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
                    m.put("shopName", o.getShop() != null ? o.getShop().getName() : "Unknown");
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        return Map.of(
                "totalPlatformRevenue", totalPlatformRevenue,
                "revenueByShop", revenueByShop,
                "platformSalesTrend", platformSalesTrend,
                "recentOrders", recentOrders
        );
    }
}
