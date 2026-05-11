package com.smartworks.smartworks_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartworks.smartworks_api.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCase(String name);
}