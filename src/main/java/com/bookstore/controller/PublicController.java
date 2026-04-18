package com.bookstore.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private AmazonS3 s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Legacy query-param proxy (kept for backward compatibility).
     * Example: /api/public/images/proxy?key=shops/shop1/uuid_name.png
     */
    @GetMapping("/images/proxy")
    public ResponseEntity<InputStreamResource> proxyS3Image(@RequestParam("key") String key) {
        return fetchAndServeS3Image(key);
    }

    /**
     * Path-based proxy endpoint for Instagram/Meta compatibility.
     * Avoids double-encoding issues with query parameters.
     * Example: /api/public/images/s3/shops/shop1/uuid_name.png
     */
    @GetMapping("/images/s3/**")
    public ResponseEntity<InputStreamResource> proxyS3ImageByPath(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String prefix = "/api/public/images/s3/";
        String key = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
        
        System.out.println("DIAG PROXY - Path-based request for S3 key: " + key);
        return fetchAndServeS3Image(key);
    }

    private ResponseEntity<InputStreamResource> fetchAndServeS3Image(String key) {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, key);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();
            
            String contentType = s3Object.getObjectMetadata().getContentType();
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            System.err.println("PROXY ERROR: Failed to fetch key '" + key + "' from S3. " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
