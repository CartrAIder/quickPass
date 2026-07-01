package com.mart.quickpass.user.repository;

import com.mart.quickpass.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
