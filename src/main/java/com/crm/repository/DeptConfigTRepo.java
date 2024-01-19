package com.crm.repository;

import com.crm.model.Roles;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.crm.model.DeptConfigT;

import java.util.List;
import java.util.Optional;

public interface DeptConfigTRepo extends MongoRepository<DeptConfigT, String>{

    List<DeptConfigT> findByDeptUsername(String username);

    Optional<DeptConfigT> findByDeptRoleId(String id);
}
