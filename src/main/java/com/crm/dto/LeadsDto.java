package com.crm.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class LeadsDto {

    @Id
    private String id;
    private String account;          // imp.   // list of accounts = NTPC, IAF, Navy  -> list of contacts related to that account
    private String description;      // imp.
    private String leadTitle;        // imp.
    private String status;
    private String contact;
    private List<String> Products;       // not necessary
    private List<Integer> discount;       // not necessary
    private List<String> quantity;
    private List<String> cost;
}
