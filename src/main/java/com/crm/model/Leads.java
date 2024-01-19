package com.crm.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Leads {

    @Id
    private String id;
    private String account;          // imp.   // list of accounts = NTPC, IAF, Navy  -> list of contacts related to that account
    private String description;      // imp.
    private String leadTitle;        // imp.
    private String status;
    private String contact;
    private List<String> product;       // not necessary
    private Integer discount;       // not necessary
    private List<String> quantity;
    private List<String> cost;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    private LocalDateTime date = LocalDateTime.now();    // start date
    private String leadId;

    private List<PaymentMilestone> paymentMilestoneLst;

    private int productCounter=0;
    private int serviceCounter=0;

    private List<Products> products;
    private Integer estimatedValue;     // make different for products and services
    private List<String> note;
    private String startDate;
    private String endDate;
    private List<FileDetails> fileDetails;      // should be used for invoices
    private String quotationUrl;
    private String poUrl;
    // 6:40


}
