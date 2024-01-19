package com.crm.repository;

import com.crm.kafka.AuditMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepo extends MongoRepository<AuditMessage, String> {
    AuditMessage findByLeadId(String id);
}
