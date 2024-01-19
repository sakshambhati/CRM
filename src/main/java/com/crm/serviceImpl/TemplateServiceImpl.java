package com.crm.serviceImpl;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.crm.config.AWSClientConfigService;
import com.crm.dto.LeadsDto;
import com.crm.dto.TemplateDto;
import com.crm.exception.CustomException;
import com.crm.model.*;
import com.crm.repository.AccountRepo;
import com.crm.repository.LeadsRepo;
import com.crm.repository.ProductDescriptionRepo;
import com.crm.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@Service
public class TemplateServiceImpl implements TemplateService {
    @Autowired
    AWSClientConfigService awsClientConfig;

    @Autowired
    ProductDescriptionRepo productDescriptionRepo;

    @Autowired
    DiscoveryClient client;
    @Autowired
    AccountRepo accountRepo;
    @Autowired
    LeadsRepo leadsRepo;
    private final static String bucket = "template";
    private final static String key = "Quotation.docx";

    // To make bucket public
    public String bucketPolicy(String rolename) {
        Policy bucket_policy = new Policy().withStatements(new Statement(Statement.Effect.Allow)
                .withPrincipals(Principal.AllUsers).withActions(S3Actions.GetObject)
                .withResources(new Resource("arn:aws:s3:::" + rolename + "/*")));
        return bucket_policy.toJson();
    }

    //  for sign-service via consul
    @Override
    public HashMap<String, Object> editTemplate(TemplateDto templateDto, String deptName, String token) throws IOException {

        token = token.replace("Bearer ", "");
        AmazonS3Client awsClient = (AmazonS3Client) awsClientConfig.awsClientConfiguration(token);
        if(!awsClient.doesBucketExistV2(bucket)) {
            CreateBucketRequest withObjectLockEnabledForBucket = new CreateBucketRequest(deptName)
                    .withObjectLockEnabledForBucket(true);
            awsClient.createBucket(withObjectLockEnabledForBucket);
            String policy = bucketPolicy(deptName);
            awsClient.setBucketPolicy(deptName, policy);
        }
        InputStream input1 = awsClient.getObject(bucket, key).getObjectContent();

        // method call to sign service via consul
        URI uri = client.getInstances("sign-service").stream().map(si -> si.getUri()).findFirst()
                .map(s -> s.resolve("/sign_service/api/sign")).get();

        RestTemplate restTemplate = new RestTemplate();

        // Define the parameterized type reference for the response type
        ParameterizedTypeReference<HashMap<String, Object>> responseType = new ParameterizedTypeReference<HashMap<String, Object>>() {};

        ResponseEntity<HashMap<String, Object>> json2 = restTemplate.exchange(uri, HttpMethod.POST,
                new HttpEntity<>(httpHeaders(token)), responseType);


        try (XWPFDocument doc = new XWPFDocument(input1)) {
            List<XWPFParagraph> xwpfParagraphList = doc.getParagraphs();
            for (XWPFParagraph xwpfParagraph : xwpfParagraphList) {
                for (XWPFRun xwpfRun : xwpfParagraph.getRuns()) {
                    String docText = xwpfRun.getText(0);

                    if (docText != null) {
                        docText = docText.replace("<contact>", templateDto.getContact());

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
                        LocalDateTime localdate = LocalDateTime.now();
                        String date = "";
                        date = localdate.format(formatter);
                        docText = docText.replace("<date>", date);
                        docText = docText.replace("<date> ", date);
                        docText = docText.replace(" <date>", date);
                        docText = docText.replace(" <date> ", date);
                        docText = docText.replace("<address1>", templateDto.getAddress1());
                        docText = docText.replace("<address2>", templateDto.getAddress2());
                        docText = docText.replace("<address3>", templateDto.getAddress3());
                        docText = docText.replace(" <address1> ", templateDto.getAddress1());
                        docText = docText.replace(" <address2> ", templateDto.getAddress2());
                        docText = docText.replace(" <address3> ", templateDto.getAddress3());
//                        docText = docText.replace(" <signature> ", sign);

                        log.info("date passed " +date);

                        xwpfRun.setText(docText, 0);
                    }
                }
            }

            log.info("entering table");
            // table
            XWPFTable table = doc.getTables().get(0);   // Assuming the table is the first one

            Double costSum = 0.0;
            for (int i = 0; i < templateDto.getQuotationDetails().size(); i++) {
                XWPFTableRow newRow = table.createRow();

                // Populate the columns with data
                newRow.getCell(0).setText(String.valueOf(i + 1));
                String product = templateDto.getQuotationDetails().get(i).getProduct();
                String sku = productDescriptionRepo.findByProductTitle(product).getSku();
                newRow.getCell(1).setText(sku);
                newRow.getCell(2).setText(product);
                newRow.getCell(3).setText(templateDto.getQuotationDetails().get(i).getQuantity());
                newRow.getCell(4).setText(templateDto.getQuotationDetails().get(i).getCost());
                Integer cost = Integer.parseInt(templateDto.getQuotationDetails().get(i).getQuantity()) * Integer.parseInt(templateDto.getQuotationDetails().get(i).getCost());
                newRow.getCell(5).setText(String.valueOf(cost));

                costSum += cost;
            }


            //GST
            Double totalCost = 0.0;
            totalCost = (costSum * 0.18) + costSum;
            double gstCost = costSum * 0.18;

            XWPFTableRow secondLastRow = table.createRow();
            secondLastRow.getCell(3).setText("GST");
            secondLastRow.getCell(4).setText("@18%");
            secondLastRow.getCell(5).setText(String.valueOf(gstCost));

            XWPFTableRow lastRow = table.createRow();
            // delete last cell
            XWPFTableCell lastCell = lastRow.getTableCells().get(lastRow.getTableCells().size() - 1);
            lastRow.getCell(3).getCTTc().addNewTcPr().addNewGridSpan().setVal(BigInteger.valueOf(2));  // merging two cells
            lastRow.getCell(3).setText("GRAND TOTAL (inclusive of taxes)");
            lastRow.getCell(4).setText(String.valueOf(totalCost));
            lastRow.getTableCells().remove(lastCell);



            ByteArrayOutputStream b = new ByteArrayOutputStream();
            doc.write(b); // doc should be a XWPFDocument
            InputStream targetStream = new ByteArrayInputStream(b.toByteArray());
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            metadata.setContentLength(targetStream.available());

            PutObjectRequest request1 = new PutObjectRequest(deptName, key, targetStream, metadata);
            awsClient.putObject(request1);
            targetStream.close();  //
            S3Object object = new S3Object();
            try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(deptName, key);
                object = awsClient.getObject(getObjectRequest);

            } catch (Exception e) {
                log.info(e.getMessage());
                throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                while (targetStream != null && targetStream.read() != -1) {
                    // Read the rest of the stream
                }
            }
        byte[] bytes = IOUtils.toByteArray(object.getObjectContent());



        HashMap<String, Object> hash = new HashMap<>();
        hash.put("sign", json2);
        hash.put("byte", bytes);
        return hash;

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    private static HttpHeaders httpHeaders(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", token);
        httpHeaders.add("signTitle", token);   // Displayusername
        httpHeaders.add("url", token);
        httpHeaders.add("roleName", token);

        httpHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return httpHeaders;
    }


    @Override
    public byte[] editTemplate1(TemplateDto templateDto, String deptName, String token) throws IOException {

        token = token.replace("Bearer ", "");
        AmazonS3Client awsClient = (AmazonS3Client) awsClientConfig.awsClientConfiguration(token);
        if(!awsClient.doesBucketExistV2(bucket)) {
            CreateBucketRequest withObjectLockEnabledForBucket = new CreateBucketRequest(deptName)
                    .withObjectLockEnabledForBucket(true);
            awsClient.createBucket(withObjectLockEnabledForBucket);
            String policy = bucketPolicy(deptName);
            awsClient.setBucketPolicy(deptName, policy);
        }
        InputStream input1 = awsClient.getObject(bucket, key).getObjectContent();

        try (XWPFDocument doc = new XWPFDocument(input1)) {
            List<XWPFParagraph> xwpfParagraphList = doc.getParagraphs();
            for (XWPFParagraph xwpfParagraph : xwpfParagraphList) {
                for (XWPFRun xwpfRun : xwpfParagraph.getRuns()) {
                    String docText = xwpfRun.getText(0);

                    if (docText != null) {
                        docText = docText.replace("<contact>", templateDto.getContact());

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
                        LocalDateTime localdate = LocalDateTime.now();
                        String date = "";
                        date = localdate.format(formatter);
                        docText = docText.replace("<date>", date);
                        docText = docText.replace("<date> ", date);
                        docText = docText.replace(" <date>", date);
                        docText = docText.replace(" <date> ", date);
                        docText = docText.replace("<address1>", templateDto.getAddress1());
                        docText = docText.replace("<address2>", templateDto.getAddress2());
                        docText = docText.replace("<address3>", templateDto.getAddress3());
                        docText = docText.replace(" <address1> ", templateDto.getAddress1());
                        docText = docText.replace(" <address2> ", templateDto.getAddress2());
                        docText = docText.replace(" <address3> ", templateDto.getAddress3());
                        log.info("date passed " +date);

                        xwpfRun.setText(docText, 0);
                    }
                }
            }

            log.info("entering table");
            // table
            XWPFTable table = doc.getTables().get(0);   // Assuming the table is the first one

            Double costSum = 0.0;
            for (int i = 0; i < templateDto.getQuotationDetails().size(); i++) {
                XWPFTableRow newRow = table.createRow();

                // Populate the columns with data
                newRow.getCell(0).setText(String.valueOf(i + 1));
                String product = templateDto.getQuotationDetails().get(i).getProduct();
                if(product == null) {
                    throw new CustomException(product + " not present", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                String sku = productDescriptionRepo.findByProductTitle(product).getSku();
                newRow.getCell(1).setText(sku);
                newRow.getCell(2).setText(product);
                newRow.getCell(3).setText(templateDto.getQuotationDetails().get(i).getQuantity());
                newRow.getCell(4).setText(templateDto.getQuotationDetails().get(i).getCost());
                Integer cost = Integer.parseInt(templateDto.getQuotationDetails().get(i).getQuantity()) * Integer.parseInt(templateDto.getQuotationDetails().get(i).getCost());
                newRow.getCell(5).setText(String.valueOf(cost));

                costSum += cost;
            }


            //GST
            Double totalCost = 0.0;
            totalCost = (costSum * 0.18) + costSum;
            double gstCost = costSum * 0.18;

            XWPFTableRow secondLastRow = table.createRow();
            secondLastRow.getCell(3).setText("GST");
            secondLastRow.getCell(4).setText("@18%");
            secondLastRow.getCell(5).setText(String.valueOf(gstCost));

            XWPFTableRow lastRow = table.createRow();
            // delete last cell
            XWPFTableCell lastCell = lastRow.getTableCells().get(lastRow.getTableCells().size() - 1);
            lastRow.getCell(3).getCTTc().addNewTcPr().addNewGridSpan().setVal(BigInteger.valueOf(2));  // merging two cells
            lastRow.getCell(3).setText("GRAND TOTAL (inclusive of taxes)");
            lastRow.getCell(4).setText(String.valueOf(totalCost));
            lastRow.getTableCells().remove(lastCell);



            ByteArrayOutputStream b = new ByteArrayOutputStream();
            doc.write(b); // doc should be a XWPFDocument
            InputStream targetStream = new ByteArrayInputStream(b.toByteArray());
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            metadata.setContentLength(targetStream.available());

            PutObjectRequest request1 = new PutObjectRequest(deptName, key, targetStream, metadata);
            awsClient.putObject(request1);
            targetStream.close();  //
            S3Object object = new S3Object();
            try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(deptName, key);
                object = awsClient.getObject(getObjectRequest);

            } catch (Exception e) {
                log.info(e.getMessage());
                throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                while (targetStream != null && targetStream.read() != -1) {
                    // Read the rest of the stream
                }
            }
            byte[] bytes = IOUtils.toByteArray(object.getObjectContent());
            return bytes;

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    // data from leads
    @Override
    public byte[] editTemplate2(Leads leads, String deptName, Double finalDiscount, String templateType, String token) throws IOException {

        token = token.replace("Bearer ", "");
        AmazonS3Client awsClient = (AmazonS3Client) awsClientConfig.awsClientConfiguration(token);

        if(!awsClient.doesBucketExistV2(bucket)) {
            CreateBucketRequest withObjectLockEnabledForBucket = new CreateBucketRequest(deptName)
                    .withObjectLockEnabledForBucket(true);
            awsClient.createBucket(withObjectLockEnabledForBucket);
            String policy = bucketPolicy(deptName);
            awsClient.setBucketPolicy(deptName, policy);
        }
        InputStream input1 = awsClient.getObject(bucket, templateType+".docx").getObjectContent();

        try (XWPFDocument doc = new XWPFDocument(input1)) {
            List<XWPFParagraph> xwpfParagraphList = doc.getParagraphs();
            for (XWPFParagraph xwpfParagraph : xwpfParagraphList) {
                for (XWPFRun xwpfRun : xwpfParagraph.getRuns()) {
                    String docText = xwpfRun.getText(0);

                    if (docText != null) {
                        Account account = accountRepo.findByAccountNameIgnoreCase(leads.getAccount()).get();
                        docText = docText.replace("<contact>", account.getAccountOwner());

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
                        LocalDateTime localdate = LocalDateTime.now();
                        String date = "";
                        date = localdate.format(formatter);
                        docText = docText.replace("<date>", date);
                        docText = docText.replace("<date> ", date);
                        docText = docText.replace(" <date>", date);
                        docText = docText.replace(" <date> ", date);
                        docText = docText.replace("<address1>", account.getAddress1());
                        docText = docText.replace("<address2>", account.getAddress2());
//                        docText = docText.replace("<address3>", templateDto.getAddress3());

                        log.info("date passed " + date);

                        xwpfRun.setText(docText, 0);
                    }
                }
            }

            log.info("entering table");
            // table
            XWPFTable table = doc.getTables().get(0);   // Assuming the table is the first one

            Double costSum = 0.0;
            if (leads.getProducts().size() > 0) {
                for (int i = 0; i < leads.getProducts().size(); i++) {
                    XWPFTableRow newRow = table.createRow();

                    // Populate the columns with data
                    newRow.getCell(0).setText(String.valueOf(i + 1));
                    Products product = leads.getProducts().get(i);
                    if (product == null) {
                        throw new CustomException(product + " not present", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    String sku = productDescriptionRepo.findByProductTitle(product.getProductTitle()).getSku();
                    newRow.getCell(1).setText(sku);
                    newRow.getCell(2).setText(product.getProductTitle());
                    newRow.getCell(3).setText(String.valueOf(product.getQuantity()));
                    newRow.getCell(3).setText(String.valueOf(product.getCost()));
                    Double cost = 0.0;
                    if(product.getDiscount() != null) {
                        Double discount = product.getDiscount() / 100;
                        newRow.getCell(5).setText(String.valueOf(discount)+"%");
                        cost = product.getQuantity() * product.getCost() - ((product.getQuantity() * product.getCost()) * discount);
                    } else {
                        newRow.getCell(5).setText("0%");
                        cost = (double) product.getQuantity() * product.getCost();
                    }

                    newRow.getCell(6).setText(String.valueOf(cost));

                    costSum += cost;
                }
        }   // end of if

            //GST
            Double totalCost = 0.0;
            totalCost = (costSum * 0.18) + costSum;
            double gstCost = costSum * 0.18;

            XWPFTableRow secondLastRow = table.createRow();
            secondLastRow.getCell(4).setText("GST");
            secondLastRow.getCell(5).setText("@18%");
            secondLastRow.getCell(6).setText(String.valueOf(gstCost));

            XWPFTableRow lastRow = table.createRow();
            // delete last cell
            XWPFTableCell lastCell = lastRow.getTableCells().get(lastRow.getTableCells().size() - 1);
            lastRow.getCell(4).getCTTc().addNewTcPr().addNewGridSpan().setVal(BigInteger.valueOf(2));  // merging two cells
            lastRow.getCell(4).setText("GRAND TOTAL (inclusive of taxes)");
            lastRow.getCell(5).setText(String.valueOf(totalCost));
            lastRow.getTableCells().remove(lastCell);



            ByteArrayOutputStream b = new ByteArrayOutputStream();
            doc.write(b); // doc should be a XWPFDocument
            InputStream targetStream = new ByteArrayInputStream(b.toByteArray());
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            metadata.setContentLength(targetStream.available());

            // cretaing new file details list to be saved in lead
//            ArrayList<FileDetails> newFileDetails = new ArrayList<>();
//            int fileDetailsSize = 0;
//            if(leads.getFileDetails() != null) {
//                for(FileDetails fd: leads.getFileDetails()) {
//                    newFileDetails.add(fd);
//                }
//            }
//
//            FileDetails file = new FileDetails();
//            if(leads.getFileDetails() != null) {
//                fileDetailsSize = leads.getFileDetails().size();
//            }
            String url = "/" +leads.getLeadId() + "/quotation" +"/" +templateType+".docx";
//            file.setFileUrl(bucket+url);
//            newFileDetails.add(file);
//            leads.setFileDetails(newFileDetails);
            leads.setQuotationUrl(deptName +"/"+ url);
            leadsRepo.save(leads);

            PutObjectRequest request1 = new PutObjectRequest(deptName, url, targetStream, metadata);
            awsClient.putObject(request1);
            targetStream.close();  //
            S3Object object = new S3Object();
            try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(deptName, url);
                object = awsClient.getObject(getObjectRequest);

            } catch (Exception e) {
                log.info(e.getMessage());
                throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                while (targetStream != null && targetStream.read() != -1) {
                    // Read the rest of the stream
                }
            }
            byte[] bytes = IOUtils.toByteArray(object.getObjectContent());
            return bytes;

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    @Override
    public byte[] invoice(Leads leads, String deptName, List<PaymentMilestone> paymentMilestone, String invoice, String type, String token) throws IOException {
        token = token.replace("Bearer ", "");
        AmazonS3Client awsClient = (AmazonS3Client) awsClientConfig.awsClientConfiguration(token);
        if (!awsClient.doesBucketExistV2(bucket)) {
            CreateBucketRequest withObjectLockEnabledForBucket = new CreateBucketRequest(deptName)
                    .withObjectLockEnabledForBucket(true);
            awsClient.createBucket(withObjectLockEnabledForBucket);
            String policy = bucketPolicy(deptName);
            awsClient.setBucketPolicy(deptName, policy);
        }
        InputStream input1 = awsClient.getObject(bucket, invoice+".docx").getObjectContent();

        try (XWPFDocument doc = new XWPFDocument(input1)) {
            List<XWPFParagraph> xwpfParagraphList = doc.getParagraphs();
            for (XWPFParagraph xwpfParagraph : xwpfParagraphList) {
                for (XWPFRun xwpfRun : xwpfParagraph.getRuns()) {
                    String docText = xwpfRun.getText(0);

                    if (docText != null) {
                        Account account = accountRepo.findByAccountNameIgnoreCase(leads.getAccount()).get();
                        docText = docText.replace("<name>", account.getAccountOwner());

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
                        LocalDateTime localdate = LocalDateTime.now();
                        String date = "";
                        date = localdate.format(formatter);
                        docText = docText.replace("<date>", date);
                        docText = docText.replace("<address1>", account.getAddress1());
                        docText = docText.replace("<address2>", account.getAddress2());
                        log.info("date passed " + date);

                        xwpfRun.setText(docText, 0);
                    }
                }
            }

            log.info("entering table");
            // table
            XWPFTable table = doc.getTables().get(0);   // Assuming the table is the first one
            int i=0;
            for(PaymentMilestone entry: paymentMilestone) {
//                String product = entry.getKey();
//                Products byProductTitle = productDescriptionRepo.findByProductTitle(entry.getProducts().get(i).getProductTitle());
                Products byProductTitle = productDescriptionRepo.findByProductTitle(entry.getName());
                Double value = (Double) entry.getTotalCost();

                XWPFTableRow newRow = table.createRow();
                newRow.getCell(0).setText(entry.getName());
                newRow.getCell(1).setText(leads.getDescription());
                newRow.getCell(2).setText(byProductTitle.getMrp());
                newRow.getCell(3).setText(String.valueOf(value));
                i++;
            }

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            doc.write(b); // doc should be a XWPFDocument
            InputStream targetStream = new ByteArrayInputStream(b.toByteArray());
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            metadata.setContentLength(targetStream.available());

            if(!type.equals("Service")) {
                int productCounter = leads.getProductCounter();
                productCounter++;
                leads.setProductCounter(productCounter);
                leadsRepo.save(leads);
                PutObjectRequest request1 = new PutObjectRequest(deptName, "invoice-" + productCounter, targetStream, metadata);
                awsClient.putObject(request1);
            } else {
                int serviceCounter = leads.getServiceCounter();
                serviceCounter++;
                leads.setProductCounter(serviceCounter);
                leadsRepo.save(leads);
                PutObjectRequest request1 = new PutObjectRequest(deptName, "invoice-" + serviceCounter, targetStream, metadata);
                awsClient.putObject(request1);
            }
//            awsClient.putObject(request1);
            targetStream.close();  //
            S3Object object = new S3Object();
            try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(deptName, key);
                object = awsClient.getObject(getObjectRequest);

            } catch (Exception e) {
                log.info(e.getMessage());
                throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                while (targetStream != null && targetStream.read() != -1) {
                    // Read the rest of the stream
                }
            }
            byte[] bytes = IOUtils.toByteArray(object.getObjectContent());
            return bytes;
        } catch (Exception e) {
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


//package com.crm.serviceImpl;
//
//import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import com.amazonaws.services.s3.model.S3Object;
//import com.crm.config.AWSClientConfigService;
//import com.crm.dto.TemplateDto;
//import com.crm.exception.CustomException;
//import com.crm.service.TemplateService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.IOUtils;
//import org.apache.poi.xwpf.usermodel.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
//@Slf4j
//@Service
//public class TemplateServiceImpl implements TemplateService {
//    @Autowired
//    AWSClientConfigService awsClientConfig;
//
//    private final static String bucket = "template";
//    private final static String key = "Quotation.docx";
//    @Override
//    public byte[] editTemplate(TemplateDto templateDto, String deptName, String token) throws IOException {
//
//        token = token.replace("Bearer ", "");
//        AmazonS3Client awsClient = (AmazonS3Client) awsClientConfig.awsClientConfiguration(token);
//        InputStream input1 = awsClient.getObject(bucket, key).getObjectContent();
//
//        try (XWPFDocument doc = new XWPFDocument(input1)) {
//            List<XWPFParagraph> xwpfParagraphList = doc.getParagraphs();
//            for (XWPFParagraph xwpfParagraph : xwpfParagraphList) {
//                for (XWPFRun xwpfRun : xwpfParagraph.getRuns()) {
//                    String docText = xwpfRun.getText(0);
//
//                    if (docText != null) {
//                        docText = docText.replace("<contact>", templateDto.getContact());
//
//                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
//                        LocalDateTime localdate = LocalDateTime.now();
//                        String date = "";
//                        date = localdate.format(formatter);
//                        docText = docText.replace("<date>", date);
//
////                        docText = docText.replace("<pd1>", templateDto.getPd1());
//
//                        xwpfRun.setText(docText, 0);
//                    }
//                }
//            }
//
//            // table
//            XWPFTable table = doc.getTables().get(0);   // Assuming the table is the first one
//
////            for(int i=0; i<templateDto.getProduct().size(); i++) {
////                XWPFTableRow newRow = table.createRow();
////                XWPFTableCell cell = newRow.getCell(i); // Assuming data goes in the first cell
////                cell.setText(String.valueOf(i+1));
////
////                XWPFTableCell cell = newRow.getCell(i);
////            }
//
//            XWPFTableRow row = table.getRow(1);    // Assuming the field is in the second row
//            // Edit the cell value (customize as needed)
//            if(row.getCell(1) != null)
//                row.getCell(1).setText(templateDto.getPd1());     // Assuming the field is in the second cell
//            if(row.getCell(2) != null)
//                row.getCell(2).setText(templateDto.getQty1());
//            if(row.getCell(3) != null)
//                row.getCell(3).setText(templateDto.getCost1());
//            Integer cost = Integer.parseInt(templateDto.getQty1()) * Integer.parseInt(templateDto.getCost1());
//            if(row.getCell(4) != null)
//                row.getCell(4).setText(cost.toString());
//
//
//            //GST
//            Double totalCost = 0.0;
//            totalCost = (cost * 0.18) + cost;
//            double gstCost = cost * 0.18;
//
//            XWPFTableRow row5 = table.getRow(4);
//            if(row5.getCell(4) != null)
//                row5.getCell(4).setText(String.valueOf(gstCost));
//
//            XWPFTableRow row6 = table.getRow(5);
//            if(row6.getCell(3) != null)
//                row6.getCell(3).setText(totalCost.toString());
//
//
//            ByteArrayOutputStream b = new ByteArrayOutputStream();
//            doc.write(b); // doc should be a XWPFDocument
//            InputStream targetStream = new ByteArrayInputStream(b.toByteArray());
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
//            metadata.setContentLength(targetStream.available());
//
//            PutObjectRequest request1 = new PutObjectRequest(deptName, key, targetStream, metadata);
//            awsClient.putObject(request1);
//
//            S3Object object = new S3Object();
//            try {
//                GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key.replace("%20", " "));
//                object = awsClient.getObject(getObjectRequest);
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            byte[] bytes = IOUtils.toByteArray(object.getObjectContent());
//            return bytes;
//        } catch (Exception e) {
//            log.info(e.getMessage());
//            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//    }
//"list": ["",""]
}