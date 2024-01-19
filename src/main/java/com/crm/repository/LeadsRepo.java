package com.crm.repository;

import com.crm.model.Leads;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface LeadsRepo extends MongoRepository<Leads, String> {
    Page<Leads> findAllByAccountOrDescriptionOrLeadTitleOrStatus(String account, String description, String leadTitle, String status, Pageable paging);

    Leads findByLeadId(String leadId);

    @Query(value = "{}", fields = "{'leadTitle': 1}")
    List<Leads> findLeadTitle();

//    @Override
//    Mono<Leads> findById(String s);
}
