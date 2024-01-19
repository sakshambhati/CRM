package com.crm.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class PaymentMilestoneDto {

    @Id
    private String id;
    private String name;
    private Double cost;
    private Integer quantity;

}
