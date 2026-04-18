package com.bookstore.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileStorageService {

    @Autowired
    private AmazonS3 s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String storeFile(MultipartFile file) {
        try {
            String shopPrefix = "global/";
            com.bookstore.entity.Shop shop = com.bookstore.context.ShopContext.getCurrentShop();
            if (shop != null) {
                shopPrefix = "shops/" + shop.getSlug() + "/";
            }
            
            String cleanOriginalName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("\\s+", "_") : "image.jpg";
            String fileName = shopPrefix + UUID.randomUUID().toString() + "_" + cleanOriginalName;
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            try {
                s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata)
                    .withCannedAcl(com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead));
            } catch (Exception e) {
                System.out.println("S3 ERROR: ACL not supported or blocked. Uploading as private fallback. Error: " + e.getMessage());
                // FALLBACK: If ACLs are disabled on the bucket, upload without ACL
                s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
            }

            // Return the public AWS URL (will be stored in DB and returned instantly to frontend)
            return s3Client.getUrl(bucketName, fileName).toString();
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + " directly to S3. Please try again!", ex);
        }
    }

    @Autowired
    private com.bookstore.repository.BookRepository bookRepository;

    @Value("${app.upload.dir:/uploads}")
    private String uploadDir;

    @jakarta.annotation.PostConstruct
    public void autoRunMigration() {
        System.out.println("Running auto-migration to AWS S3...");
        java.util.Map<String, Object> result = migrateLocalImagesToS3();
        System.out.println("AWS S3 MIGRATION RESULTS: " + result);
    }

    public java.util.Map<String, Object> migrateLocalImagesToS3() {
        java.util.List<com.bookstore.entity.Book> books = bookRepository.findAll();
        int migrated = 0;
        int failed = 0;
        int skipped = 0;
        
        for (com.bookstore.entity.Book book : books) {
            if (book.getImageUrl() != null && book.getImageUrl().startsWith("/uploads/")) {
                try {
                    String fileName = book.getImageUrl().substring("/uploads/".length());
                    java.nio.file.Path localFilePath = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);
                    java.io.File file = localFilePath.toFile();
                    
                    if (!file.exists()) {
                        failed++;
                        continue;
                    }
                    
                    String shopPrefix = "global/";
                    if (book.getShop() != null) {
                        shopPrefix = "shops/" + book.getShop().getSlug() + "/";
                    }
                    String s3Key = shopPrefix + fileName;

                    try {
                        s3Client.putObject(new PutObjectRequest(bucketName, s3Key, file)
                            .withCannedAcl(com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead));
                    } catch (Exception e) {
                        System.out.println("S3 MIGRATION ERROR: ACL not supported for " + s3Key + ". Error: " + e.getMessage());
                        s3Client.putObject(new PutObjectRequest(bucketName, s3Key, file));
                    }
                    book.setImageUrl(s3Client.getUrl(bucketName, s3Key).toString());
                    bookRepository.save(book);
                    migrated++;
                } catch (Exception e) {
                    failed++;
                }
            } else {
                skipped++;
            }
        }
        return java.util.Map.of("migrated", migrated, "failed", failed, "skippedUnhookedOrUnsplash", skipped);
    }

    public java.util.Map<String, Object> repairS3Acls() {
        java.util.List<com.bookstore.entity.Book> books = bookRepository.findAll();
        int repaired = 0;
        int skipped = 0;
        int failed = 0;

        for (com.bookstore.entity.Book book : books) {
            if (book.getImageUrl() != null && book.getImageUrl().contains("amazonaws.com")) {
                try {
                    // Extract s3Key from URL
                    // Example URL: https://bucket.s3.region.amazonaws.com/shops/shop1/uuid_name.png
                    String url = book.getImageUrl();
                    String s3Key = url.substring(url.indexOf(".com/") + 5);
                    
                    try {
                        s3Client.setObjectAcl(bucketName, s3Key, com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead);
                    } catch (Exception e) {
                        System.out.println("S3 REPAIR ERROR: ACL not supported for " + s3Key + ". Error: " + e.getMessage());
                    }
                    repaired++;
                } catch (Exception e) {
                    failed++;
                }
            } else {
                skipped++;
            }
        }
        return java.util.Map.of("repaired", repaired, "failed", failed, "skippedNonS3", skipped);
    }
}
