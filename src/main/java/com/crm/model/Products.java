package com.crm.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "Product_Description")
public class Products {
	
	@Id
	private String id;
	private String sku;
	private String productTitle;
	private String mrp;             // cost
	private String description;
	private LocalDateTime date;
	private String hsn;
	private String productType;

	// Banty
	private Integer cost;      // currently being used
	private Double discount;
	private String product;
	private Integer quantity;
	private Double estimatedValue;
}
