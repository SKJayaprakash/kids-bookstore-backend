package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.entity.Shop;
import com.bookstore.repository.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.net.URI;

@Service
public class InstagramService {

    private static final Logger logger = LoggerFactory.getLogger(InstagramService.class);
    private static final String GRAPH_FB_BASE = "https://graph.facebook.com/v21.0";
    private static final String OAUTH_BASE = "https://www.facebook.com/v21.0/dialog/oauth";

    @Value("${instagram.redirect.uri:http://localhost:5176/instagram/callback}")
    private String redirectUri;

    private final ShopRepository shopRepository;

    @Value("${app.public.url}")
    private String publicApiUrl;


    @Autowired
    private RestTemplate restTemplate;

    public InstagramService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Check if a shop has Instagram app credentials configured
     */
    public boolean isConfigured(Shop shop) {
        return shop != null
                && shop.getInstagramAppId() != null && !shop.getInstagramAppId().isEmpty()
                && shop.getInstagramAppSecret() != null && !shop.getInstagramAppSecret().isEmpty();
    }

    /**
     * Save Instagram app credentials for a shop
     */
    @org.springframework.transaction.annotation.Transactional
    public void saveAppCredentials(Shop shop, String appId, String appSecret) {
        System.out.println("DIAG - Service: Starting save for shop ID: " + shop.getId());
        
        // Re-fetch shop by ID to ensure it's a managed entity in the current transaction
        Shop managedShop = shopRepository.findById(shop.getId())
                .orElseThrow(() -> new RuntimeException("Shop not found for ID: " + shop.getId()));

        managedShop.setInstagramAppId(appId != null ? appId.trim() : null);
        managedShop.setInstagramAppSecret(appSecret != null ? appSecret.trim() : null);
        
        // Use saveAndFlush to force the update to the database immediately
        shopRepository.saveAndFlush(managedShop);
        
        System.out.println("DIAG - Service: SAVED AND FLUSHED. New AppID (truncated): " + 
            (managedShop.getInstagramAppId() != null && managedShop.getInstagramAppId().length() > 4 ? managedShop.getInstagramAppId().substring(0, 4) : "N/A"));
    }

    /**
     * Build the Meta OAuth authorization URL using the shop's own credentials.
     * Uses Facebook Login flow to access linked Instagram Business accounts.
     */
    public String getAuthorizationUrl(Shop shop) {
        String appId = shop.getInstagramAppId();
        logger.info("DIAG - Service: Generating Auth URL with App ID: {} (Shop: {})", 
            (appId != null && appId.length() > 4 ? appId.substring(0, 4) : "N/A"),
            shop.getName());
        
        if (appId == null || appId.isEmpty()) {
            throw new RuntimeException("Instagram App ID not configured for this shop");
        }

        return UriComponentsBuilder.fromHttpUrl(OAUTH_BASE)
                .queryParam("client_id", appId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "instagram_basic,instagram_content_publish,pages_show_list,pages_read_engagement,business_management,public_profile")
                .queryParam("response_type", "code")
                .queryParam("state", shop.getId().toString())
                .toUriString();
    }

    /**
     * Exchange authorization code for access token, then resolve the Instagram Business account ID
     */
    @SuppressWarnings("unchecked")
    public void handleOAuthCallback(String code, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        String appId = shop.getInstagramAppId();
        String appSecret = shop.getInstagramAppSecret();

        logger.info("DIAG - Using App ID: {}... and Secret: {}...", 
            appId != null && appId.length() > 4 ? appId.substring(0, 4) : "N/A",
            appSecret != null && appSecret.length() > 4 ? appSecret.substring(0, 4) : "N/A");

        // Step 1: Exchange code for User Access Token
        logger.info("DIAG - Step 1: Exchanging code for user access token...");
        URI tokenUri = UriComponentsBuilder.fromHttpUrl(GRAPH_FB_BASE + "/oauth/access_token")
                .queryParam("client_id", appId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_secret", appSecret)
                .queryParam("code", code)
                .build()
                .toUri();

        ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.getForEntity(tokenUri,
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        Map<String, Object> tokenBody = tokenResponse.getBody();

        if (tokenBody == null || !tokenBody.containsKey("access_token")) {
            logger.error("DIAG ERROR - Step 1 failed. tokenBody: {}", tokenBody);
            throw new RuntimeException("Failed to obtain Facebook access token");
        }

        String userAccessToken = (String) tokenBody.get("access_token");
        logger.info("DIAG - Step 1 success. Obtained user access token.");

        // Step 2: Exchange for long-lived token (valid for 60 days)
        logger.info("DIAG - Step 2: Exchanging for long-lived token...");
        URI longLivedUri = UriComponentsBuilder.fromHttpUrl(GRAPH_FB_BASE + "/oauth/access_token")
                .queryParam("grant_type", "fb_exchange_token")
                .queryParam("client_id", appId)
                .queryParam("client_secret", appSecret)
                .queryParam("fb_exchange_token", userAccessToken)
                .build()
                .toUri();

        ResponseEntity<Map<String, Object>> longLivedResponse = restTemplate.getForEntity(longLivedUri,
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        Map<String, Object> longLivedBody = longLivedResponse.getBody();

        String finalAccessToken = longLivedBody != null
                ? (String) longLivedBody.get("access_token")
                : userAccessToken;
        logger.info("DIAG - Step 2 success. Long-lived token obtained.");

        // Step 2.5: Introspect Permissions
        try {
            logger.info("DIAG - Step 2.5: Inspecting token permissions...");
            URI permUri = UriComponentsBuilder.fromHttpUrl(GRAPH_FB_BASE + "/me/permissions")
                    .queryParam("access_token", finalAccessToken)
                    .build()
                    .toUri();
            ResponseEntity<Map<String, Object>> permResponse = restTemplate.getForEntity(permUri,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            logger.info("DIAG RAW - TOKEN PERMISSIONS: {}", permResponse.getBody());
        } catch (Exception e) {
            logger.error("DIAG ERROR - Failed to fetch permissions: {}", e.getMessage());
        }

        // Step 3: Find the Instagram Business Account linked to the user's pages
        logger.info("DIAG - Step 3: Looking up user's Facebook pages...");
        URI pagesUri = UriComponentsBuilder.fromHttpUrl(GRAPH_FB_BASE + "/me/accounts")
                .queryParam("fields", "name,id")
                .queryParam("access_token", finalAccessToken)
                .build()
                .toUri();

        ResponseEntity<Map<String, Object>> pagesResponse = restTemplate.getForEntity(pagesUri,
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        Map<String, Object> pagesBody = pagesResponse.getBody();
        logger.info("DIAG RAW - Pages Body: {}", pagesBody);

        if (pagesBody == null || !pagesBody.containsKey("data")) {
            logger.error("DIAG ERROR - Step 3 failed to find pages data. pagesBody: {}", pagesBody);
            throw new RuntimeException("Could not find any Facebook pages linked to your account. Ensure you selected your pages in the Facebook popup.");
        }

        java.util.List<Map<String, Object>> pages = (java.util.List<Map<String, Object>>) pagesBody.get("data");
        logger.info("DIAG - Found {} pages. Checking each for a linked Instagram account...", pages.size());
        Map<String, Object> igAccount = null;

        for (Map<String, Object> page : pages) {
            String pageId = (String) page.get("id");
            logger.info("DIAG - Checking page: {} ({})", page.get("name"), pageId);
            
            try {
                URI pageIgUri = UriComponentsBuilder.fromHttpUrl(GRAPH_FB_BASE + "/" + pageId)
                        .queryParam("fields", "instagram_business_account")
                        .queryParam("access_token", finalAccessToken)
                        .build()
                        .toUri();

                ResponseEntity<Map<String, Object>> pageIgResponse = restTemplate.getForEntity(pageIgUri,
                        (Class<Map<String, Object>>) (Class<?>) Map.class);
                Map<String, Object> pageIgBody = pageIgResponse.getBody();

                if (pageIgBody != null && pageIgBody.containsKey("instagram_business_account")) {
                    igAccount = (Map<String, Object>) pageIgBody.get("instagram_business_account");
                    logger.info("DIAG - Found linked Instagram Account ID: {}", igAccount.get("id"));
                    break;
                }
            } catch (Exception e) {
                logger.info("DIAG - No IG account on this page or error: {}", e.getMessage());
            }
        }

        if (igAccount == null) {
            logger.error("DIAG ERROR - Step 3 failed. No linked IG account discovered after checking all pages.");
            throw new RuntimeException("No Instagram Business Account linked to your Facebook pages found. Please ensure your account is connected to a Facebook Page.");
        }

        String igUserId = (String) igAccount.get("id");
        
        // Step 3.5: Get the username directly from the IG User ID
        logger.info("DIAG - Step 3.5: Fetching Instagram username for ID: {}", igUserId);
        URI igUserUri = UriComponentsBuilder.fromHttpUrl(GRAPH_FB_BASE + "/" + igUserId)
                .queryParam("fields", "username")
                .queryParam("access_token", finalAccessToken)
                .build()
                .toUri();

        ResponseEntity<Map<String, Object>> igUserResponse = restTemplate.getForEntity(igUserUri,
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        Map<String, Object> igUserBody = igUserResponse.getBody();
        String username = igUserBody != null ? (String) igUserBody.get("username") : "instagram_user";
        logger.info("DIAG - Successfully resolved username: {}", username);

        // Step 4: Store credentials in Shop entity
        shop.setInstagramAccessToken(finalAccessToken);
        shop.setInstagramUserId(igUserId);
        shop.setInstagramUsername(username);
        shop.setInstagramTokenExpiry(LocalDateTime.now().plusDays(60));
        shopRepository.save(shop);

        logger.info("Instagram (via Facebook) connected for shop {} as @{}", shop.getName(), username);
        logger.info("DIAG - Step 4 success. Instagram connected successfully.");
    }



    /**
     * Get connection status for a shop
     */
    public Map<String, Object> getConnectionStatus(Shop shop) {
        boolean configured = isConfigured(shop);
        boolean connected = configured
                && shop.getInstagramAccessToken() != null
                && !shop.getInstagramAccessToken().isEmpty()
                && (shop.getInstagramTokenExpiry() == null
                        || shop.getInstagramTokenExpiry().isAfter(LocalDateTime.now()));

        if (connected) {
            return Map.of(
                    "configured", true,
                    "connected", true,
                    "username", shop.getInstagramUsername() != null ? shop.getInstagramUsername() : "",
                    "tokenExpiry", shop.getInstagramTokenExpiry() != null
                            ? shop.getInstagramTokenExpiry().toString() : ""
            );
        }
        return Map.of("configured", configured, "connected", false);
    }

    /**
     * Publish a book as an Instagram post (two-step container-based publishing)
     */
    public Map<String, Object> publishBookPost(Book book, String customCaption) {
        Shop shop = book.getShop();
        if (shop == null || shop.getInstagramAccessToken() == null) {
            throw new RuntimeException("Instagram not connected for this shop");
        }

        String accessToken = shop.getInstagramAccessToken();
        String igUserId = shop.getInstagramUserId();

        // Build the caption (use custom if provided, otherwise default)
        String caption = (customCaption != null && !customCaption.trim().isEmpty()) 
                ? customCaption 
                : buildCaption(book, shop);

        // Give Meta the direct S3 HTTPS URL.
        // We cannot use our EC2 proxy because it is HTTP (port 8081), and Meta strictly requires HTTPS.
        // We cannot use pre-signed URLs because Meta mangles the signature query parameters.
        // Since FileStorageService uploads with PublicRead ACL, the direct HTTPS URL should work perfectly!
        // Convert S3 URL to Path-Style to bypass Meta's strict SSL/DNS wildcard limitations.
        // Virtual-Hosted Style (Blocked): https://bucket.s3.region.amazonaws.com/key
        // Path Style (Allowed): https://s3.region.amazonaws.com/bucket/key
        String imageUrl = book.getImageUrl();
        if (imageUrl != null && imageUrl.contains(".s3.") && imageUrl.startsWith("https://")) {
            try {
                String withoutProtocol = imageUrl.substring(8);
                String bucketName = withoutProtocol.substring(0, withoutProtocol.indexOf(".s3."));
                String rest = withoutProtocol.substring(withoutProtocol.indexOf(".s3.") + 1);
                imageUrl = "https://" + rest.replaceFirst("/", "/" + bucketName + "/");
                logger.info("DIAG - Transformed Image URL for Meta (S3 Path-Style): {}", imageUrl);
            } catch (Exception e) {
                logger.error("DIAG ERROR - Failed to convert S3 URL to Path-Style: {}", e.getMessage());
            }
        }

        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new RuntimeException("Book must have an image to post to Instagram");
        }

        // Step 1: Create media container
        // We strictly use LinkedMultiValueMap so RestTemplate sends the request as 
        // application/x-www-form-urlencoded. FB Graph API has known bugs parsing JSON image_url.
        String createUrl = GRAPH_FB_BASE + "/" + igUserId + "/media";
        org.springframework.util.MultiValueMap<String, String> createPayload = new org.springframework.util.LinkedMultiValueMap<>();
        createPayload.add("image_url", imageUrl);
        createPayload.add("caption", caption);
        createPayload.add("access_token", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<org.springframework.util.MultiValueMap<String, String>> createRequest = new HttpEntity<>(createPayload, headers);

        logger.info("DIAG - Attempting to create IG media container. URL: {}, Image: {}", createUrl, imageUrl);
        
        String creationId;
        try {
            ResponseEntity<Map<String, Object>> createResponse = restTemplate.exchange(
                    createUrl, HttpMethod.POST, createRequest,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> createBody = createResponse.getBody();

            if (createBody == null || !createBody.containsKey("id")) {
                logger.error("DIAG ERROR - Failed to create IG media container. Response: {}", createBody);
                throw new RuntimeException("Failed to create Instagram media container");
            }
            creationId = (String) createBody.get("id");
            logger.info("DIAG - IG media container created with ID: {}", creationId);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("DIAG ERROR - HTTP Failure during IG container creation. Status: {}, Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Instagram API Error: " + e.getResponseBodyAsString());
        }

        // Step 2: Publish the container
        String publishUrl = GRAPH_FB_BASE + "/" + igUserId + "/media_publish";
        org.springframework.util.MultiValueMap<String, String> publishPayload = new org.springframework.util.LinkedMultiValueMap<>();
        publishPayload.add("creation_id", creationId);
        publishPayload.add("access_token", accessToken);
        
        HttpEntity<org.springframework.util.MultiValueMap<String, String>> publishRequest = new HttpEntity<>(publishPayload, headers);

        logger.info("DIAG - Attempting to publish IG container: {}", creationId);

        try {
            ResponseEntity<Map<String, Object>> publishResponse = restTemplate.exchange(
                    publishUrl, HttpMethod.POST, publishRequest,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> publishBody = publishResponse.getBody();

            if (publishBody == null || !publishBody.containsKey("id")) {
                logger.error("DIAG ERROR - Failed to publish IG post. Response: {}", publishBody);
                throw new RuntimeException("Failed to publish Instagram post");
            }

            String mediaId = (String) publishBody.get("id");
            logger.info("DIAG SUCCESS - Instagram post published! Media ID: {}", mediaId);

            return Map.of(
                    "success", true,
                    "mediaId", mediaId,
                    "message", "Posted to Instagram successfully!"
            );
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("DIAG ERROR - Meta rejected the publish request. Status: {}, Response: {}", 
                e.getStatusCode(), errorBody);
            
            String friendlyError = "Instagram Error: ";
            if (errorBody.contains("The image could not be loaded")) {
                friendlyError += "Instagram couldn't download your image. Please check if port 8081 is open on your EC2 and your Public URL is correct.";
            } else if (errorBody.contains("The business is restricted")) {
                friendlyError += "Your Facebook/Instagram account has restrictions. Check your Meta Account Quality page.";
            } else if (errorBody.contains("access token")) {
                friendlyError += "Connection expired. Please disconnect and reconnect Instagram.";
            } else {
                friendlyError += errorBody;
            }
            
            throw new RuntimeException(friendlyError);
        } catch (Exception e) {
            logger.error("DIAG ERROR - Unexpected failure during IG publish: {}", e.getMessage());
            throw new RuntimeException("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Build an engaging caption for the Instagram post
     */
    private String buildCaption(Book book, Shop shop) {
        StringBuilder caption = new StringBuilder();
        caption.append("📚 New Arrival! ✨\n\n");
        caption.append("📖 ").append(book.getTitle()).append("\n");
        caption.append("✍️ by ").append(book.getAuthor()).append("\n\n");

        if (book.getDescription() != null && !book.getDescription().isEmpty()) {
            String desc = book.getDescription();
            if (desc.length() > 200) {
                desc = desc.substring(0, 200) + "...";
            }
            caption.append(desc).append("\n\n");
        }

        caption.append("💰 ₹").append(book.getPrice()).append("\n");
        caption.append("📂 ").append(book.getCategory()).append("\n\n");
        caption.append("🛒 Available now at ").append(shop.getName()).append("!\n\n");
        caption.append("#KidsBooks #ChildrensBooks #BookStore #NewArrival #ReadingIsFun");

        return caption.toString();
    }

    /**
     * Disconnect Instagram from a shop (keeps app credentials)
     */
    public void disconnectAccount(Shop shop) {
        shop.setInstagramAccessToken(null);
        shop.setInstagramUserId(null);
        shop.setInstagramUsername(null);
        shop.setInstagramTokenExpiry(null);
        shopRepository.save(shop);
        logger.info("Instagram disconnected for shop: {}", shop.getName());
    }

    /**
     * Diagnostic method to test publishing with a known public, valid image.
     * This helps isolate if the issue is with the shop's S3 bucket or the Meta App permissions.
     */
    public Map<String, Object> testPublish(Shop shop) {
        if (shop.getInstagramAccessToken() == null) {
            throw new RuntimeException("Instagram not connected");
        }

        // Use a verified, public, high-quality image from Unsplash
        String testImageUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=1000&auto=format&fit=crop";
        
        String accessToken = shop.getInstagramAccessToken();
        String igUserId = shop.getInstagramUserId();
        String caption = "🚀 Instagram Integration Test für " + shop.getName() + "\n\nConnection verified! If you can see this, the API works perfectly. #IntegrationSuccess #KidsBookstore";

        // Step 1: Create media container
        String createUrl = GRAPH_FB_BASE + "/" + igUserId + "/media";
        Map<String, String> createPayload = Map.of(
                "image_url", testImageUrl,
                "caption", caption,
                "access_token", accessToken
        );

        logger.info("DIAG - RUNNING TEST PUBLISH. Image: {}", testImageUrl);

        try {
            ResponseEntity<Map<String, Object>> createResponse = restTemplate.exchange(
                    createUrl, HttpMethod.POST, new HttpEntity<>(createPayload),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> createBody = createResponse.getBody();
            String creationId = (String) createBody.get("id");

            // Step 2: Publish
            String publishUrl = GRAPH_FB_BASE + "/" + igUserId + "/media_publish";
            Map<String, String> publishPayload = Map.of(
                    "creation_id", creationId,
                    "access_token", accessToken
            );

            ResponseEntity<Map<String, Object>> publishResponse = restTemplate.exchange(
                    publishUrl, HttpMethod.POST, new HttpEntity<>(publishPayload),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            String mediaId = (String) publishResponse.getBody().get("id");
            logger.info("DIAG SUCCESS - Test publish successful! Media ID: {}", mediaId);

            return Map.of("success", true, "mediaId", mediaId, "message", "Test post successful! Check your Instagram feed.");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("DIAG ERROR - Test publish failed. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Instagram Test Failed: " + e.getResponseBodyAsString());
        }
    }
}
