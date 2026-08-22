package com.mart.quickpass.user.controller;

import com.mart.quickpass.global.config.AuthCookieProperties;
import com.mart.quickpass.user.dto.UserResponse;
import com.mart.quickpass.user.service.UserService;
import com.mart.quickpass.user.service.UserWithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private UserWithdrawalService userWithdrawalService;
    @Mock
    private AuthCookieProperties authCookieProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserController(
                userService,
                userWithdrawalService,
                authCookieProperties
        ))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyInfoReturnsAuthenticatedUsersBasicAccountInformation() throws Exception {
        when(userService.getMyInfo(1L))
                .thenReturn(new UserResponse(1L, "user@example.com", "홍길동"));
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(1L, null));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"));
    }
}
