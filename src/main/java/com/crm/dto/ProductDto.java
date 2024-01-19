package com.crm.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ProductDto {

    @Id
    private String id;
    private Integer cost;
    private Integer discount;
    private String product;
    private Integer quantity;
    private Integer estimatedValue;
}
