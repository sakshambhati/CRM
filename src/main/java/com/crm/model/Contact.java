package com.crm.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.crm.dto.CreateContactDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Contact_Details")
public class Contact {
	
	@Id
	private String id;
	private String contactId;
	private String name;        // Eg - group captain, acct.= IAF
	public ArrayList<HashMap<String, Object>> contact = new ArrayList<>();
//	public Map<String, Object> contact;
	private String account; 
	private String department;
	private String designation;
	private String description;
	private String phoneNo;
	private String email;
	private List<CreateContactDto> dto;
	private List<FileDetails> fileDetails;
	private int fileNumber;
	private String contactOwner;
	private LocalDateTime date;
//	private ArrayList<Object> arrayList = new ArrayList<>();
}
