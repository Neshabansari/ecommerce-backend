package com.ecommerce;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.util.TimeZone;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests: the real application, real security, and a real
 * temporary PostgreSQL. The "test" profile switches off the seed data, so every test
 * creates its own data with unique names and never depends on another test.
 */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static {
        // Same fix as in EcommerceBackendApplication. That block only runs through main(),
        // not when the test framework starts the app, and PostgreSQL 16 rejects the
        // legacy zone name "Asia/Calcutta" that Windows Java reports.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    protected static final String PASSWORD = "Password123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    // ---- request helpers (token and body may be null) ----

    protected ResultActions doGet(String url, String token, Object... uriVars) throws Exception {
        return perform(MockMvcRequestBuilders.get(url, uriVars), token, null);
    }

    protected ResultActions doPost(String url, String token, String body, Object... uriVars) throws Exception {
        return perform(MockMvcRequestBuilders.post(url, uriVars), token, body);
    }

    protected ResultActions doPut(String url, String token, String body, Object... uriVars) throws Exception {
        return perform(MockMvcRequestBuilders.put(url, uriVars), token, body);
    }

    protected ResultActions doPatch(String url, String token, String body, Object... uriVars) throws Exception {
        return perform(MockMvcRequestBuilders.patch(url, uriVars), token, body);
    }

    protected ResultActions doDelete(String url, String token, Object... uriVars) throws Exception {
        return perform(MockMvcRequestBuilders.delete(url, uriVars), token, null);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token, String body)
            throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    // ---- account helpers ----

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected String registerBody(String fullName, String email, String password) {
        return "{\"fullName\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(fullName, email, password);
    }

    protected String login(String email, String password) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        String response = doPost("/api/auth/login", null, body)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    /** Registers a brand-new customer through the API and returns a login token. */
    protected String customerToken() throws Exception {
        String email = uniqueEmail();
        doPost("/api/auth/register", null, registerBody("Test Customer", email, PASSWORD))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    /** Creates an ADMIN directly in the database (the API never creates admins) and logs in. */
    protected String adminToken() throws Exception {
        String email = uniqueEmail();
        userRepository.save(User.builder().fullName("Test Admin").email(email)
                .password(passwordEncoder.encode(PASSWORD)).role(Role.ADMIN).build());
        return login(email, PASSWORD);
    }

    // ---- data helpers ----

    protected Category createCategory() {
        return categoryRepository.save(Category.builder().name("Cat-" + UUID.randomUUID()).build());
    }

    protected Product createProduct(String name, String price, int stock, Category category) {
        return productRepository.save(Product.builder().name(name).description("Test product")
                .price(new BigDecimal(price)).stockQuantity(stock).category(category).build());
    }

    protected Product createProduct(String name, String price, int stock) {
        return createProduct(name, price, stock, createCategory());
    }

    protected int stockOf(Long productId) {
        return productRepository.findById(productId).orElseThrow().getStockQuantity();
    }

    protected long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}