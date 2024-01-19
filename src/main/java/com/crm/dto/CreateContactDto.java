package com.crm.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.crm.model.FileDetails;

import lombok.Data;

@Data
public class CreateContactDto {

	private String id;
	private String name;
	private List<String> notes;
	private List<MultipartFile> files;
	private String fileUrl;
	private String note;
	private FileDetails fileDetails;
}
