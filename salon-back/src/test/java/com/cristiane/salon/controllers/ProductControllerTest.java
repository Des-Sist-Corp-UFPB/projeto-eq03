package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.ProductController;
import com.cristiane.salon.models.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import com.cristiane.salon.models.product.dto.ProductResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser
    void createReturns201_whenValid() throws Exception {
        when(productService.create(any())).thenReturn(null);

        String body = "{\"name\":\"xyz\",\"price\":10.0}";

        mvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void createReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void reactivateReturns200() throws Exception {
        ProductResponse dummyResponse = new ProductResponse(1L, "XYZ", 10, new BigDecimal("10.0"), true);
        when(productService.reactivate(any())).thenReturn(dummyResponse);

        mvc.perform(patch("/v1/products/1/reactivate"))
                .andExpect(status().isOk());
    }
}

