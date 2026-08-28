package com.mart.quickpass.product.controller;

import com.mart.quickpass.global.exception.GlobalExceptionHandler;
import com.mart.quickpass.product.dto.ProductImageUploadResponse;
import com.mart.quickpass.product.service.AdminProductService;
import com.mart.quickpass.product.service.ProductImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AdminProductControllerTest {

    @Mock
    private AdminProductService adminProductService;

    @Mock
    private ProductImageService productImageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new AdminProductController(adminProductService, productImageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadImageReturnsMinioImageUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "milk.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        when(productImageService.upload("8800000000001", image, false))
                .thenReturn(new ProductImageUploadResponse(
                        "http://localhost:9000/product-images/products/8800000000001/image.jpg", true));

        mockMvc.perform(multipart("/api/admin/products/8800000000001/image").file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl")
                        .value("http://localhost:9000/product-images/products/8800000000001/image.jpg"))
                .andExpect(jsonPath("$.uploaded").value(true));

        verify(productImageService).upload("8800000000001", image, false);
    }
}
