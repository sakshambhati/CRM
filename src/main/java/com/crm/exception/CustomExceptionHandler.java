package com.crm.exception;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

//import com.amazonaws.SdkClientException;
//import com.amazonaws.services.s3.model.AmazonS3Exception;
//import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoWriteException;

@RestControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler{
	
	@Autowired
	private KafkaTemplate<String, HashMap<String, Object>> kafkaTemplate;

	@SuppressWarnings("unchecked")
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ExceptionResponseObject> customException(CustomException customException) {
		HashMap<String, Object> json = new HashMap<String, Object>();
		Date date = new Date();
		HttpStatus status= customException.getHttpStatus();


		ExceptionResponseObject exceptionResponseObjectBuilder = new ExceptionResponseObject();
		exceptionResponseObjectBuilder.setError(customException.getMessage());
		exceptionResponseObjectBuilder.setHttpStatus(status);
		exceptionResponseObjectBuilder.setTime(date);
		exceptionResponseObjectBuilder.setStatusCode(status.value());		
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		json.put("error", exceptionResponseObjectBuilder.getError());
		json.put("serviceName", exceptionResponseObjectBuilder.getServiceName());
		json.put("time", formatter.format(date));
		json.put("httpStatus", status);
//		kafkaTemplate.send("exception", json);
		return new ResponseEntity<ExceptionResponseObject>(exceptionResponseObjectBuilder, status);
	}

//	@SuppressWarnings("unchecked")
//	@ExceptionHandler(AmazonS3Exception.class)
//	public ResponseEntity<ExceptionResponseObject> amazonS3Exception(AmazonS3Exception amazonS3Exception) {
//		System.out.println("AmazonS3Exception");
//		HashMap<String, Object> json = new HashMap<String, Object>();
//		Date date = new Date();
//		HttpStatus failedDependency = HttpStatus.FAILED_DEPENDENCY;
//		
//	
//		ExceptionResponseObject exceptionResponseObjectBuilder = new ExceptionResponseObject();
//		exceptionResponseObjectBuilder.setError(amazonS3Exception.getMessage());
//		exceptionResponseObjectBuilder.setHttpStatus(failedDependency);
//		exceptionResponseObjectBuilder.setTime(date);
//		exceptionResponseObjectBuilder.setStatusCode(failedDependency.value());
//		
////		ExceptionResponseObject exceptionResponseObjectBuilder = ExceptionResponseObject.builder()
////				.message(amazonS3Exception.getMessage()).httpStatus(failedDependency).time(date).build();
//
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		json.put("error", exceptionResponseObjectBuilder.getError());
//		json.put("serviceName", exceptionResponseObjectBuilder.getServiceName());
//		json.put("time", formatter.format(date));
//		json.put("httpStatus", failedDependency);
//		kafkaTemplate.send("exception", json);
//		return new ResponseEntity<ExceptionResponseObject>(exceptionResponseObjectBuilder, failedDependency);
//
//	}
//
//	@SuppressWarnings("unchecked")
//	@ExceptionHandler(AWSSecurityTokenServiceException.class)
//	public ResponseEntity<ExceptionResponseObject> aWSSecurityTokenServiceException(
//			AWSSecurityTokenServiceException aWSSecurityTokenServiceException) {
//		System.out.println("Error connecting minIO with keycloak ");
//		HashMap<String, Object> json = new HashMap<String, Object>();
//		Date date = new Date();
//		HttpStatus failedDependency = HttpStatus.FAILED_DEPENDENCY;
//		
//		ExceptionResponseObject exceptionResponseObjectBuilder = new ExceptionResponseObject();
//		exceptionResponseObjectBuilder.setError(aWSSecurityTokenServiceException.getMessage());
//		exceptionResponseObjectBuilder.setHttpStatus(failedDependency);
//		exceptionResponseObjectBuilder.setTime(date);
//		exceptionResponseObjectBuilder.setStatusCode(failedDependency.value());
//		
////		ExceptionResponseObject exceptionResponseObjectBuilder = ExceptionResponseObject.builder()
////				.message("It seems like your minIO is not integrated with keycloak. Please check your minIO connection.")
////				.httpStatus(failedDependency)
////				.time(date)
////				.build();
//
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		json.put("error", exceptionResponseObjectBuilder.getError());
//		json.put("serviceName", exceptionResponseObjectBuilder.getServiceName());
//		json.put("time", formatter.format(date));
//		json.put("httpStatus", failedDependency);
//		kafkaTemplate.send("exception", json);
//		return new ResponseEntity<ExceptionResponseObject>(exceptionResponseObjectBuilder, failedDependency);
//
//	}
//	
//	
//	@SuppressWarnings("unchecked")
//	@ExceptionHandler(SdkClientException.class)
//	public ResponseEntity<ExceptionResponseObject> sdkClientException(SdkClientException sdkClientException) {
//
//		System.out.println("Exception in minIO connection ");
//		HashMap<String, Object> json = new HashMap<String, Object>();
//		Date date = new Date();
//		HttpStatus failedDependency = HttpStatus.FAILED_DEPENDENCY;
//		ExceptionResponseObject exceptionResponseObjectBuilder = ExceptionResponseObject.builder()
//				.error("There is a problem in minIO connection. Please check your minIO server.")
//				.httpStatus(failedDependency).time(date)
//				.statusCode(failedDependency.value())
//				.build();
//				
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		json.put("error", exceptionResponseObjectBuilder.getError());
//		json.put("serviceName", exceptionResponseObjectBuilder.getServiceName());
//		json.put("time", formatter.format(date));
//		json.put("httpStatus", failedDependency);
//		kafkaTemplate.send("exception", json);
//		return new ResponseEntity<ExceptionResponseObject>(exceptionResponseObjectBuilder, failedDependency);
//
//	}

	@SuppressWarnings("unchecked")
	@ExceptionHandler(MongoTimeoutException.class)
	public ResponseEntity<ExceptionResponseObject> mongoTimeoutException(MongoTimeoutException mongoSocketException) {

		System.out.println("Exception in mongo ");
		HashMap<String, Object> json = new HashMap<String, Object>();
		Date date = new Date();
		HttpStatus failedDependency = HttpStatus.FAILED_DEPENDENCY;
		ExceptionResponseObject exceptionResponseObjectBuilder = ExceptionResponseObject.builder()
				.error("It seems like there is problem in connection application with the Database. Please check your MongoDB connection.")
				.httpStatus(failedDependency)
				.time(date)
				.statusCode(failedDependency.value())
				.build();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		json.put("error", exceptionResponseObjectBuilder.getError());
		json.put("serviceName", exceptionResponseObjectBuilder.getServiceName());
		json.put("time", formatter.format(date));
		json.put("httpStatus", failedDependency);
//		kafkaTemplate.send("exception", json);
		return new ResponseEntity<ExceptionResponseObject>(exceptionResponseObjectBuilder, failedDependency);
	}
	
	@SuppressWarnings("unchecked")
	@ExceptionHandler(DataAccessResourceFailureException.class)
	public ResponseEntity<ExceptionResponseObject> mongoAccess(MongoTimeoutException mongoSocketException) {

		System.out.println("Exception in mongo ");
		HashMap<String, Object> json = new HashMap<String, Object>();
		Date date = new Date();
		HttpStatus failedDependency = HttpStatus.FAILED_DEPENDENCY;
		ExceptionResponseObject exceptionResponseObjectBuilder = ExceptionResponseObject.builder()
				.error("It seems like there is problem in connection application with the Database. Please check your MongoDB connection.")
				.httpStatus(failedDependency)
				.time(date)
				.statusCode(failedDependency.value())
				.build();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		json.put("error", exceptionResponseObjectBuilder.getError());
		json.put("serviceName", exceptionResponseObjectBuilder.getServiceName());
		json.put("time", formatter.format(date));
		json.put("httpStatus", failedDependency);
//		kafkaTemplate.send("exception", json);
		return new ResponseEntity<ExceptionResponseObject>(exceptionResponseObjectBuilder, failedDependency);
	}

	@SuppressWarnings("unchecked")
	@ExceptionHandler(MongoWriteException.class)
	public ResponseEntity<ExceptionResponseObject> mongoWriteException(MongoWriteException mongoWriteException) {

		System.out.println("Exception in writing data in mongo ");
		HashMap<String, Object> json = new HashMap<String, Object>();
		Date date = new Date();
		HttpStatus failedDependency = HttpStatus.FAILED_DEPENDENCY;
		ExceptionResponseObject exceptionResponseObjectBuilder = ExceptionResponseObject.builder()
				.error("It seems like there is problem in writing data into the Database. Please check your MongoDB server.")
				.httpStatus(failedDependency).time(date)
				.statusCode(failedDependency.value())
				.build();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		json.put("error", exceptionResponseObjectBuilder.getError());
		json.put("serviceName", exceptionResponseObjectBuilder.getServiceName());
		json.put("time", formatter.format(date));
		json.put("httpStatus", failedDependency);
//		kafkaTemplate.send("exception", json);
		return new ResponseEntity<ExceptionResponseObject>(exceptionResponseObjectBuilder, failedDependency);
	}
}

