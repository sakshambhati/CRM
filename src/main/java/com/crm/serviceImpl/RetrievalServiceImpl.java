package com.crm.serviceImpl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.crm.config.AWSClientConfigService;
import com.crm.exception.CustomException;
import com.crm.service.RetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
@Service
public class RetrievalServiceImpl implements RetrievalService {

    @Autowired
    private AWSClientConfigService awsClientConfig;

    @SuppressWarnings("unchecked")
    @Override
    public HashMap<String, Object> saveDocument(MultipartFile file, String token, String deptName, String fileurl
                 ) throws IOException {
        HashMap<String, Object> hash = new HashMap<>();
        try {
            String fileURL = "";

            AmazonS3 awsClient = awsClientConfig.awsClientConfiguration(token);
            String[] split = fileURL.split("/");
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
//            StringBuilder stringBuilder = new StringBuilder();
//
//            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
//            String str = stringBuilder.toString().replace("%20", " ");
//            log.info("new file url is " + str);


            PutObjectRequest request = new PutObjectRequest(deptName, fileurl, file.getInputStream(), metadata);
//            String version = findVersionId(awsClient, username, str);
//            logger.info("Annexure version id is " + version);
//            logger.info("Application version id is " + version);
//            logger.info("Application url is " + str);
            awsClient.putObject(request);
//            deleteVersion(awsClient, username, str, version);


            hash.put("isSuccess", true);
            return hash;
        } catch (Exception e) {
            e.printStackTrace();
            hash.put("isSuccess", false);
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
