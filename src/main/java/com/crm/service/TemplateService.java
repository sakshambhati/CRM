package com.crm.service;

import com.crm.dto.LeadsDto;
import com.crm.dto.TemplateDto;
import com.crm.model.Leads;
import com.crm.model.PaymentMilestone;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public interface TemplateService {

//    byte[] editTemplate1(TemplateDto templateDto, String deptName, String token) throws IOException;

    HashMap<String, Object> editTemplate(TemplateDto templateDto, String deptName, String token) throws IOException;

    byte[] editTemplate1(TemplateDto templateDto, String deptName, String token) throws IOException;

//    byte[] editTemplate2(TemplateDto templateDto, String deptName, String token) throws IOException;

    // data from leads
//    byte[] editTemplate2(Leads leads, String deptName, String token) throws IOException;

    // data from leads
    byte[] editTemplate2(Leads leads, String deptName, Double finalDiscount, String templateType, String token) throws IOException;

    byte[] invoice(Leads leads, String deptName, List<PaymentMilestone> hash, String invoice, String type, String token) throws IOException;
}
