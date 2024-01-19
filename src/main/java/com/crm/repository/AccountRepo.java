package com.crm.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.crm.model.Account;

public interface AccountRepo extends MongoRepository<Account, String> {

	Page<Account> findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCityAndDateBetween(
			String accountName, String accountType, String email, String phone, String description, String country,
			String city, LocalDateTime date, LocalDateTime plusDays, Pageable paging);


	Page<Account> findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCity(String accountName,
			String accountType, String email, String phone, String description, String country, String city,
			Pageable paging);

	Optional<Account> findByAccountNameIgnoreCase(String accountName);
}
