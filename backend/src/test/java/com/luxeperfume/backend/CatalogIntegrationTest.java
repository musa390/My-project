package com.luxeperfume.backend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publicCatalogEndpointsAreAnonymousAndReturnPublishedContent() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(2)));
        mockMvc.perform(get("/api/v1/catalog/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/brands")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/collections")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/featured")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/new-arrivals")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/best-sellers")).andExpect(status().isOk());
    }

    @Test
    void searchFilterSortAndPaginationAreApplied() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products")
                .param("q","Floral").param("category","floral")
                .param("sort","price_asc").param("page","0").param("size","1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    void productDetailIncludesImagesVariantsAndStockAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products/demo-floral-eau-de-parfum"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Demo Floral Eau de Parfum"))
            .andExpect(jsonPath("$.data.images.length()").value(1))
            .andExpect(jsonPath("$.data.variants.length()").value(1))
            .andExpect(jsonPath("$.data.available_stock").value(25));
    }

    @Test
    void draftAndArchivedProductsAreNotPublic() throws Exception {
        String slug = "catalog-hidden-" + UUID.randomUUID();
        Long category = jdbc.queryForObject("SELECT id FROM categories WHERE slug='floral'", Long.class);
        jdbc.update("INSERT INTO products(category_id,name,slug,status) VALUES(?,?,?,'DRAFT')",category,"Hidden test product",slug);
        mockMvc.perform(get("/api/v1/catalog/products/" + slug)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/catalog/products").param("q","Hidden test product"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void staffCatalogMutationRequiresAuthenticationAndPermission() throws Exception {
        mockMvc.perform(post("/api/v1/staff/catalog/categories").with(csrf())
                .contentType("application/json").content("{\"name\":\"Protected Category\"}"))
            .andExpect(status().isUnauthorized());

        String email = "catalog-customer-" + UUID.randomUUID() + "@example.invalid";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Catalog\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = registration.getResponse().getCookie("lp_access");
        mockMvc.perform(post("/api/v1/staff/catalog/categories").with(csrf()).cookie(access)
                .contentType("application/json").content("{\"name\":\"Forbidden Category\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreateCategoryAndItAppearsInPublicCatalog() throws Exception {
        String email = "catalog-manager-" + UUID.randomUUID() + "@example.invalid";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Catalog\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = registration.getResponse().getCookie("lp_access");
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?",Long.class,email);
        jdbc.update("DELETE FROM user_roles WHERE user_id=?",userId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='MANAGER'",userId);

        String name = "Test Catalog " + UUID.randomUUID();
        String slug = "test-catalog-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/staff/catalog/categories").with(csrf()).cookie(access)
                .contentType("application/json")
                .content("{\"name\":\"" + name + "\",\"slug\":\"" + slug + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.slug").value(slug));
        mockMvc.perform(get("/api/v1/catalog/categories/" + slug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value(name));
        mockMvc.perform(get("/api/v1/staff/catalog/categories").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/api/v1/staff/catalog/brands").cookie(access))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/staff/catalog/collections").cookie(access))
            .andExpect(status().isOk());
    }

    @Test
    void staffProductWritePersistsAndAppearsInCustomerCatalog() throws Exception {
        String email = "catalog-writer-" + UUID.randomUUID() + "@example.invalid";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Catalog\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = registration.getResponse().getCookie("lp_access");
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?",Long.class,email);
        jdbc.update("DELETE FROM user_roles WHERE user_id=?",userId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='MANAGER'",userId);

        Long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE slug='floral'",Long.class);
        String slug = "api-created-" + UUID.randomUUID();
        String name = "API Created Perfume " + UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/v1/staff/catalog/products").with(csrf()).cookie(access)
                .contentType("application/json")
                .content("{\"name\":\"" + name + "\",\"slug\":\"" + slug + "\",\"categoryId\":" + categoryId + ",\"productType\":\"VARIABLE\",\"status\":\"DRAFT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.slug").value(slug)).andReturn();
        Long productId = jdbc.queryForObject("SELECT id FROM products WHERE slug=?",Long.class,slug);

        String sku = "SKU-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/staff/catalog/products/" + productId + "/variants").with(csrf()).cookie(access)
                .contentType("application/json")
                .content("{\"sku\":\"" + sku + "\",\"variantName\":\"50 ml\",\"price\":3200,\"isDefault\":true,\"initialStock\":7}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.quantity_on_hand").value(7));
        Long variantId = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku=?",Long.class,sku);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/staff/catalog/variants/" + variantId).with(csrf()).cookie(access)
                .contentType("application/json").content("{\"variantName\":\"50 ml Eau de Parfum\",\"price\":3300,\"compareAtPrice\":3600}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.price").value(3300));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/staff/catalog/variants/" + variantId + "/inventory").with(csrf()).cookie(access)
                .contentType("application/json").content("{\"quantityOnHand\":9,\"quantityReserved\":1,\"reorderLevel\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.quantity_on_hand").value(9));
        mockMvc.perform(get("/api/v1/staff/catalog/products/" + productId + "/variants").cookie(access))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/api/v1/staff/catalog/products/" + productId + "/images").with(csrf()).cookie(access)
                .contentType("application/json")
                .content("{\"imageUrl\":\"https://images.example.invalid/perfume.webp\",\"altText\":\"Test perfume\",\"primary\":true}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/staff/catalog/products/" + productId + "/images").cookie(access))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post("/api/v1/staff/catalog/cloudinary/signature").with(csrf()).cookie(access))
            .andExpect(status().isServiceUnavailable());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/staff/catalog/products/" + productId).with(csrf()).cookie(access)
                .contentType("application/json").content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/catalog/products/" + slug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value(name))
            .andExpect(jsonPath("$.data.variants.length()").value(1))
            .andExpect(jsonPath("$.data.images.length()").value(1))
            .andExpect(jsonPath("$.data.available_stock").value(8));
        mockMvc.perform(get("/api/v1/staff/catalog/products/" + productId).cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(productId));
        assertThat(jdbc.queryForObject("SELECT status FROM products WHERE id=?",String.class,productId)).isEqualTo("ACTIVE");
    }

    @Test
    void invalidSortAndPaginationAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products").param("sort","unsafe_sql"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalog/products").param("page","-1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalog/products").param("size","101"))
            .andExpect(status().isBadRequest());
    }
}
