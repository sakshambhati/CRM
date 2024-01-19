package com.crm.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

@Configuration
public class AWSS3ContentService {

	@Bean
	public AmazonS3 client() {	
		@SuppressWarnings("deprecation")
		AmazonS3Client amazonS3Client = new AmazonS3Client(new AnonymousAWSCredentials());
		return amazonS3Client;
	}
}

