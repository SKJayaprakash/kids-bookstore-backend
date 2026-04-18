package com.bookstore.task;

import com.bookstore.entity.Book;
import com.bookstore.entity.Shop;
import com.bookstore.repository.ShopRepository;
import com.bookstore.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StockMonitorTask {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private EmailService emailService;

    // Run every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void monitorStockLevels() {
        System.out.println("Starting daily stock monitoring task...");
        List<Shop> allShops = shopRepository.findAll();
        
        for (Shop shop : allShops) {
            if (shop.getOwner() == null || shop.getOwner().getEmail() == null) {
                continue;
            }

            List<Book> lowStockBooks = shop.getBooks().stream()
                    .filter(book -> book.getStock() != null && book.getStock() < 5)
                    .collect(Collectors.toList());

            if (!lowStockBooks.isEmpty()) {
                System.out.println("Low stock detected for shop: " + shop.getName() + ". Sending alert to: " + shop.getOwner().getEmail());
                emailService.sendLowStockAlert(shop.getOwner().getEmail(), lowStockBooks);
            }
        }
        System.out.println("Daily stock monitoring task completed.");
    }

    // Optional: Run on startup or every 4 hours for debugging?
    // @Scheduled(fixedDelay = 14400000) 
    public void checkNow() {
        monitorStockLevels();
    }
}
