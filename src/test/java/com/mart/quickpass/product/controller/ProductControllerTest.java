package com.mart.quickpass.product.controller;

import com.mart.quickpass.global.exception.GlobalExceptionHandler;
import com.mart.quickpass.product.dto.ProductSliceResponse;
import com.mart.quickpass.product.dto.ProductSortType;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchUsesDocumentedDefaults() throws Exception {
        when(productService.search(null, null, ProductSortType.NAME_ASC, 0, 20))
                .thenReturn(new ProductSliceResponse(List.of(), 0, 20, false));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(productService).search(null, null, ProductSortType.NAME_ASC, 0, 20);
    }

    @Test
    void searchAcceptsKeywordCategorySortAndPageParameters() throws Exception {
        when(productService.search("우유", ProductCategory.DAIRY, ProductSortType.PRICE_ASC, 1, 5))
                .thenReturn(new ProductSliceResponse(List.of(), 1, 5, false));

        mockMvc.perform(get("/api/products")
                        .param("keyword", "우유")
                        .param("category", "DAIRY")
                        .param("sort", "PRICE_ASC")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(productService).search("우유", ProductCategory.DAIRY, ProductSortType.PRICE_ASC, 1, 5);
    }

    @Test
    void categoriesExposeStableCodesAndKoreanNames() throws Exception {
        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DAIRY"))
                .andExpect(jsonPath("$[0].name").value("유제품"))
                .andExpect(jsonPath("$[6].code").value("HOUSEHOLD"))
                .andExpect(jsonPath("$[6].name").value("생활용품"));
    }
}
