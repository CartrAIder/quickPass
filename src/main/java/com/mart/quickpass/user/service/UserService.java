package com.mart.quickpass.user.service;

import com.mart.quickpass.global.exception.DuplicateEmailException;
import com.mart.quickpass.email.service.EmailVerificationService;
import com.mart.quickpass.user.dto.SignUpRequest;
import com.mart.quickpass.user.dto.SignUpResponse;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    // 회원가입 메서드
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        emailVerificationService.requireVerified(request.email());

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);
        emailVerificationService.consumeVerification(request.email());

        return SignUpResponse.from(savedUser);
    }
}
