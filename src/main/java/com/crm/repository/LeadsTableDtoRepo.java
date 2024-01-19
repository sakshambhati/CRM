package com.crm.repository;

import com.crm.dto.LeadsTableDto;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeadsTableDtoRepo extends MongoRepository<LeadsTableDto, String> {
}
