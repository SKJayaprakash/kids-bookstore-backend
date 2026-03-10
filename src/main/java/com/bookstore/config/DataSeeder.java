package com.bookstore.config;

import com.bookstore.entity.Book;
import com.bookstore.entity.Review;
import com.bookstore.entity.Shop;
import com.bookstore.entity.User;
import com.bookstore.enums.AgeGroup;
import com.bookstore.enums.UserRole;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.ReviewRepository;
import com.bookstore.repository.ShopRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.DockerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;
    private final DockerService dockerService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, BookRepository bookRepository, ReviewRepository reviewRepository,
            ShopRepository shopRepository, DockerService dockerService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.shopRepository = shopRepository;
        this.dockerService = dockerService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting DataSeeder...");
        List<Shop> seededShops = seedShops();
        seedUsers(seededShops);
        seedBooks(seededShops);
        seedReviews();
        logger.info("DataSeeder completed.");
    }

    private List<Shop> seedShops() {
        if (shopRepository.count() > 0) {
            logger.info("Shops already seeded. Checking count: " + shopRepository.count());
            return shopRepository.findAll();
        }
        logger.info("Seeding shops and owners...");

        List<Shop> createdShops = new ArrayList<>();

        // Create Shop 1
        User owner1 = new User();
        owner1.setFirstName("Shop1");
        owner1.setLastName("Owner");
        owner1.setEmail("owner1@bookstore.com");
        owner1.setPassword(passwordEncoder.encode("password"));
        owner1.setRoles(new ArrayList<>(List.of(UserRole.SHOP_OWNER)));
        owner1.setProvider("local");
        userRepository.save(owner1);

        Shop shop1 = new Shop();
        shop1.setName("Shop 1");
        shop1.setShopNumber("SH001");
        shop1.setSlug("shop1");
        shop1.setCustomDomain("myshop1.com");
        shop1.setOwner(owner1);
        shop1.setPrimaryColor("#1976d2");
        shop1.setDescription("Welcome to Shop 1 - The best books in town.");
        shop1 = shopRepository.save(shop1);

        // Link owner back to shop for login detection
        owner1.setShop(shop1);
        userRepository.save(owner1);

        createdShops.add(shop1);

        // Provision Container
        logger.info("Provisioning container for shop1...");
        dockerService.provisionShopContainer(shop1);

        // Create Shop 2
        User owner2 = new User();
        owner2.setFirstName("Shop2");
        owner2.setLastName("Owner");
        owner2.setEmail("owner2@bookstore.com");
        owner2.setPassword(passwordEncoder.encode("password"));
        owner2.setRoles(new ArrayList<>(List.of(UserRole.SHOP_OWNER)));
        owner2.setProvider("local");
        userRepository.save(owner2);

        Shop shop2 = new Shop();
        shop2.setName("Shop 2");
        shop2.setShopNumber("SH002");
        shop2.setSlug("shop2");
        shop2.setCustomDomain("myshop2.com");
        shop2.setOwner(owner2);
        shop2.setPrimaryColor("#9c27b0");
        shop2.setDescription("Welcome to Shop 2 - Your reading adventure awaits.");
        shop2 = shopRepository.save(shop2);

        // Link owner back to shop for login detection
        owner2.setShop(shop2);
        userRepository.save(owner2);

        createdShops.add(shop2);

        // Provision Container
        logger.info("Provisioning container for shop2...");
        dockerService.provisionShopContainer(shop2);

        logger.info("Seeding shops finished.");
        return createdShops;
    }

    private void seedUsers(List<Shop> shops) {
        if (userRepository.count() > 3) { // more than just superadmin + 2 owners
            logger.info("Users already seeded. Checking count: " + userRepository.count());
            return;
        }
        logger.info("Seeding users...");

        // Super Admin User (for platform administration)
        if (userRepository.findByEmail("admin@bookstore.com").isEmpty()) {
            User superAdmin = new User();
            superAdmin.setFirstName("Super");
            superAdmin.setLastName("Admin");
            superAdmin.setEmail("admin@bookstore.com");
            superAdmin.setPassword(passwordEncoder.encode("admin123"));
            superAdmin.setRoles(new ArrayList<>(List.of(UserRole.SUPER_ADMIN)));
            superAdmin.setProvider("local");
            userRepository.save(superAdmin);
        }

        // Regular Users assigned to shops
        if (!shops.isEmpty()) {
            IntStream.range(0, 20).forEach(i -> {
                User user = new User();
                user.setFirstName("User" + i);
                user.setLastName("Test");
                user.setEmail("user" + i + "@example.com");
                user.setPassword(passwordEncoder.encode("password"));
                user.setRoles(new ArrayList<>(List.of(UserRole.CUSTOMER)));
                user.setProvider("local");

                // Assign alternate users to shop 1 and shop 2
                Shop assignedShop = shops.get(i % shops.size());
                user.setShop(assignedShop);

                userRepository.save(user);
            });
        }
        logger.info("Seeding users finished.");
    }

    private void seedBooks(List<Shop> shops) {
        List<String> imageUrls = Arrays.asList(
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1629992160506-ebf9d0e4e5aa?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1476275466078-4007374efbbe?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1519682337058-a94d519337bc?auto=format&fit=crop&q=80&w=800");

        Random random = new Random();
        AgeGroup[] ageGroups = AgeGroup.values();

        if (bookRepository.count() > 0) {
            logger.info("Books already seeded.");
            return;
        }

        if (shops.isEmpty()) {
            logger.warn("No shops available to assign books to.");
            return;
        }

        logger.info("Seeding new books...");
        String[] categories = { "Fiction", "Adventure", "Sci-Fi", "Fantasy", "Mystery", "History", "Science" };
        String[] titles = {
                "Harry Potter and the Philosopher's Stone", "The Hobbit", "Percy Jackson",
                "Charlotte's Web", "Matilda", "The Lion, the Witch and the Wardrobe",
                "Diary of a Wimpy Kid", "Wonder", "The Giver", "Holes"
        };

        IntStream.range(0, 50).forEach(i -> {
            Book book = new Book();
            String category = categories[random.nextInt(categories.length)];
            String titleBase = titles[random.nextInt(titles.length)];

            book.setTitle(titleBase + " - Vol " + (i + 1));
            book.setAuthor("Author " + (i + 1));
            book.setPrice(BigDecimal.valueOf(10.0 + (random.nextDouble() * 90.0)));
            book.setStock(random.nextInt(50) + 1);
            book.setCategory(category);
            book.setDescription("This is a fantastic book for children who love " + category + ".");
            book.setImageUrl(imageUrls.get(random.nextInt(imageUrls.size())));
            book.setAgeGroup(ageGroups[random.nextInt(ageGroups.length)]);

            List<String> additional = new ArrayList<>();
            for (int k = 0; k < 3; k++) {
                additional.add(imageUrls.get(random.nextInt(imageUrls.size())));
            }
            book.setAdditionalImages(additional);

            // Assign books alternately to the shops
            Shop assignedShop = shops.get(i % shops.size());
            book.setShop(assignedShop);

            bookRepository.save(book);
        });
        logger.info("Seeding books finished.");
    }

    private void seedReviews() {
        if (reviewRepository.count() > 0) {
            logger.info("Reviews already seeded.");
            return;
        }

        logger.info("Seeding reviews...");
        List<Book> books = bookRepository.findAll();
        List<User> users = userRepository.findAll(); // Includes customers and admins
        List<User> customers = users.stream()
                .filter(u -> u.getRoles().contains(UserRole.CUSTOMER))
                .toList();

        if (customers.isEmpty()) {
            logger.warn("No customers available to write reviews.");
            return;
        }

        Random random = new Random();
        String[] comments = {
                "Great book! My kids loved it.",
                "Amazing story and beautiful illustrations.",
                "Good read but shipping was slow.",
                "Highly recommended for early readers.",
                "The content is appropriate for the age group."
        };

        for (Book book : books) {
            // Find customers that belong to the SAME SHOP as the book
            List<User> shopCustomers = customers.stream()
                    .filter(c -> c.getShop() != null && c.getShop().getId().equals(book.getShop().getId()))
                    .toList();

            if (shopCustomers.isEmpty())
                continue;

            // Add 0 to 5 reviews per book
            int reviewCount = random.nextInt(6);
            for (int i = 0; i < reviewCount; i++) {
                Review review = new Review();
                review.setBook(book);
                review.setUser(shopCustomers.get(random.nextInt(shopCustomers.size())));
                review.setRating(random.nextInt(3) + 3); // 3 to 5 stars
                review.setComment(comments[random.nextInt(comments.length)]);
                reviewRepository.save(review);
            }
        }
        logger.info("Seeding reviews finished.");
    }
}
