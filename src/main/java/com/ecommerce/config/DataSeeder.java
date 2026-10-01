package com.ecommerce.config;

import com.ecommerce.model.Cart;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the admin account and a sample Ashgrove catalog on first start.
 * Safe to run repeatedly: it only adds data that is missing.
 * Prices are in NPR.
 */
@Slf4j
@Component
@Profile("!test")
@EnableConfigurationProperties(AdminProperties.class)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedCatalog();
    }

    private void seedAdmin() {
        String email = adminProperties.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = userRepository.save(User.builder()
                .fullName("Store Administrator")
                .email(email)
                .password(passwordEncoder.encode(adminProperties.password()))
                .role(Role.ADMIN)
                .build());
        cartRepository.save(Cart.builder().user(admin).build());
        log.info("Created admin user {}", email);
    }

    private void seedCatalog() {
        if (categoryRepository.count() > 0 || productRepository.count() > 0) {
            log.info("Catalog already contains data, skipping seed");
            return;
        }

        Category dining = category("Dining Tables",
                "Solid hardwood dining tables built to host the whole family.");
        Category casework = category("Case Furniture",
                "Dressers, bookcases and nightstands with hand-cut joinery.");
        Category seating = category("Chairs & Benches",
                "Chairs and benches shaped for comfort and long life.");
        Category builtIns = category("Built-Ins & Shelving",
                "Wall-mounted shelving and cabinetry for fitted storage.");
        Category smallGoods = category("Small Goods",
                "Kitchen and tabletop pieces made from workshop offcuts.");

        productRepository.saveAll(List.of(
                product("Live-Edge Walnut Dining Table",
                        "Seats eight. A single slab of black walnut with a natural live edge and a hand-rubbed oil finish.",
                        "185000.00", 3, "Black Walnut", dining),
                product("Round Oak Pedestal Table",
                        "A 120 cm round table on a turned pedestal base, finished with a hard-wearing wax oil.",
                        "142000.00", 4, "White Oak", dining),
                product("Maple Farmhouse Table",
                        "A sturdy trestle-style table with breadboard ends that keep the top flat through every season.",
                        "128000.00", 5, "Hard Maple", dining),
                product("Six-Drawer Cherry Dresser",
                        "Dovetailed drawers on solid wood runners. Cherry darkens beautifully with age.",
                        "96000.00", 4, "Cherry", casework),
                product("Maple Bookcase",
                        "A tall bookcase with adjustable shelves and a solid back panel.",
                        "64500.00", 6, "Hard Maple", casework),
                product("Oak Nightstand",
                        "One drawer and one open shelf, sized to sit neatly beside a standard bed.",
                        "32000.00", 8, "White Oak", casework),
                product("Windsor Dining Chair",
                        "A classic spindle-back chair with a saddle-shaped seat for all-day comfort.",
                        "18500.00", 12, "Ash", seating),
                product("Entryway Storage Bench",
                        "A lift-top bench with hidden storage for shoes, bags and winter gear.",
                        "24000.00", 7, "White Oak", seating),
                product("Walnut Rocking Chair",
                        "A sculpted rocker with a smooth, balanced motion and gently curved arms.",
                        "41000.00", 5, "Black Walnut", seating),
                product("Floating Shelf Set",
                        "A set of three wall-mounted shelves with hidden brackets and a clean edge profile.",
                        "12500.00", 15, "White Oak", builtIns),
                product("Cherry Wall Cabinet",
                        "A shallow wall cabinet with two framed doors, suited to kitchens and hallways.",
                        "54000.00", 4, "Cherry", builtIns),
                product("End-Grain Cutting Board",
                        "A heavy checkerboard board that is gentle on knives and sealed with food-safe oil.",
                        "6500.00", 20, "Hard Maple", smallGoods),
                product("Walnut Serving Tray",
                        "A slim serving tray with carved handles, made from workshop offcuts.",
                        "4200.00", 25, "Black Walnut", smallGoods)));

        log.info("Seeded {} categories and {} products", categoryRepository.count(), productRepository.count());
    }

    private Category category(String name, String description) {
        return categoryRepository.save(Category.builder().name(name).description(description).build());
    }

    private Product product(String name, String description, String price, int stock,
                            String woodType, Category category) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .woodType(woodType)
                .category(category)
                .build();
    }
}