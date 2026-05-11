package com.smartworks.smartworks_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartworks.smartworks_api.entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
