package com.crm.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.crm.model.DeptConfigS;

public interface DeptConfigSRepo extends MongoRepository<DeptConfigS, String> {

}
