package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.entity.Order;
import com.bookstore.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.from:noreply@bookstore.com}")
    private String fromEmail;

    @Async
    public void sendOrderReceipt(Order order) {
        try {
            Context context = new Context();
            context.setVariable("order", order);
            String process = templateEngine.process("email/order-receipt", context);
            
            sendHtmlMessage(order.getUser().getEmail(), "Order Confirmation - #" + order.getId(), process);
        } catch (Exception e) {
            System.err.println("Failed to send order receipt: " + e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            Context context = new Context();
            context.setVariable("user", user);
            String process = templateEngine.process("email/welcome-email", context);
            
            sendHtmlMessage(user.getEmail(), "Welcome to Kids Bookstore!", process);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }

    @Async
    public void sendLowStockAlert(String ownerEmail, List<Book> lowStockBooks) {
        try {
            Context context = new Context();
            context.setVariable("books", lowStockBooks);
            String process = templateEngine.process("email/low-stock-alert", context);
            
            sendHtmlMessage(ownerEmail, "Action Required: Low Stock Alert", process);
        } catch (Exception e) {
            System.err.println("Failed to send low stock alert: " + e.getMessage());
        }
    }

    private void sendHtmlMessage(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }
}
