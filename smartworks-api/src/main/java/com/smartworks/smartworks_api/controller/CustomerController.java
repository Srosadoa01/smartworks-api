package com.smartworks.smartworks_api.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartworks.smartworks_api.entity.Customer;
import com.smartworks.smartworks_api.repository.CustomerRepository;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository repo;

    public CustomerController(CustomerRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Customer> list() {
        return repo.findAll();
    }

    @PostMapping
    public Customer create(@RequestBody Customer c) {
        if (c.getName() == null || c.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        if (c.getEmail() == null) {
            c.setEmail("");
        }

        if (c.getPhone() == null) {
            c.setPhone("");
        }

        if (c.getImageUrl() == null) {
            c.setImageUrl("");
        }

        return repo.save(c);
    }

    @PatchMapping("/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer data) {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        if (data.getName() != null && !data.getName().isBlank()) {
            customer.setName(data.getName());
        }

        if (data.getPhone() != null) {
            customer.setPhone(data.getPhone());
        }

        if (data.getEmail() != null) {
            customer.setEmail(data.getEmail());
        }

        if (data.getImageUrl() != null) {
            customer.setImageUrl(data.getImageUrl());
        }

        return repo.save(customer);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        repo.delete(customer);
    }

    @PatchMapping(
            value = "/{id}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Customer uploadPhoto(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        String extension = resolveExtension(file);

        if (!isAllowedImageExtension(extension)) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        Path uploadDir = Paths
                .get(System.getProperty("user.dir"), "uploads", "customers")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadDir);

        String fileName = UUID.randomUUID() + extension;

        Path destination = uploadDir
                .resolve(fileName)
                .toAbsolutePath()
                .normalize();

        if (!destination.startsWith(uploadDir)) {
            throw new IOException("Invalid upload path");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        customer.setImageUrl("/uploads/customers/" + fileName);

        return repo.save(customer);
    }

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();

        if (originalName != null && !originalName.isBlank()) {
            int dotIndex = originalName.lastIndexOf(".");

            if (dotIndex != -1) {
                String extension = originalName.substring(dotIndex).toLowerCase();

                if (extension.equals(".jpeg")) {
                    return ".jpg";
                }

                return extension;
            }
        }

        String contentType = file.getContentType();

        if (contentType == null) {
            return ".jpg";
        }

        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/jpeg", "image/jpg" -> ".jpg";
            default -> ".jpg";
        };
    }

    private boolean isAllowedImageExtension(String extension) {
        if (extension == null) {
            return false;
        }

        return extension.equals(".jpg")
                || extension.equals(".png")
                || extension.equals(".webp")
                || extension.equals(".gif");
    }
}