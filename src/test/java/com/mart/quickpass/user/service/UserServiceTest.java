package com.mart.quickpass.user.service;

import com.mart.quickpass.email.service.EmailVerificationService;
import com.mart.quickpass.global.exception.UserNotFoundException;
import com.mart.quickpass.user.dto.UserResponse;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private User user;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, emailVerificationService);
    }

    @Test
    void getMyInfoReturnsBasicAccountInformation() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getName()).thenReturn("홍길동");

        UserResponse response = userService.getMyInfo(userId);

        assertThat(response).isEqualTo(new UserResponse(1L, "user@example.com", "홍길동"));
    }

    @Test
    void getMyInfoRejectsUnknownUser() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo(userId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
