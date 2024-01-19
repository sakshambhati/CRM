package com.crm.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Data
public class Deal {

    @Id
    private String id;
    private String dealName;        // leadName
    private String accountName;   //from lead
    private String leadId;
//    private HashMap<String, String> paymentMilestone;
    private String poNumber;
    private String status;
    private PaymentMilestone paymentMilestone;
    private String milestoneId;
    private LocalDate dueDate;
    private String transactionId;     // for payment inclusion in milestone
}
