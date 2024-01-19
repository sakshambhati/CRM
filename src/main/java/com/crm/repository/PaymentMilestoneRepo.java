package com.crm.repository;

import com.crm.model.PaymentMilestone;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentMilestoneRepo extends MongoRepository<PaymentMilestone, String> {
}
