package com.crm.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "Accounts")
public class Account {

	@Id
	private String id;
	
	private String accountName;       // bucket
	private String accountOwner;      // key
	private String accountType;
	private String address1;
	private String address2;
	private String email;
	private String phone;
	private String description;
	private String country;
	private String city;
	private LocalDateTime date;
	private List<Leads> leads;
	
}
