package com.crm.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.crm.model.Products;

public interface ProductDescriptionRepo extends MongoRepository<Products, String> {




	Page<Products> findBySkuOrProductTitleOrDescriptionOrMrpAndDateBetween(String sku, String productTitle,
                                                                           String description, String mrp, LocalDateTime date, LocalDateTime plusDays, Pageable paging);

	Page<Products> findBySkuOrProductTitleOrDescriptionOrMrp(String sku, String productTitle,
                                                             String description, String mrp, Pageable paging);


	Products findByProductTitle(String product);
}
