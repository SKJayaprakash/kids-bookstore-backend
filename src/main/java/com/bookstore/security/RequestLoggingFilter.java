package com.bookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();

        // Only log /api/ endpoints to avoid cluttering logs
        if (uri.startsWith("/api/")) {
            logger.info("EXTREME DIAG - Request: {} {} from {}", method, uri, remoteAddr);
        }

        try {
            filterChain.doFilter(request, response);
            
            if (uri.startsWith("/api/")) {
                logger.info("EXTREME DIAG - Response: {} {} Status: {}", method, uri, response.getStatus());
            }
        } catch (Exception e) {
            if (uri.startsWith("/api/")) {
                logger.error("EXTREME DIAG - Error: {} {} Message: {}", method, uri, e.getMessage());
            }
            throw e;
        }
    }
}
