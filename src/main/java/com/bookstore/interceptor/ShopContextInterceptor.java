package com.bookstore.interceptor;

import com.bookstore.context.ShopContext;
import com.bookstore.entity.Shop;
import com.bookstore.service.ShopDetectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ShopContextInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ShopContextInterceptor.class);

    @Autowired
    private ShopDetectionService shopDetectionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        Shop shop = shopDetectionService.detectShopFromRequest(request);

        if (shop != null) {
            logger.debug("Detected shop {} from request", shop.getId());
            ShopContext.setCurrentShop(shop);
        }

        // If no shop resolved from domain, try resolving from authenticated user (e.g.,
        // Shop Owner on localhost)
        if (ShopContext.getCurrentShop() == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                String email = auth.getName();
                logger.debug("Resolving shop for authenticated user: {}", email);
                com.bookstore.repository.UserRepository userRepository = org.springframework.web.context.support.WebApplicationContextUtils
                        .getRequiredWebApplicationContext(request.getServletContext())
                        .getBean(com.bookstore.repository.UserRepository.class);

                userRepository.findByEmail(email).ifPresent(user -> {
                    if (user.getShop() != null) {
                        logger.debug("Mapped authenticated user {} to Shop ID: {}", email, user.getShop().getId());
                        ShopContext.setCurrentShop(user.getShop());
                    }
                });
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        ShopContext.clear();
    }
}
