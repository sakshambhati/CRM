package com.crm.repository;

import com.crm.model.Roles;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RolesRepo extends MongoRepository<Roles, String> {
    Optional<Roles> findByRoleName(String roleName);
}
