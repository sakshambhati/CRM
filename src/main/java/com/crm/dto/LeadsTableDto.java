package com.crm.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class LeadsTableDto {
    @Id
    private String id;
    private Integer discount;
    private Integer qty;
    private Integer price;
    private String productName;
    private String leadsId;
//    private Integer sum;
}
