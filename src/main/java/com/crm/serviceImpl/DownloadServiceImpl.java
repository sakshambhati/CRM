package com.crm.serviceImpl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.crm.config.AWSClientConfigService;
import com.crm.model.Contact;
import com.crm.repository.CreateContactRepo;

@Service
public class DownloadServiceImpl {

	
	@Autowired
	CreateContactRepo contactRepo;
	
	@Autowired
	AWSClientConfigService awsClientConfigService;
	
	
	public ResponseEntity<?> getDownloadedfile(String contactId, String fileUrl, String deptName, String token) throws IOException {
		
		System.out.println("id = "+contactId);
		token = token.replace("Bearer ", "");
		Optional<Contact> findById = contactRepo.findById(contactId);
		if(!findById.isPresent()) {
			throw new IllegalArgumentException("No id found");
		}
		
		Contact createContact = findById.get();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
		
		
//		String[] url = fileUrl.split("/", 2);
		
		// finding extension
		int lastIndexOf = fileUrl.lastIndexOf(".");
		String extension = null;
		if(lastIndexOf != -1) {
			 extension = fileUrl.substring(lastIndexOf+1);
		}
		System.out.println("url = "+fileUrl);
		
		AmazonS3 awsClient = awsClientConfigService.awsClientConfiguration(token);
		
		S3Object fil = awsClient.getObject(deptName, fileUrl);
		InputStream inputStream = fil.getObjectContent();
		File file2 = new File(createContact.getName() + extension);     
		FileUtils.copyInputStreamToFile(inputStream, file2);
//		zipOutputStream.putNextEntry(new ZipEntry(source[1]));          // updated
//				outboxId + "/Dak/" + fileTrackInventory.getApplicationList().getFileName()));
		FileInputStream fileInputStream = new FileInputStream(file2);
//		IOUtils.copy(fileInputStream, zipOutputStream);
		byte[] readAllBytes = fileInputStream.readAllBytes();
		fileInputStream.close();
		inputStream.close();
//		zipOutputStream.closeEntry();
		inputStream.close();
		
		IOUtils.closeQuietly(bufferedOutputStream);
		IOUtils.closeQuietly(byteArrayOutputStream);
		
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Content-Disposition",
		"attachment; filename=\"" + LocalDateTime.now().toString() + "."+extension + "\"");	
		
		
		return ResponseEntity.status(HttpStatus.OK).headers(httpHeaders).body(readAllBytes);
		
	}

	
}
