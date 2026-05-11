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

import com.smartworks.smartworks_api.dto.UpdateProductRequest;
import com.smartworks.smartworks_api.entity.Product;
import com.smartworks.smartworks_api.repository.ProductRepository;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Product> list() {
        return repo.findAll();
    }

    @GetMapping("/low-stock")
    public List<Product> lowStock() {
        return repo.findLowStock();
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        if (product.getLowStockThreshold() < 0) {
            product.setLowStockThreshold(5);
        }

        if (product.getStock() < 0) {
            product.setStock(0);
        }

        if (product.getPrice() < 0) {
            product.setPrice(0);
        }

        if (product.getImageUrl() == null) {
            product.setImageUrl("");
        }

        return repo.save(product);
    }

    @PatchMapping("/{id}")
    public Product update(
            @PathVariable Long id,
            @RequestBody UpdateProductRequest req
    ) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (req.name != null && !req.name.isBlank()) {
            p.setName(req.name);
        }

        if (req.stock != null) {
            p.setStock(req.stock);
        }

        if (req.lowStockThreshold != null) {
            p.setLowStockThreshold(req.lowStockThreshold);
        }

        if (req.price != null) {
            p.setPrice(req.price);
        }

        if (req.imageUrl != null) {
            p.setImageUrl(req.imageUrl);
        }

        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        repo.delete(product);
    }

    @PatchMapping(
            value = "/{id}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Product uploadPhoto(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        Product product = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        String extension = resolveExtension(file);

        if (!isAllowedImageExtension(extension)) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        Path uploadDir = Paths
                .get(System.getProperty("user.dir"), "uploads", "products")
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

        product.setImageUrl("/uploads/products/" + fileName);

        return repo.save(product);
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