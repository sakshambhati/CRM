package com.crm.kafka;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Audit_inventory")
public class AuditMessage {

    @Id
    private String id;
    private String message;
    private String action;
    private String leadId;
    private String note;
    private Date milestoneDate;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm a")
    private LocalDateTime localDateTime = LocalDateTime.now();
    private String time;
    private String date;
    private Date testDate;
    private String clientAddress;
}
