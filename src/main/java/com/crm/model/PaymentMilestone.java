package com.crm.model;

import com.crm.dto.PaymentMilestoneDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class PaymentMilestone {

    @Id
    private String id;

    private List<PaymentMilestoneDto> paymentMilestoneDto;   // name, cost, quantity

    // xtra    // to remove change in templateServiceImpl
    private String name;
    private Double cost;
    private Integer quantity;

    private Double totalCost;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date paymentDate;
    private int milestoneId;
    private String milestoneNumber;
    private Double totalCostSum;
    private LocalDateTime deliveryDate;
    private int expectedEndDate;     // days in int form from frontend
    private String leadId;
    private String status;

}
