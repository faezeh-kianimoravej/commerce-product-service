package com.faezeh.commerce.product.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.faezeh.commerce.product.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.product.dto.ProductResponse;
import com.faezeh.commerce.product.exception.DuplicateProductSkuException;
import com.faezeh.commerce.product.exception.ProductNotFoundException;
import com.faezeh.commerce.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getProductsReturnsProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(productResponse()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"));
    }

    @Test
    void createProductReturnsCreatedProduct() throws Exception {
        when(productService.createProduct(any())).thenReturn(productResponse());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-001",
                                  "name": "Wireless Mouse",
                                  "description": "Ergonomic wireless mouse",
                                  "price": 29.99,
                                  "availableQuantity": 25
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createProductWithDuplicateSkuReturnsConflict() throws Exception {
        when(productService.createProduct(any())).thenThrow(new DuplicateProductSkuException("SKU-001"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-001",
                                  "name": "Wireless Mouse",
                                  "description": "Ergonomic wireless mouse",
                                  "price": 29.99,
                                  "availableQuantity": 25
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.message").value("Product already exists with SKU: SKU-001"));
    }

    @Test
    void createProductWithInvalidRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "",
                                  "name": "",
                                  "price": -1,
                                  "availableQuantity": -3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.fieldErrors.sku").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.availableQuantity").exists());
    }

    @Test
    void getProductByIdReturnsProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productResponse());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"));
    }

    @Test
    void getMissingProductReturnsNotFound() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException("id", 99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/products/99"))
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void getProductBySkuReturnsProduct() throws Exception {
        when(productService.getProductBySku("SKU-001")).thenReturn(productResponse());

        mockMvc.perform(get("/api/products/sku/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void checkProductAvailabilityReturnsAvailableResponse() throws Exception {
        when(productService.checkAvailability(1L, 10))
                .thenReturn(new ProductAvailabilityResponse(1L, true, true, 25));

        mockMvc.perform(get("/api/products/1/availability")
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.existsAndActive").value(true))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availableQuantity").value(25));

        verify(productService).checkAvailability(1L, 10);
    }

    @Test
    void checkProductAvailabilityReturnsUnavailableResponse() throws Exception {
        when(productService.checkAvailability(1L, 30))
                .thenReturn(new ProductAvailabilityResponse(1L, true, false, 25));

        mockMvc.perform(get("/api/products/1/availability")
                        .param("quantity", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.existsAndActive").value(true))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.availableQuantity").value(25));
    }

    @Test
    void checkProductAvailabilityWithInvalidQuantityReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/1/availability")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/products/1/availability"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void updateProductReturnsUpdatedProduct() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(productResponse());

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Wireless Mouse",
                                  "description": "Ergonomic wireless mouse",
                                  "price": 29.99,
                                  "availableQuantity": 25,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.availableQuantity").value(25))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deleteProductReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    private ProductResponse productResponse() {
        return new ProductResponse(
                1L,
                "SKU-001",
                "Wireless Mouse",
                "Ergonomic wireless mouse",
                new BigDecimal("29.99"),
                25,
                true
        );
    }
}
