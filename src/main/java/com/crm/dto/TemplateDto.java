package com.crm.dto;

import lombok.Data;

import java.util.List;

@Data
public class TemplateDto {

    private String id;
    private String date;
    private String contact;

    private String address1;
    private String address2;
    private String address3;

    private String type;          // t1, t2, t3, t4....
//    private String quantity;
////    private String qty1;
////    private String qty2;
////    private String qty3;
//
//    private String product;
////    private String pd1;
////    private String pd2;
////    private String pd3;
//
//    private String cost;
//    private String cost1;
//    private String cost2;
//    private String cost3;

    private List<QuotationDetails> quotationDetails;
}
