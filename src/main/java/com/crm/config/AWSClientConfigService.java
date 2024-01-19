package com.crm.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;


@Service
public class AWSClientConfigService {

	@Value("${minio.rest-url}")
	private String baseUrl;
	
	@Value("${minio.rest-port}")
	private String port;
	
	//	AWS Services
	@Autowired
	private AWSConfigService awsConfigService;
	@Autowired
	private AWSS3ContentService awsS3ContentService; 


	public AmazonS3 awsClientConfiguration(String token)
	{
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setSignerOverride("AWSS3V4SignerType");
		
		BasicSessionCredentials response = awsConfigService.client(token);
		System.out.println(response);
		AmazonS3 awsClient = awsS3ContentService.client();

		awsClient = AmazonS3ClientBuilder.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(baseUrl+":"+port,
						Regions.US_EAST_1.getName()))
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(response)).build();
		System.out.println(awsClient);
		return awsClient;
	}
}
