//package com.crm.kafka;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//
//@Service
//public class EsearchProducer {
//
//	@Autowired
//	private KafkaTemplate<String, String> kafkaTemplate;
//
//	String kafkaTopic = "crmfirst";
//
//	public String send(String fid,String fileName,String miniourl) {
//
//		String msg= "{\"fid\":\""+fid+"\","
//				+ " \"fileName\":\""+fileName+"\","
//				+ " \"miniourl\":\""+miniourl+"\"}";
//
//	kafkaTemplate.executeInTransaction(kafkaTemplate -> {
//		kafkaTemplate.send(kafkaTopic, msg);
//		System.out.println("message produced");
//		return true;
//	});
//
//
//	return "OK";
//    }
//}
