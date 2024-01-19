package com.crm.model;

import java.time.LocalDateTime;

//import jakarta.persistence.GeneratedValue;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Document(collection = "CRM_DOCS")
public class FileDetails {
	
	@Id
	private String id;
	private int flagNumber;    // flag no. from front end
	private String fileName;
	private String fileUrl;   // filepath
	private String rtiId;
	// RTI reference number removed
	private String roleName;   // front end  
	private String filetype;
	private String deptName;
	private String contentType;
	@Builder.Default
	private String annotationId="";
	private boolean isSigned;
	@JsonFormat(pattern ="dd MMM yyyy")
	private LocalDateTime signedOn;
	private String prevVersionId = "";
	private String uuid;
	private String status;
	
	@Builder.Default
	private String comment="";
	private String fileId;
	private String subject;
	private String serviceLetterId;
	@Builder.Default
	private boolean isCoverNote=false;
	
//	@JsonFormat(pattern = "dd MMM yyyy")
	@JsonFormat(pattern = "yyyy/MM/dd")
	private LocalDateTime uploadTime = LocalDateTime.now();
	
	private String uploader;
	private String prevFlagNumber;
	private String flagNumberMarking="A";
	private boolean canDelete;
	@Transient
	private MultipartFile file;
}
