//package com.crm.kafka;
//
//import com.crm.repository.AuditRepo;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//@Component
//public class KafkaListeners {
//
//    @Autowired
//    AuditRepo auditRepo;
//
//    @KafkaListener(topics = "audits", groupId="crm")
//    public void CrmListener(AuditMessage auditMessage) {
//        auditRepo.save(auditMessage);
//    }
//
//    @KafkaListener(topics = "crmfirst", groupId="crm")
//    public void CrmListener2(String fid, String fileName, String fileUrl) {
////        auditRepo.save(auditMessage);
//    }
//}
