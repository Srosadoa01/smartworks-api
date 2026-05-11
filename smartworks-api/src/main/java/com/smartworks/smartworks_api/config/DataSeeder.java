package com.smartworks.smartworks_api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.smartworks.smartworks_api.entity.Product;
import com.smartworks.smartworks_api.repository.ProductRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepo) {
        return args -> {
            seed(productRepo, "Palet Ciruela amarilla", 18, 4, 935.0);
            seed(productRepo, "Palet Ciruela roja", 18, 4, 880.0);
            seed(productRepo, "Palet Nectarina amarilla", 16, 4, 900.0);
            seed(productRepo, "Palet Nectarina roja", 16, 4, 930.0);
            seed(productRepo, "Palet Aguacate", 12, 3, 1450.0);
            seed(productRepo, "Palet Melocotón amarillo", 16, 4, 920.0);
            seed(productRepo, "Palet Melocotón rojo", 16, 4, 950.0);
            seed(productRepo, "Palet Manzana roja", 20, 5, 720.0);
            seed(productRepo, "Palet Manzana amarilla", 20, 5, 700.0);
            seed(productRepo, "Palet Mango", 10, 3, 1050.0);
            seed(productRepo, "Palet Kiwi", 14, 4, 1080.0);
            seed(productRepo, "Palet Naranja", 22, 6, 810.0);
            seed(productRepo, "Palet Mandarina/Clementina", 20, 5, 920.0);
            seed(productRepo, "Palet Limón", 18, 5, 700.0);
            seed(productRepo, "Palet Plátano", 22, 6, 1100.0);
            seed(productRepo, "Palet Pera conferencia", 18, 5, 780.0);
            seed(productRepo, "Palet Uva", 12, 3, 990.0);
            seed(productRepo, "Palet Piña", 10, 3, 820.0);
            seed(productRepo, "Palet Sandía", 14, 4, 620.0);
        };
    }

    private void seed(ProductRepository repo, String name, int stock, int lowStock, double price) {
        // Si ya existe por nombre, actualiza lo básico; si no, crea.
        Product p = repo.findAll().stream()
                .filter(x -> name.equalsIgnoreCase(x.getName()))
                .findFirst()
                .orElse(null);

        if (p == null) {
            p = new Product();
            p.setName(name);
        }

        p.setStock(stock);
        p.setLowStockThreshold(lowStock);
        p.setPrice(price);

        repo.save(p);
    }
}
