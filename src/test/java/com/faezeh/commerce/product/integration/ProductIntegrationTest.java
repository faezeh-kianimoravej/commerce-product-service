package com.faezeh.commerce.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faezeh.commerce.product.entity.Product;
import com.faezeh.commerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
        productRepository.flush();
    }

    @Test
    void createAndRetrieveProduct() throws Exception {
        String createResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("TSHIRT-BLACK-L", 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sku").value("TSHIRT-BLACK-L"))
                .andExpect(jsonPath("$.name").value("Black T-Shirt Large"))
                .andExpect(jsonPath("$.description").value("Black cotton T-shirt, size large"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.availableQuantity").value(10))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdProduct = objectMapper.readTree(createResponse);
        long productId = createdProduct.get("id").asLong();

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.sku").value("TSHIRT-BLACK-L"))
                .andExpect(jsonPath("$.name").value("Black T-Shirt Large"))
                .andExpect(jsonPath("$.description").value("Black cotton T-shirt, size large"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.availableQuantity").value(10))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void duplicateSkuReturnsConflictWithStructuredError() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("TSHIRT-BLACK-L", 10)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("TSHIRT-BLACK-L", 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Product already exists with SKU: TSHIRT-BLACK-L"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void invalidCreateProductReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "",
                                  "name": "Black T-Shirt Large",
                                  "description": "Black cotton T-shirt, size large",
                                  "price": -29.99,
                                  "availableQuantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.fieldErrors.sku").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.availableQuantity").exists());
    }

    @Test
    void softDeleteKeepsRowAndExcludesProductFromActiveReads() throws Exception {
        String createResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("TSHIRT-BLACK-L", 10)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long productId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNoContent());

        assertThat(productRepository.findById(productId))
                .isPresent()
                .get()
                .extracting(Product::getActive)
                .isEqualTo(false);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(jsonPath("$[*].sku", not(hasItem("TSHIRT-BLACK-L"))));
    }

    private String productJson(String sku, int availableQuantity) {
        return """
                {
                  "sku": "%s",
                  "name": "Black T-Shirt Large",
                  "description": "Black cotton T-shirt, size large",
                  "price": 29.99,
                  "availableQuantity": %d
                }
                """.formatted(sku, availableQuantity);
    }
}
