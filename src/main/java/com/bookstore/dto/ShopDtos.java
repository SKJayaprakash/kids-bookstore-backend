package com.bookstore.dto;

public class ShopDtos {

    public static class CreateShopRequest {
        private String shopNumber;
        private String slug;
        private String customDomain;
        private String name;
        private String description;
        private String address;
        private String phoneNumber;
        private String email;
        private String primaryColor;
        private String logoUrl;

        public String getShopNumber() { return shopNumber; }
        public void setShopNumber(String shopNumber) { this.shopNumber = shopNumber; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getCustomDomain() { return customDomain; }
        public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    }

    public static class UpdateShopRequest {
        private String name;
        private String slug;
        private String customDomain;
        private String description;
        private String address;
        private String phoneNumber;
        private String email;
        private Boolean active;
        private String primaryColor;
        private String logoUrl;
        
        private String whatsappAccessToken;
        private String whatsappPhoneNumberId;
        private String whatsappOrderTemplateName;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getCustomDomain() { return customDomain; }
        public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
        public String getWhatsappAccessToken() { return whatsappAccessToken; }
        public void setWhatsappAccessToken(String whatsappAccessToken) { this.whatsappAccessToken = whatsappAccessToken; }
        public String getWhatsappPhoneNumberId() { return whatsappPhoneNumberId; }
        public void setWhatsappPhoneNumberId(String whatsappPhoneNumberId) { this.whatsappPhoneNumberId = whatsappPhoneNumberId; }
        public String getWhatsappOrderTemplateName() { return whatsappOrderTemplateName; }
        public void setWhatsappOrderTemplateName(String whatsappOrderTemplateName) { this.whatsappOrderTemplateName = whatsappOrderTemplateName; }
    }

    public static class ShopResponse {
        private Long id;
        private String shopNumber;
        private String slug;
        private String customDomain;
        private String name;
        private String description;
        private String address;
        private String phoneNumber;
        private String email;
        private boolean active;
        private String ownerEmail;
        private String ownerName;
        private String primaryColor;
        private String logoUrl;
        private String containerStatus;
        private Integer containerPort;
        
        private String whatsappAccessToken;
        private String whatsappPhoneNumberId;
        private String whatsappOrderTemplateName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getShopNumber() { return shopNumber; }
        public void setShopNumber(String shopNumber) { this.shopNumber = shopNumber; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getCustomDomain() { return customDomain; }
        public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public String getOwnerEmail() { return ownerEmail; }
        public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
        public String getContainerStatus() { return containerStatus; }
        public void setContainerStatus(String containerStatus) { this.containerStatus = containerStatus; }
        public Integer getContainerPort() { return containerPort; }
        public void setContainerPort(Integer containerPort) { this.containerPort = containerPort; }
        public String getWhatsappAccessToken() { return whatsappAccessToken; }
        public void setWhatsappAccessToken(String whatsappAccessToken) { this.whatsappAccessToken = whatsappAccessToken; }
        public String getWhatsappPhoneNumberId() { return whatsappPhoneNumberId; }
        public void setWhatsappPhoneNumberId(String whatsappPhoneNumberId) { this.whatsappPhoneNumberId = whatsappPhoneNumberId; }
        public String getWhatsappOrderTemplateName() { return whatsappOrderTemplateName; }
        public void setWhatsappOrderTemplateName(String whatsappOrderTemplateName) { this.whatsappOrderTemplateName = whatsappOrderTemplateName; }
    }

    public static class CreateShopOwnerRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private Long shopId;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
    }
}
