package com.crm.repository;

import com.crm.model.Deal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface DealsRepo extends MongoRepository<Deal, String> {
    List<Deal> findByPoNumberContaining(String poNumber);
    List<Deal> findByDueDate(LocalDate date);
    List<Deal> findByLeadId(Object leadId);

    List<Deal> findByStatus(Object status);
}
