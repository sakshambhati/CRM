package com.crm.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository; 
import org.springframework.stereotype.Repository;

import com.crm.model.Contact;

@Repository
public interface CreateContactRepo extends MongoRepository<Contact, String>{

	List<Contact> findByNameContainsOrAccountContainsOrPhoneNoContainsOrEmailContainsOrContactOwnerContains
						(String name, String account, String phoneNo, String email, String contactOwner);

	Contact findByNameContains(Object object);

	Page<Contact> findAllByNameContainsAndAccountContainsAndPhoneNoContainsAndEmailContainsAndContactOwnerContainsAndDateBetween(
			String contactName, String accountName, String phone, String email, String contactOwner, LocalDateTime date,
			LocalDateTime plusDays, Pageable paging);

//	Page<CreateContact> findAllByNameContainsAndAccountContainsAndPhoneNoContainsAndEmailContainsAndContactOwnerContains(
//			String contactName, String accountName, String phone, String email, String contactOwner, Pageable paging);

	List<Contact> findByNameOrAccountOrPhoneNoOrEmailOrContactOwner(String contactName, String accountName,
                                                                    String phone, String email, String contactOwner);

//	Page<CreateContact> findAllByNameAndAccountAndPhoneNoAndEmailAndContactOwner(String contactName, String accountName,
//			String phone, String email, String contactOwner, Pageable paging);

	Page<Contact> findAllByNameOrAccountOrPhoneNoOrEmailOrContactOwner(String contactName, String accountName,
                                                                       String phone, String email, String contactOwner, Pageable paging);

	Page<Contact> findAllByNameOrAccountOrPhoneNoOrEmailOrContactOwnerAndDateBetween(String contactName,
                                                                                     String accountName, String phone, String email, String contactOwner, LocalDateTime date,
                                                                                     LocalDateTime plusDays, Pageable paging);
	

}
