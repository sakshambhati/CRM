package com.crm.controller;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.xray.model.Http;
import com.crm.dto.*;
//import com.crm.email.Email;
import com.crm.kafka.AuditMessage;
import com.crm.kafka.EsearchProducer;
import com.crm.model.*;
import com.crm.repository.*;
import com.crm.service.RetrievalService;
import com.crm.service.TemplateService;
import com.crm.service.TessaractService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.swagger.v3.oas.annotations.Hidden;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.crm.config.AWSClientConfigService;
import com.crm.exception.CustomException;
//import com.crm.model.Country;
//import com.crm.repository.CountryRepo;
//import com.crm.service.DownloadService;
import com.crm.serviceImpl.DownloadServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/crm/api")
public class CrmController {
	
	@Autowired
	CreateContactRepo createContactRepo;	
	
	@Autowired
	CountryCityRepo countryCityRepo;
	
	@Autowired
	AWSClientConfigService awsClientConfigService;
	
	@Autowired
	AccountRepo accountRepo;
	
	@Autowired
	DownloadServiceImpl downloadServiceImpl;
	
	@Autowired
	ProductDescriptionRepo productDescriptionRepo;
	
	@Autowired
	private AWSClientConfigService awsClientConfig;

	@Autowired
	private DeptConfigTRepo deptConfigTRepo;

	@Autowired
	private DeptConfigSRepo deptConfigSRepo;
	@Autowired
	private RolesRepo rolesRepo;

	@Autowired
	private TemplateService templateService;

	@Autowired
	LeadsRepo leadsRepo;

	@Autowired
	LeadsTableDtoRepo leadsTableDtoRepo;

	@Autowired
	KafkaTemplate<String, AuditMessage> kafkaTemplate;

	@Autowired
	AuditRepo auditRepo;

	@Autowired
	RetrievalService retrievalService;

	@Autowired
	TessaractService tessaractService;

	@Autowired
	DealsRepo dealsRepo;

	@Autowired
	PaymentMilestoneRepo paymentMilestoneRepo;

	@Autowired
	EsearchProducer esearchProducer;

	// Hashmap ResponseObj
	private HashMap<String, Object> responseObj = new HashMap<>(2);

	private static final String TOPIC = "audits";
	
	@PostMapping("/createContact")
	public ResponseEntity<?> createContact (
			
			  @RequestParam("name") String name, @RequestParam("account") String account,			  
			  @RequestParam("designation") String designation,			 
			  @RequestParam("email") String email, @RequestParam("phoneNo") String phoneNo, 
			  @RequestParam(required = false, value= "description") String description,
			  @RequestHeader("deptName") String deptName,      // eg 7wgaco
			  @RequestHeader("contactOwner") String contactOwner,
			  @RequestParam("department") String department,
			  @RequestParam(required = false, value="notes") List<String> note,
			  @RequestParam(required = false, value="files") List<MultipartFile> files,
			  HttpServletRequest request,
			  /*@ModelAttribute CreateContactDto contactDtoList,*/
			@RequestHeader("Authorization") String token) {
		
		HashMap<String, Object> hash = new HashMap<String, Object>();
		try {
						
			log.warn("createContact hit");
			System.out.println("notes = "+note);
			
			Random r = new Random(System.currentTimeMillis());
			int fileNumber = 1000 + r.nextInt(20000);

			token = token.replace("Bearer ", "");
			AmazonS3 awsClientConfiguration = awsClientConfigService.awsClientConfiguration(token);

			Contact createContact = new Contact();
			createContact.setName(name);
			createContact.setAccount(account);
			createContact.setDesignation(designation);
			createContact.setEmail(email);
			createContact.setPhoneNo(phoneNo);
			createContact.setDescription(description);
			createContact.setDepartment(department);
			createContact.setDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

			int noteArraySize=0;
			if(note != null)
				 noteArraySize = note.size();
		
			ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
	

			if(noteArraySize > 0) {
				
			for(int i=0; i<noteArraySize; i++) {
				
				// mapping notes with files
				HashMap<String, Object> arrayMap = new HashMap<String, Object>();
				
				if( files != null) {
					if( files.get(i) != null) {
				String putobjectKey = "crm/" + name.toLowerCase().replace("\\r\\n", "") +"-" + fileNumber + 
						 "/"+ files.get(i).getOriginalFilename();
				
				
				     FileDetails fd = new FileDetails();
				     fd.setDeptName(deptName); 
				     fd.setFileName(files.get(i).getOriginalFilename());
				     fd.setFiletype("enclosure");
				     fd.setContentType(files.get(i).getContentType());
				     fd.setFileUrl(putobjectKey);
//				     fd.setUploader(roleName);
				     
				     ObjectMetadata metadata = new ObjectMetadata();
				     metadata.addUserMetadata("content-Type", files.get(i).getContentType());
				     metadata.setContentLength(files.get(i).getSize());
				     
				     if(!awsClientConfiguration.doesBucketExistV2(deptName)) 
				     {
				     	CreateBucketRequest withObjectLockEnabledForBucket = new CreateBucketRequest(deptName)
				     			.withObjectLockEnabledForBucket(true);
				     	
				     	awsClientConfiguration.createBucket(withObjectLockEnabledForBucket);
				     }
				     awsClientConfiguration.putObject(deptName, putobjectKey, files.get(i).getInputStream(), metadata);
				     
				     
				     arrayMap.put("note", note.get(i));
				     arrayMap.put("files", fd);
				     
				     arrayList.add(arrayMap);           // arrayList
			     		     
				     
				}  
			}// end of if
				else {
					 arrayMap.put("note", note.get(i));
				     arrayMap.put("files", null);
				     
				     arrayList.add(arrayMap);           // arrayList
				}
			}
			
			
			createContact.setContact(arrayList);
			}    // end of if above for loop
			
			
			createContact.setFileNumber(fileNumber);	
			createContact.setContactOwner(contactOwner);
			createContactRepo.save(createContact);
			

			hash.put("status", HttpStatus.OK);
			hash.put("message", "Contact saved successfully");
			hash.put("data", createContact);
			
			log.info("exiting create contact");
			return new ResponseEntity<>(hash, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	
	// for All contacts
	@GetMapping("/getContacts")
	public ResponseEntity<?> getAllContacts(@RequestHeader("Authorization") String token) {
		HashMap<String, Object> hash = new HashMap<String, Object>();
		try {
			log.warn("getContacts hit");
			List<Contact> allContacts = createContactRepo.findAll();
		
			hash.put("data", allContacts);
			hash.put("status", HttpStatus.OK);
			return new ResponseEntity<>(hash, HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@GetMapping("/getContactData")
	public ResponseEntity<?> getContactData(@RequestHeader("id") String contactId, @RequestHeader("Authorization") String token) {
		
		try {
			Optional<Contact> findById = createContactRepo.findById(contactId);
			Contact contact = null;
			if(findById.isPresent()) {
				contact = findById.get();
			}
			else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No data found for given id !!");
			}
			return new ResponseEntity<>(contact, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	
	@GetMapping("/country/cities")
	public ResponseEntity<?> getCountryCityData(@RequestHeader("country") String country, @RequestHeader(required = false, value="city") String pattern,
			@RequestHeader("Authorization") String token) {
		
		try {
			if(pattern == "") {
				List<CountryCity> findAll = countryCityRepo.findAllByCountryStartingWithIgnoreCase(country);
				Set<String> collect = findAll.parallelStream().map(CountryCity::getCountry).collect(Collectors.toSet());
	
				return new ResponseEntity<>(collect, HttpStatus.OK);
			}
			List<CountryCity> cityList = countryCityRepo.findAllByCountryIgnoreCaseAndCityStartingWithIgnoreCase(country, pattern);
			Set<String> collect = cityList.parallelStream().map(CountryCity::getCity).collect(Collectors.toSet());
			
			return new ResponseEntity<>(collect, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@GetMapping("/cityData")
	public ResponseEntity<?> getCountryCity(@RequestHeader("country") String country,
			@RequestHeader("Authorization") String token) {
		
	try {
			List<CountryCity> cityList = countryCityRepo.findAllByCountryIgnoreCase(country);
			Set<String> collect = cityList.parallelStream().map(CountryCity::getCity).collect(Collectors.toSet());
	
			return new ResponseEntity<>(collect, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@DeleteMapping("/deleteContact")
	public ResponseEntity<?> deleteContact(@RequestBody List<String> contactIdLst, 
			@RequestHeader("Authorization") String token) {
		
		HashMap<String, Object> hash = new HashMap<String, Object>();
		try 
		{
			log.warn("deleteContact hit");
//			Optional<CreateContact> findById = createContactRepo.findById(contactId);
//			if(findById.isPresent()) {
//				createContactRepo.deleteById(contactId);
//				return ResponseEntity.ok("Contact has been deleted");
//			}
//			else {
//				return ResponseEntity.internalServerError().body("Contact not present");
//			}
			boolean []flag = {true};
			contactIdLst.forEach(id->{
				Optional<Contact> findById = createContactRepo.findById(id);
				if(!findById.isPresent()) {
					flag[0] = false;
				}
			});
			if(flag[0])
			{
				createContactRepo.deleteAllById( contactIdLst);
				hash.put("message", "Contact has been deleted");
				hash.put("status", HttpStatus.OK);
				hash.put("data", createContactRepo.findAll());
				return new ResponseEntity<>(hash, HttpStatus.OK);
//				return ResponseEntity.ok("Contact has been deleted");
			}
			else
			{
				hash.put("message", "Contact not present");
				hash.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
//				return ResponseEntity.internalServerError().body("Contact not present");
				return new ResponseEntity<>(hash, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	
	@PostMapping("/editContact")
	@Lazy
	public ResponseEntity<?> editContact(@RequestHeader("id") String contactId, @RequestParam("name") String name, 
			@RequestParam("account") String account,			  
			  @RequestParam("designation") String designation,			 
			  @RequestParam("email") String email, @RequestParam("phoneNo") String phoneNo, 
			  @RequestParam(required = false, value= "description") String description,
			  @RequestHeader("deptName") String deptName,      // eg 7wgaco
			  @RequestHeader("contactOwner") String contactOwner,
			  @RequestParam("department") String department,
			 @RequestParam(required = false, value="notes") List<String> note,
			  @RequestParam(required = false, value="files") List<MultipartFile> files,
			@RequestHeader("Authorization") String token) {
		
		try {
			HashMap<String, Object> hash = new HashMap<String, Object>();
			Optional<Contact> findById = createContactRepo.findById(contactId);
			Contact contact = null;
			if(findById.isPresent()) {
				 contact = findById.get();
				 
				 contact.setDepartment(department);
				 contact.setDescription(description);
				 contact.setName(name);
				 contact.setDesignation(designation);
				 contact.setPhoneNo(phoneNo);
				 contact.setEmail(email);
				 contact.setAccount(account);
				 
				 int noteArraySize = note.size();
				 
				token = token.replace("Bearer ", "");
				AmazonS3 awsClientConfiguration = awsClientConfigService.awsClientConfiguration(token);
					
				ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
				
					if(noteArraySize > 0) {
						for(int i=0; i<noteArraySize; i++) {
							
							// mapping notes with files
							HashMap<String, Object> arrayMap = new HashMap<String, Object>();
							
							if( files != null) {
								if( files.get(i) != null) {
							String putobjectKey = "crm/" + name.toLowerCase().replace("\\r\\n", "") +"-" + contact.getFileNumber() + 
									 "/"+  files.get(i).getOriginalFilename();
							
							
							     FileDetails fd = new FileDetails();
							     fd.setDeptName(deptName); 
							     fd.setFileName(files.get(i).getOriginalFilename());
							     fd.setFiletype("enclosure");
							     fd.setContentType(files.get(i).getContentType());
							     fd.setFileUrl(putobjectKey);
//							     fd.setUploader(roleName);
							     
							     ObjectMetadata metadata = new ObjectMetadata();
							     metadata.addUserMetadata("content-Type", files.get(i).getContentType());
							     metadata.setContentLength(files.get(i).getSize());
							     
							     if(!awsClientConfiguration.doesBucketExistV2(deptName)) 
							     {
							     	CreateBucketRequest withObjectLockEnabledForBucket = new CreateBucketRequest(deptName)
							     			.withObjectLockEnabledForBucket(true);
							     	
							     	awsClientConfiguration.createBucket(withObjectLockEnabledForBucket);
							     }
							     awsClientConfiguration.putObject(deptName, putobjectKey, files.get(i).getInputStream(), metadata);
							     
							     
							     arrayMap.put("note", note.get(i));
							     arrayMap.put("files", fd);
							     
							     arrayList.add(arrayMap);           // arrayList
						     		     
							     
							}  
						}// end of if
							else {
								 arrayMap.put("note", note.get(i));
							     arrayMap.put("files", contact.getContact().get(i).get("files"));      // prob
							     
							     arrayList.add(arrayMap);           // arrayList
							}
						}
						
						
						contact.setContact(arrayList);
					}    // end of if above for loop
				 
				 Contact save = createContactRepo.save(contact);
				 
				hash.put("data", save);
				hash.put("message", "Contact Details has been edited");
				hash.put("status", HttpStatus.OK);
				
				 return ResponseEntity.ok(hash);
			}
			else {
				return ResponseEntity.internalServerError().body("Contact not present");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	

	@PostMapping("/test")
	public String test1(@RequestHeader(required = false, value="Authorization") String token, @RequestBody String str)  {
//		AmazonS3 amazonS3 =  awsClientConfig.awsClientConfiguration(token);
		str.replaceAll("[^A-Za-z]","");
		String s2 = str.toLowerCase();
		Map<Character, Integer> hash = new HashMap<>();
//		String[] split = str.split(" ");
		char[] s1 = s2.replace(" ", "").toCharArray();
		for(char s: s1) {
			if(hash.containsKey(s)) {
				hash.put(s, hash.get(s)+1);
			} else {
				hash.put(s, 1);
			}
		}
		for(Map.Entry<Character, Integer> map: hash.entrySet()) {
			System.out.println(map.getKey() + " : " +map.getValue());
		}
		return "ok";
	}
	
	
	
	@PostMapping("/addAccount")
	@Lazy
	public ResponseEntity<?> addAccount(@RequestBody Account account,
			@RequestHeader("Authorization") String token) {
		
		try {
			HashMap<String, Object> hash = new HashMap<>();

			Optional<Account> byAccountName = accountRepo.findByAccountNameIgnoreCase(account.getAccountName());
			if(byAccountName.isPresent()) {
				throw new CustomException("Account Name already exists !", HttpStatus.INTERNAL_SERVER_ERROR);
			}

			Account accountSaved = accountRepo.save(account);

			hash.put("data", accountSaved);
			hash.put("message", "Account has been saved successfully");
			hash.put("status", HttpStatus.OK);
			return new ResponseEntity<> (hash, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	
	
	@PatchMapping("/editAccount")
	@Lazy
	public ResponseEntity<?> editAccount(@RequestBody Account account, @RequestHeader("id") String accountId,
			@RequestHeader("Authorization") String token) {
		
		try {		
			HashMap<String, Object> hash = new HashMap<>();
			Optional<Account> findById = accountRepo.findById(accountId);
			if(findById.isPresent()) {
				Account accountInDatabase = findById.get();
				
				accountInDatabase.setAccountName(account.getAccountName());
				accountInDatabase.setAccountType(account.getAccountType());
				accountInDatabase.setAddress1(account.getAddress1());	
				accountInDatabase.setAddress2(account.getAddress2());
				accountInDatabase.setEmail(account.getEmail());
				accountInDatabase.setPhone(account.getPhone());
				accountInDatabase.setDescription(account.getDescription());
				accountInDatabase.setCountry(account.getCountry());
				accountInDatabase.setCity(account.getCity());
				accountInDatabase.setAccountOwner(account.getAccountOwner());
				
				Account accountSave = accountRepo.save(accountInDatabase);
				hash.put("data", accountSave);
			}
	
			hash.put("message", "Account Details have been updated");
			hash.put("status", HttpStatus.OK);
			return new ResponseEntity<> (hash, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

	
	
	@DeleteMapping("/deleteAccount")
	public ResponseEntity<?> deleteAccount(@RequestBody List<String> accountIds,
			@RequestHeader("Authorization") String token) {
		
		try {
			log.info("account ids = "+accountIds);

			accountRepo.deleteAllById(accountIds);
			return ResponseEntity.status(HttpStatus.OK).body(accountRepo.findAll());
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	
	@GetMapping("/viewAccountData")
	@Lazy
	public ResponseEntity<?> getAccountData(@RequestHeader("id") String accountId,
			@RequestHeader("Authorization") String token) {
		
		try {
			HashMap<String, Object> hash = new HashMap<>();
			Optional<Account> findById = accountRepo.findById(accountId);
			if(findById.isPresent()) {
				Account account = findById.get();
				
				hash.put("data", account);
//				hash.put("message", "Account has been saved successfully");
				hash.put("status", HttpStatus.OK);
				
				return new ResponseEntity<> (hash, HttpStatus.OK);
			}
			else {
				throw new IllegalArgumentException("No Data present");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
		
	
	
	@PostMapping("/fetchAllProducts")
	public ResponseEntity<?> getAllProducts(@RequestHeader("page") int page, @RequestHeader("size") int size,
			@RequestHeader("Authorization") String token, HttpServletRequest request, @RequestBody FilterSort filterSort ) {	
		
		try {
			
		log.info("fetchAllFiles >>>");
		responseObj.clear();
		
		HashMap<String, String> filter = filterSort.getFilter();
		HashMap<String, String> sort = filterSort.getSort();
		
		
		
		// filter sorting
		String sku="";
//		LocalDateTime createdOnDate=null;
		String productTitle = "";
		String description="";
		String mrp="";
		
		// 1
		if(filter == null && sort == null) {
			
		Pageable paging = PageRequest.of(page, size, Sort.by("date").descending()); 
		Page<Products> findAll = productDescriptionRepo.findAll(paging);
		
		responseObj.put("length", findAll.getTotalElements());
		responseObj.put("content", findAll.getContent());
		log.info("fetchAllFiles <<<");
		
		return ResponseEntity.ok(responseObj);
		}

		
		// 2
		if(filter != null && sort == null) {
					
			
			if(filter.get("skuNo") != null) {
				 sku = filter.get("skuNo");
			}		
			if(filter.get("productTitle") != null) {
				productTitle = filter.get("productTitle");
			}
			if(filter.get("Description") != null) {
				description = filter.get("Description");
			}
			if(filter.get("MRP") != null) {
				mrp = filter.get("MRP");
			}
			if(filter.get("date") != null) {
				 
				 System.out.println(filter.get("date"));
				 DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
				 LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
				 LocalDateTime plusDays = date.plusDays(1);
				 Pageable paging = PageRequest.of(page, size);
					
				 Page<Products> filterBySubject = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrpAndDateBetween(
						 sku,  productTitle,  description,  mrp, date , plusDays,    paging);  // date=from   plusDays=to
					
					responseObj.put("length", filterBySubject.getTotalElements());
					responseObj.put("content", filterBySubject.getContent());
					
					return ResponseEntity.ok(responseObj);
			}
			
			Pageable paging = PageRequest.of(page, size);
			Page<Products> filterBySubject = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrp(sku,  productTitle,  description, mrp, paging);
			
			responseObj.put("length", filterBySubject.getTotalElements());
			responseObj.put("content", filterBySubject.getContent());
			
			return ResponseEntity.ok(responseObj);
		}
		
		
		// 3
		if(filter == null && sort != null) {
			String sortType = sort.get("type");
			String field = sort.get("title");
			
			// ascending
			if(sortType.equals("Asc")) {
			Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending()); 
			Page<Products> sorted = productDescriptionRepo.findAll(paging);
			
			responseObj.put("length", sorted.getTotalElements());
			responseObj.put("content", sorted.getContent());
			}
			
			// descending
			if(sortType.equals("Desc")) {
				Pageable paging = PageRequest.of(page, size, Sort.by(field).descending()); 
				Page<Products> sorted = productDescriptionRepo.findAll(paging);
				
				responseObj.put("length", sorted.getTotalElements());
				responseObj.put("content", sorted.getContent());
				}			
			
			return ResponseEntity.ok(responseObj);
		}
		
		
		// 4
		if(filter != null && sort != null) {
			

			if(filter.get("skuNo") != null) {
				 sku = filter.get("skuNo");
			}
			if(filter.get("Description") != null) {
				description = filter.get("Description");
			}
			if(filter.get("productTitle") != null) {
				productTitle = filter.get("productTitle");
			}
			if(filter.get("MRP") != null) {
				mrp = filter.get("MRP");
			}
			
			String sortType = sort.get("type");
			String field = sort.get("title");
			
			if(sortType.equals("Asc") && filter.get("date") != null) {
				Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending()); 
				DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
				LocalDateTime date = LocalDateTime.parse(filter.get("createdOnDate") +" 00:00", forPattern);
				LocalDateTime plusDays = date.plusDays(1);
				
				
				Page<Products> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrpAndDateBetween(
						 sku,  productTitle,  description,  mrp, date , plusDays,  paging);  // date=from 
			
//			Page<RTIFile> sorted = rtiFileRepository.findAllBySubjectContainsAndCreatedOnDateContainsAndStatusContainsAndPriorityContains(subject, createdOnDate, status, priority, paging);
			
			responseObj.put("length", sorted.getTotalElements());
			responseObj.put("content", sorted.getContent());
			}
			if(sortType.equals("Asc") && filter.get("date") == null) {
				Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending()); 
				Page<Products> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrp(sku, productTitle, description, mrp, paging);
				
				responseObj.put("length", sorted.getTotalElements());
				responseObj.put("content", sorted.getContent());
			}
			
			if(sortType.equals("Desc") && filter.get("date") != null) {
				
				Pageable paging = PageRequest.of(page, size, Sort.by(field).descending()); 
				DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
				LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
				LocalDateTime plusDays = date.plusDays(1);
				
				Page<Products> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrpAndDateBetween(sku, productTitle, description, mrp, date, plusDays, paging);
				
				responseObj.put("length", sorted.getTotalElements());
				responseObj.put("content", sorted.getContent());
				
				
			}
			if(sortType.equals("Desc") && filter.get("date") == null) {
				Pageable paging = PageRequest.of(page, size, Sort.by(field).descending()); 
				Page<Products> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrp(sku, productTitle, description, mrp, paging);
				
				responseObj.put("length", sorted.getTotalElements());
				responseObj.put("content", sorted.getContent());
			}
			return ResponseEntity.ok(responseObj);
		}
		
		} catch (Exception e) {  
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return ResponseEntity.internalServerError().body("Error in fetching files");
	}
	
	
	
	// Front page data on front end
	@PostMapping("/fetchAllAccounts")
	public ResponseEntity<?> getAllAccount(@RequestHeader("page") int page, @RequestHeader("size") int size,
		@RequestHeader("Authorization") String token, HttpServletRequest request, @RequestBody FilterSort filterSort ) {
				
				try {
					
				log.info("fetchAllAccount >>>");
				responseObj.clear();
				
				HashMap<String, String> filter = filterSort.getFilter();
				HashMap<String, String> sort = filterSort.getSort();
				
				
				
				// filter sorting
				String accountType="";
				String accountName = "";
				String phone="";
				String email="";
				String description="";
				String country = "";
				String city = "";
				
				
				// 1
				if(filter == null && sort == null) {
					
				Pageable paging = PageRequest.of(page, size, Sort.by("date").descending()); 
				Page<Account> findAll = accountRepo.findAll(paging);		
				
				responseObj.put("length", findAll.getTotalElements());
				responseObj.put("content", findAll.getContent());
				log.info("fetchAllFiles <<<");
				
				return ResponseEntity.ok(responseObj);
				}

				
				// 2
				if(filter != null && sort == null) {
							
					
					if(filter.get("accountType") != null) {
						accountType = filter.get("accountType");
					}		
					if(filter.get("accountName") != null) {
						accountName = filter.get("accountName");
					}
					if(filter.get("phone") != null) {
						phone = filter.get("phone");
					}
					if(filter.get("email") != null) {
						email = filter.get("email");
					}
					if(filter.get("description") != null) {
						description = filter.get("description");
					}
					if(filter.get("country") != null) {
						country = filter.get("country");
					}
					if(filter.get("city") != null) {
						city = filter.get("city");
					}
					if(filter.get("date") != null) {
						 
						 System.out.println(filter.get("date"));
						 DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
						 LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
						 LocalDateTime plusDays = date.plusDays(1);
						 Pageable paging = PageRequest.of(page, size);
							
						 Page<Account> filterBySubject = accountRepo.findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCityAndDateBetween(
								 accountName,  accountType,  email, phone, description, country, city, date , plusDays, paging);  // date=from   plusDays=to
							
							responseObj.put("length", filterBySubject.getTotalElements());
							responseObj.put("content", filterBySubject.getContent());
							
							return ResponseEntity.ok(responseObj);
					}
					
					Pageable paging = PageRequest.of(page, size);
					
					Page<Account> filterBySubject = accountRepo.findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCity(accountName,  accountType,  email, phone, description, country, city, paging);
				
					System.out.println("size "+filterBySubject.getTotalElements());
					responseObj.put("length", filterBySubject.getTotalElements());
					responseObj.put("content", filterBySubject.getContent());
					
					return ResponseEntity.ok(responseObj);
				}
				
				
				// 3
				if(filter == null && sort != null) {
					String sortType = sort.get("type");
					String field = sort.get("title");
					
					// ascending
					if(sortType.equals("Asc")) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending()); 
					Page<Account> sorted = accountRepo.findAll(paging);
					
					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
					}
					
					// descending
					if(sortType.equals("Desc")) {
						Pageable paging = PageRequest.of(page, size, Sort.by(field).descending()); 
						Page<Account> sorted = accountRepo.findAll(paging);
						
						responseObj.put("length", sorted.getTotalElements());
						responseObj.put("content", sorted.getContent());
						}			
					
					return ResponseEntity.ok(responseObj);
				}
				
				
				// 4
				if(filter != null && sort != null) {
					

					if(filter.get("accountType") != null) {
						accountType = filter.get("accountType");
					}		
					if(filter.get("accountName") != null) {
						accountName = filter.get("accountName");
					}
					if(filter.get("phone") != null) {
						phone = filter.get("phone");
					}
					if(filter.get("email") != null) {
						email = filter.get("email");
					}
					if(filter.get("description") != null) {
						description = filter.get("description");
					}
					if(filter.get("country") != null) {
						country = filter.get("country");
					}
					if(filter.get("city") != null) {
						city = filter.get("city");
					}
					
					String sortType = sort.get("type");
					String field = sort.get("title");
					
					if(sortType.equals("Asc") && filter.get("date") != null) {
						Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending()); 
						DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
						LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
						LocalDateTime plusDays = date.plusDays(1);
						
						
						Page<Account> sorted = accountRepo.findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCityAndDateBetween(
								accountName,  accountType,  email, phone, description, country, city,  date , plusDays,  paging);  // date=from 
					
//					Page<RTIFile> sorted = rtiFileRepository.findAllBySubjectContainsAndCreatedOnDateContainsAndStatusContainsAndPriorityContains(subject, createdOnDate, status, priority, paging);
					
					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
					}
					if(sortType.equals("Asc") && filter.get("date") == null) {
						Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending()); 
						Page<Account> sorted = accountRepo.findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCity(accountName,  accountType,  email, phone, description, country, city, paging);
						
						responseObj.put("length", sorted.getTotalElements());
						responseObj.put("content", sorted.getContent());
					}
					
					if(sortType.equals("Desc") && filter.get("date") != null) {
						
						Pageable paging = PageRequest.of(page, size, Sort.by(field).descending()); 
						DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
						LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
						LocalDateTime plusDays = date.plusDays(1);
						
						Page<Account> sorted = accountRepo.findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCityAndDateBetween
								(accountName,  accountType,  email, phone, description, country, city, date, plusDays, paging);
						
						responseObj.put("length", sorted.getTotalElements());
						responseObj.put("content", sorted.getContent());
						
						
					}
					if(sortType.equals("Desc") && filter.get("date") == null) {
						Pageable paging = PageRequest.of(page, size, Sort.by(field).descending()); 
						Page<Account> sorted = accountRepo.findByAccountNameOrAccountTypeOrEmailOrPhoneOrDescriptionOrCountryOrCity(accountName,  accountType,  email, phone, description, country, city, paging);
						
						responseObj.put("length", sorted.getTotalElements());
						responseObj.put("content", sorted.getContent());
					}
					return ResponseEntity.ok(responseObj);
				}
				
				} catch (Exception e) {  
					e.printStackTrace();
					throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				return ResponseEntity.internalServerError().body("Error in fetching files");
			}
			
	
	
	@PostMapping("/search") 
	public ResponseEntity<?> getSearchedData(@RequestBody HashMap<String, Object> search,
			@RequestHeader("Authorization") String token) {
		
		try {
			log.info("Entering search");
			HashMap<String, Object> hash = new HashMap<String, Object>();
			
			String contactName = "";
			String accountName = "";
			String phone = "";
			String email = "";
			String contactOwner = "";
			
			if(search.get("ContactName") != null) {
				contactName = (String) search.get("ContactName");
			}
			
			if(search.get("accountName") != null) {
				accountName = (String) search.get("accountName");
			}
			
			if(search.get("phone") != null) {
				phone = (String) search.get("phone");
			}
			
			if(search.get("email") != null) {
				email = (String) search.get("email");
			}
			
			if(search.get("contactOwner") != null) {
				contactOwner = (String) search.get("contactOwner");
			}
			
			System.out.println("contact name "+ contactName);
			List<Contact> list = createContactRepo.findByNameOrAccountOrPhoneNoOrEmailOrContactOwner(contactName, accountName, phone, email, contactOwner);
			
			hash.put("data", list);
//			hash.put("message", "Account has been saved successfully");
			hash.put("status", HttpStatus.OK);
			
			return new ResponseEntity<> (hash, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	// for preview
	@Transactional
	@SuppressWarnings("resource")
	@GetMapping("/url")
	public ResponseEntity<?> uploadUrls(@RequestParam String urls, @RequestHeader(required=false, value="sessionId") String sessionId,
			HttpServletRequest request) throws Exception {
		try {
			System.out.println("url is  " + urls);
			String token = request.getHeader("Authorization").replace("Bearer ", "");
			AmazonS3Client awsClient = (AmazonS3Client) awsClientConfig.awsClientConfiguration(token);
			S3Object object = new S3Object();
			String[] split = null;
			String[] split2 = urls.split("\\?");
			String bucket = "";
			String key = "";
			if (urls.startsWith("http")) {
				split = split2[0].split("/", 5);
				bucket = split[3];
				key = split[4];
			} else {
				split = split2[0].split("/", 2);
				bucket = split[0];
				key = split[1];
			}

			log.info("length==>>" + split2.length);
			log.info("url===>>" + urls);

			if (split2.length == 1) {
				object = awsClient.getObject(bucket, key);
			} else {
				String versionId = split2[1].split("=")[1];
				log.info(versionId);
				System.out.println(split[0]);
				System.out.println(split[1]);
				try {
					GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key.replace("%20", " "),
							versionId);
					object = awsClient.getObject(getObjectRequest);
				} catch (Exception e) {
					e.printStackTrace();
					throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
			byte[] obj = IOUtils.toByteArray(object.getObjectContent());
			System.out.println("obj = "+ obj);
			return new ResponseEntity<byte[]>(obj, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	
	// download file
	@GetMapping("/downloadFile")
	public ResponseEntity<?> downloadFile(@RequestHeader("id") String contactId, @RequestHeader("fileUrl") String fileUrl,
			@RequestHeader("deptName") String deptName, @RequestHeader("Authorization") String token)
			throws IOException {
		
		try {
			log.info("Download hit");
			AuditMessage auditObj = new AuditMessage();
			auditObj.setAction("File Downloaded");
			auditObj.setMessage("File Downloaded by dept "+deptName);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
			String formattedDate = auditObj.getLocalDateTime().format(formatter);
			auditObj.setDate(formattedDate);

			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
			String time = auditObj.getLocalDateTime().format(timeFormatter);
			auditObj.setTime(time);

			kafkaTemplate.send(TOPIC, auditObj);
			return downloadServiceImpl.getDownloadedfile(contactId, fileUrl, deptName, token);
			
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	
	
	    // PRODUCTS PAGE
		@PostMapping("/addProduct")
		public ResponseEntity<?> productDescription(@RequestParam("productTitle") String productTitle, @RequestParam("MRP") String mrp,
				@RequestParam("description") String description, @RequestParam("hsn") String hsn, @RequestParam("type") String type,
				@RequestHeader("Authorization") String token)
			{
			
			try {
				log.info("productDescription");
				HashMap<String, Object> hash = new HashMap<>();
				Products pd = new Products();
				Random random = new Random(System.currentTimeMillis());
				long rd = random.nextInt(900) + 100;       // generate nos till 899 and + 100
				
				pd.setSku(productTitle.substring(0, 3).toUpperCase() + rd);
				pd.setSku(type.charAt(0) + productTitle.substring(1,3).toUpperCase() + rd);
				pd.setProductTitle(productTitle);
				pd.setDescription(description);
				pd.setMrp(mrp);
				pd.setDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
				pd.setHsn(hsn);
				pd.setProductType(type);

				productDescriptionRepo.save(pd);
				
				hash.put("data", pd);
				hash.put("status", HttpStatus.OK);
				hash.put("message", "Product has been added successfully");
				return new ResponseEntity<> (hash, HttpStatus.OK);
			} catch (Exception e) {
				e.printStackTrace();
				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		}
		
		
		
		@PostMapping("/editProductDescription")
		public ResponseEntity<?> editProductDescription(@RequestParam("productTitle") String productTitle, @RequestParam("MRP") String mrp,
				@RequestParam("description") String description, @RequestHeader("id") String productId, @RequestParam("HSN") String hsn, @RequestParam("type") String type,
				@RequestHeader("Authorization") String token)
			{
			
				try {
					log.info("editProductDescription");
					Products pd = productDescriptionRepo.findById(productId).orElseThrow(() -> new RuntimeException("No Value Present"));
					pd.setDescription(description);
					pd.setMrp(mrp);
					pd.setProductTitle(productTitle);
					pd.setHsn(hsn);
					pd.setProductType(type);

					productDescriptionRepo.save(pd);
					
					return new ResponseEntity<> (pd, HttpStatus.OK);
				} catch (Exception e) {
					log.info(e.getMessage());
					throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				
			}
		
		
		
		@GetMapping("/getAllProductDescription")
		public ResponseEntity<?> getAllProductDescription(@RequestHeader("Authorization") String token)  {
			
			try {
				log.info("getAllProductDescription");
				List<Products> findAll = productDescriptionRepo.findAll();
				return new ResponseEntity<> (findAll, HttpStatus.OK);
			} catch (Exception e) {
				log.info(token);
				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		}
		
		
		
		@DeleteMapping("/deleteProduct")
		public ResponseEntity<?> deleteProduct(@RequestHeader("id") String productId, @RequestHeader("Authorization") String token)  {
			
			try {
				log.info("deleteProduct");
				productDescriptionRepo.deleteById(productId);
				return new ResponseEntity<> ("Product has been deleted", HttpStatus.OK);
			} catch (Exception e) {
				log.info(token);
				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		}
		
		
		
		@DeleteMapping("/deleteMultipleProducts")
		public ResponseEntity<?> deleteMultipleProducts(@RequestBody List<String> productId,
				@RequestHeader("Authorization") String token)  {
			
			try {
				log.info("deleteMultipleProducts");
				log.info("product ids = "+productId);
				productDescriptionRepo.deleteAllById(productId);
				return new ResponseEntity<> (productDescriptionRepo.findAll(), HttpStatus.OK);
			} catch (Exception e) {
				log.info(token);
				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		}


	// get single product
	@GetMapping("/getProduct")
	public ResponseEntity<Products> getProduct(@RequestHeader("id") String productId) {

		try {
			log.info("getProduct");
			Products prod = productDescriptionRepo.findById(productId).orElseThrow(() -> new NullPointerException(productId));
			return new ResponseEntity<> (prod, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}


	@GetMapping("/getProductByName")
	public ResponseEntity<Products> getProductByName(@RequestHeader("name") String productName, @RequestHeader("Authorization") String token) {

		try {
			log.info("getProductByName");
			Products byProductTitle = productDescriptionRepo.findByProductTitle(productName);
//			ProductDescription prod = productDescriptionRepo.findById(productId).orElseThrow(() -> new NullPointerException(productId));
			return new ResponseEntity<> (byProductTitle, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}


		@PostMapping("/bulkUpdate")       // products
		public ResponseEntity<?> bulkUpdate(@RequestHeader(required =false, value="MRP", defaultValue = "") String mrp, @RequestHeader(required =false, value="Description", defaultValue = "") String description,
				@RequestHeader(required =false, value="productTitle", defaultValue = "") String productTitle, @RequestHeader(required =false, value="type", defaultValue = "") String type,
				@RequestHeader(required =false, value="HSN", defaultValue = "") String hsn, @RequestBody(required = false) List<String> ids, @RequestHeader("Authorization") String token)  {
			
			try {
				log.info("Bulk update ");
				if(ids.size() > 0) {
					
					Iterable<Products> findAllById = productDescriptionRepo.findAllById(ids);
					
					for (Products pds : findAllById) {
						if(!mrp.isEmpty()) {
							pds.setMrp(mrp);
							}
						
						if(!description.isEmpty()) {
							pds.setDescription(description);
							}
						
						if(!productTitle.isEmpty()) {
							pds.setProductTitle(token);
							}
						if(!hsn.isEmpty()) {
							pds.setHsn(hsn);
						}
						if(!type.isEmpty()) {
							pds.setProductType(type);
						}
					}
					
					productDescriptionRepo.saveAll(findAllById);
					
//					Stream<ProductDescription> productDescriptionStream = StreamSupport.stream(findAllById.spliterator(), false);
//					
//					productDescriptionStream.forEach(pds -> {
//						if(!mrp.isEmpty()) {
//							pds.setMrp(mrp);
//							}
//						
//						if(!description.isEmpty()) {
//							pds.setDescription(description);
//							}
//						
//						if(!productTitle.isEmpty()) {
//							pds.setProductTitle(token);
//							}
//					});
					
//					productDescriptionRepo.saveAll(findAllById);
					
					log.info("Exit Bulk update ");
					return new ResponseEntity<> (findAllById, HttpStatus.OK);
					
				} else {
					
				List<Products> findAll = productDescriptionRepo.findAll();
				findAll.forEach(pd -> {
					if(!mrp.isEmpty()) {
						pd.setMrp(mrp);
						}
					
					if(!description.isEmpty()) {
						pd.setDescription(description);
						}
					
					if(!productTitle.isEmpty()) {
						pd.setProductTitle(token);
					}
					if(!hsn.isEmpty()) {
						pd.setHsn(hsn);
					}
					if(!hsn.isEmpty()) {
						pd.setProductType(type);
					}
				});
				productDescriptionRepo.saveAll(findAll);
				return new ResponseEntity<> (findAll, HttpStatus.OK);
				}
			} catch (Exception e) {
				log.info(e.getMessage());
				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		
	
		@PostMapping("/bulkUpdateContacts")
		public ResponseEntity<?> bulkUpdateContacts(@RequestHeader(required =false, value="accountOwner") String accountOwner,
		@RequestBody List<String> ids, @RequestHeader("Authorization") String token)  {
			
			try {
				log.info("bulk update contacts hit");
				List<Contact> findAll = createContactRepo.findAll();
				findAll.forEach(pd -> {
//					pd.setDepartment(department);
//					pd.setDesignation(designation);
//					pd.setName(contactName);
					pd.setAccount(accountOwner);
				});
				createContactRepo.saveAll(findAll);
				return new ResponseEntity<> (findAll, HttpStatus.OK);
			} catch (Exception e) {
				log.info(e.getMessage());
				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		}


	@PostMapping("/bulkUpdateAccount")
	public ResponseEntity<?> bulkUpdateAccount(@RequestHeader(required =false, value="accountOwner") String accountOwner,
		@RequestBody List<String> ids , @RequestHeader("Authorization") String token) {

		try {
			Iterable<Account> accountObjects = accountRepo.findAllById(ids);
			for(Account act: accountObjects) {
				act.setAccountOwner(accountOwner);
			}
			List<Account> updatedAccounts = accountRepo.saveAll(accountObjects);
			return new ResponseEntity<> (updatedAccounts, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}


		// Front page data on front end
		@PostMapping("/fetchAllContacts")
		public ResponseEntity<?> getRtiFiles1(@RequestHeader("page") int page, @RequestHeader("size") int size, @RequestHeader("Authorization") String token,
			HttpServletRequest request, @RequestBody FilterSort filterSort ) {

		try {

			log.info("fetchAllContacts >>>");
			responseObj.clear();

			HashMap<String, String> filter = filterSort.getFilter();
			HashMap<String, String> sort = filterSort.getSort();



			// filter sorting
			String contactName="";
			LocalDateTime createdOnDate=null;
			String accountName = "";
			String phone="";
			String email="";
			String contactOwner="";


			// 1
			if(filter == null && sort == null) {

				Pageable paging = PageRequest.of(page, size, Sort.by("date").descending());
				Page<Contact> findAll = createContactRepo.findAll(paging);

				responseObj.put("length", findAll.getTotalElements());
				responseObj.put("content", findAll.getContent());
				log.info("fetchAllFiles <<<");

				return ResponseEntity.ok(responseObj);
			}

			// 2
			if(filter != null && sort == null) {


				if(filter.get("ContactName") != null) {
					contactName = filter.get("ContactName");
				}
				if(filter.get("accountName") != null) {
					accountName = filter.get("accountName");
				}
				if(filter.get("phone") != null) {
					phone = filter.get("phone");
				}
				if(filter.get("email") != null) {
					email = filter.get("email");
				}
				if(filter.get("contactOwner") != null) {
					contactOwner = filter.get("contactOwner");
				}
				if(filter.get("date") != null) {

					System.out.println(filter.get("date"));
					DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
					LocalDateTime plusDays = date.plusDays(1);
					Pageable paging = PageRequest.of(page, size);

					Page<Contact> filterBySubject = createContactRepo.findAllByNameOrAccountOrPhoneNoOrEmailOrContactOwnerAndDateBetween(
							contactName,  accountName,  phone, email, contactOwner, date , plusDays, paging);  // date=from   plusDays=to

					responseObj.put("length", filterBySubject.getTotalElements());
					responseObj.put("content", filterBySubject.getContent());

					return ResponseEntity.ok(responseObj);
				}

				Pageable paging = PageRequest.of(page, size);
				System.out.println("contact name "+ contactName);
				Page<Contact> filterBySubject = createContactRepo.findAllByNameOrAccountOrPhoneNoOrEmailOrContactOwner(contactName,  accountName,  phone, email, contactOwner, paging);

				System.out.println("size "+filterBySubject.getTotalElements());
				responseObj.put("length", filterBySubject.getTotalElements());
				responseObj.put("content", filterBySubject.getContent());

				return ResponseEntity.ok(responseObj);
			}


			// 3
			if(filter == null && sort != null) {
				String sortType = sort.get("type");
				String field = sort.get("title");

				// ascending
				if(sortType.equals("Asc")) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
					Page<Contact> sorted = createContactRepo.findAll(paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}

				// descending
				if(sortType.equals("Desc")) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
					Page<Contact> sorted = createContactRepo.findAll(paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}

				return ResponseEntity.ok(responseObj);
			}


			// 4
			if(filter != null && sort != null) {

				if(filter.get("contactName") != null) {
					contactName = filter.get("contactName");
				}
				if(filter.get("accountName") != null) {
					accountName = filter.get("accountName");
				}
				if(filter.get("phone") != null) {
					phone = filter.get("phone");
				}
				if(filter.get("email") != null) {
					email = filter.get("email");
				}
				if(filter.get("contactOwner") != null) {
					contactOwner = filter.get("contactOwner");
				}

				String sortType = sort.get("type");
				String field = sort.get("title");

				if(sortType.equals("Asc") && filter.get("date") != null) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
					DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
					LocalDateTime plusDays = date.plusDays(1);


					Page<Contact> sorted = createContactRepo.findAllByNameContainsAndAccountContainsAndPhoneNoContainsAndEmailContainsAndContactOwnerContainsAndDateBetween(
							contactName,  accountName,  phone, email, contactOwner,  date , plusDays,  paging);  // date=from

//					Page<RTIFile> sorted = rtiFileRepository.findAllBySubjectContainsAndCreatedOnDateContainsAndStatusContainsAndPriorityContains(subject, createdOnDate, status, priority, paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}
				if(sortType.equals("Asc") && filter.get("date") == null) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
					Page<Contact> sorted = createContactRepo.findAllByNameOrAccountOrPhoneNoOrEmailOrContactOwner(contactName,  accountName,  phone, email, contactOwner, paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}

				if(sortType.equals("Desc") && filter.get("date") != null) {

					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
					DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
					LocalDateTime plusDays = date.plusDays(1);

					Page<Contact> sorted = createContactRepo.findAllByNameContainsAndAccountContainsAndPhoneNoContainsAndEmailContainsAndContactOwnerContainsAndDateBetween
							(contactName,  accountName,  phone, email, contactOwner, date, plusDays, paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());


				}
				if(sortType.equals("Desc") && filter.get("date") == null) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
					Page<Contact> sorted = createContactRepo.findAllByNameOrAccountOrPhoneNoOrEmailOrContactOwner(contactName,  accountName,  phone, email, contactOwner, paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}
				return ResponseEntity.ok(responseObj);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return ResponseEntity.internalServerError().body("Error in fetching files");
	}
		
				
		
		//  data insertion apis
		@PostMapping("/insertCityData")
		public String insertCityData(@RequestBody List<CountryCity> cities, @RequestHeader("Authorization") String token) {
			
			try {
				countryCityRepo.saveAll(cities);
				return "City data inserted in mongo successfully";
			} catch (Exception e) {
				log.info(e.getMessage());
				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}




	//  data insertion apis
	@GetMapping("/getUserRoles")
	public ResponseEntity<?> getUserRoles(@RequestHeader String username, @RequestHeader("Authorization") String token) {

		try {

			List<DeptConfigT> byDeptUsername = deptConfigTRepo.findByDeptUsername(username);
			return new ResponseEntity<>(byDeptUsername, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}



//	@PostMapping("/assignRole")
//	@Transactional
//	public ResponseEntity<?> assignRoles(@RequestHeader("roleName") String roleName,
//										 @RequestHeader("userName") String userName) {
//		try {
//			Roles role = rolesRepo.findByRoleName(roleName).orElse(null);
//			if (role == null) {
//				throw new CustomException("roleName : " + roleName + " ,Does Not Exists!", HttpStatus.BAD_REQUEST);
//			}
//			DeptConfigT ckUsr = deptConfigTRepo.findByDeptRoleId(role.getId()).orElse(null);
//			if (ckUsr != null) {
//				throw new CustomException("Role Already Assigned To Sone Other User!", HttpStatus.BAD_REQUEST);
//			}
//			List<DeptConfigT> usrLst = deptConfigTRepo.findByDeptUsername(userName);
//			if (usrLst == null || usrLst.isEmpty()) {
//				throw new CustomException("userName : " + userName + " ,Does Not Exists!", HttpStatus.BAD_REQUEST);
//			}
//			DeptConfigT curUsr = usrLst.get(0);
//			DeptConfigS dept = deptConfigSRepo.findById(role.getDepartmentId()).orElse(null);
//			if (curUsr.getDeptRole() != null) {
//				curUsr.setId(null);
//			}
//
//			curUsr.setDeptRole(role);
//			curUsr.setDeptDisplayName((dept.getCau()+"."+dept.getDeptName().toLowerCase().split(dept.getCau().toLowerCase())[1]).toUpperCase());
//			curUsr.setDeptName(dept.getDeptName());
////			curUsr.setBranch(dept.getBranch());
//
//			deptConfigTRepo.save(curUsr);
//
////			if(role.isPresent())
////			{
////				Optional<DeptConfigT> user= deptConfigTrepo.findByDeptRole(role.get());
////				if(user.isPresent())
////				{
////					throw new CustomException("Role is already assigned to another user!!", HttpStatus.BAD_REQUEST);
////				}
////				users.setDeptRole(role.get());
////				users.setId(null);;
////				deptConfigTrepo.save(users);
////			}
//			return new ResponseEntity<>("Role assigned successfully!!", HttpStatus.OK);
//		} catch (CustomException e) {
////			System.out.println(e.getMessage());
//			e.printStackTrace();
//			throw new CustomException(e.getMessage(), HttpStatus.BAD_REQUEST);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}


	@PostMapping("/createUser")
	public ResponseEntity<?> createUser(@RequestBody DeptConfigT user) {
		deptConfigTRepo.save(user);
		return new ResponseEntity<>(user, HttpStatus.OK);

	}



	@PostMapping("/generateQuotation")    // in LEAD
	public ResponseEntity<?> generateQuotation(@RequestBody(required = false) TemplateDto templateDto,
		@RequestHeader("deptName") String deptName, @RequestHeader("id") String id, @RequestHeader(value = "templateType") String templateType,
		@RequestHeader(required = false, value = "discount") Double finalDiscount, @RequestHeader("Authorization") String token) {
		try {
			Leads leads = leadsRepo.findById(id).get();
			byte[] bytes = templateService.editTemplate2(leads, deptName, finalDiscount, templateType, token);
			return new ResponseEntity<>(bytes, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}


	@PostMapping("/generateInvoice")    // attached with LEAD
	public ResponseEntity<?> generateInvoice(
		@RequestHeader("deptName") String deptName, @RequestHeader("id") String id, @RequestHeader("template") String templateType,
		@RequestHeader("invoice") String invoice, @RequestHeader("type") String type, @RequestHeader("Authorization") String token) {
		try {
			Leads lead = leadsRepo.findById(id).get();
			byte[] bytes = templateService.invoice(lead, deptName, lead.getPaymentMilestoneLst(), invoice, type, token);
			return new ResponseEntity<>(bytes, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

//	@PostMapping("/template1")
//	public ResponseEntity<?> insertUsers1(@RequestBody TemplateDto templateDto,
//		@RequestHeader("deptName") String deptName, @RequestHeader("Authorization") String token) {
//		try {
//
//			byte[] bytes = templateService.editTemplate1(templateDto, deptName, token);
//			return new ResponseEntity<>(bytes, HttpStatus.OK);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//
//	}


	@GetMapping("/getTemplate")
	public ResponseEntity<?> getAllTemplates(@RequestHeader("Authorization") String token, @RequestHeader("template") String templateFromFrontEnd) {
		try {
			token = token.replace("Bearer ", "");
			log.info("template = "+templateFromFrontEnd);
			AmazonS3 awsClient = awsClientConfigService.awsClientConfiguration(token);
			final String bucket = "template";
			GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, templateFromFrontEnd+".docx");

			S3Object object = awsClient.getObject(getObjectRequest);

			byte[] obj = IOUtils.toByteArray(object.getObjectContent());
			log.info("exiting templates");
			return new ResponseEntity<>(obj, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}


	@GetMapping("/accountForLeads")
	public ResponseEntity<List<AccountForLeadsDto>> dropDownAccountForLeads(@RequestHeader("Authorization") String token) {

		try {
			List<AccountForLeadsDto> collect = accountRepo.findAll().stream().map(lead -> new AccountForLeadsDto(lead.getAccountName(), lead.getAccountOwner())).collect(Collectors.toList());
			return new ResponseEntity<>(collect, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/*
	APIS FOR LEADS
	Author - Saksham Bhati
	 */

	@PostMapping("/addLeads")
	public ResponseEntity<?> addLeads(@RequestBody Leads leads, @RequestHeader("Authorization") String token) {

		try {
			log.info("addLeads hit");
			leads.setLeadId("LEAD" + new Random().nextInt(900) + 100);

			AuditMessage auditObj = new AuditMessage();
			auditObj.setAction("Lead created with id "+leads.getLeadId());
			auditObj.setLeadId(leads.getLeadId());

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
			String formattedDate = auditObj.getLocalDateTime().format(formatter);
			auditObj.setDate(formattedDate);
//			auditObj.setTestDate(date);

			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
			String time = auditObj.getLocalDateTime().format(timeFormatter);
			auditObj.setTime(time);

			kafkaTemplate.send(TOPIC, auditObj);

			// start date
			LocalDate startDate = auditObj.getLocalDateTime().toLocalDate();
			DateTimeFormatter startDateformatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			String starttDate = startDate.format(startDateformatter);
			leads.setStartDate(starttDate);

			// End Date
			LocalDate localDate = auditObj.getLocalDateTime().toLocalDate().plusDays(30);
			DateTimeFormatter endDateformatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
			String endDate = localDate.format(endDateformatter);
			leads.setEndDate(endDate);

			leadsRepo.save(leads);

			return new ResponseEntity<>(leads, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/getLeads")
	public ResponseEntity<?> insertUsers(@RequestHeader("page") int page, @RequestHeader("size") int size,
		@RequestBody FilterSort filterSort, @RequestHeader("Authorization") String token) {

		try {
			HashMap<String, String> filter = filterSort.getFilter();
			HashMap<String, String> sort = filterSort.getSort();
			responseObj.clear();

			// 1
			if(filter == null && sort == null) {
				Pageable paging = PageRequest.of(page, size);
				Page<Leads> findAll = leadsRepo.findAll(paging);

				responseObj.put("length", findAll.getTotalElements());
				responseObj.put("content", findAll.getContent());
				log.info("fetchAllFiles <<<");

				return ResponseEntity.ok(responseObj);
			}

			String account="";
			String description="";
			String leadTitle="";
			String status="";

			// 2
			if(filter != null && sort == null) {


				if(filter.get("account") != null) {
					account = filter.get("account");
				}
				if(filter.get("description") != null) {
					description = filter.get("description");
				}
				if(filter.get("leadTitle") != null) {
					leadTitle = filter.get("leadTitle");
				}
				if(filter.get("status") != null) {
					status = filter.get("status");
				}

				Pageable paging = PageRequest.of(page, size);
				System.out.println("filter = "+ filter.toString());
				System.out.println("status = "+ status);
				Page<Leads> filterBySubject = leadsRepo.findAllByAccountOrDescriptionOrLeadTitleOrStatus(account, description, leadTitle, status, paging);

				responseObj.put("length", filterBySubject.getTotalElements());
				responseObj.put("content", filterBySubject.getContent());

				return ResponseEntity.ok(responseObj);
			}

			// 3
			if(filter == null && sort != null) {
				String sortType = sort.get("type");
				String field = sort.get("title");

				// ascending
				if(sortType.equals("Asc")) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
					Page<Leads> sorted = leadsRepo.findAll(paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}

				// descending
				if(sortType.equals("Desc")) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
					Page<Leads> sorted = leadsRepo.findAll(paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}

				return ResponseEntity.ok(responseObj);
			}

			// 4
			if(filter != null && sort != null) {

				if(filter.get("account") != null) {
					account = filter.get("account");
				}
				if(filter.get("description") != null) {
					description = filter.get("description");
				}
				if(filter.get("leadTitle") != null) {
					leadTitle = filter.get("leadTitle");
				}
				if(filter.get("status") != null) {
					status = filter.get("status");
				}

				String sortType = sort.get("type");
				String field = sort.get("title");

				if(sortType.equals("Asc")) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
					Page<Leads> sorted = leadsRepo.findAllByAccountOrDescriptionOrLeadTitleOrStatus(account, description, leadTitle, status, paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}

				if(sortType.equals("Desc") && filter.get("date") == null) {
					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
					Page<Leads> sorted = leadsRepo.findAllByAccountOrDescriptionOrLeadTitleOrStatus(account, description, leadTitle, status, paging);

					responseObj.put("length", sorted.getTotalElements());
					responseObj.put("content", sorted.getContent());
				}
				return ResponseEntity.ok(responseObj);
			}

		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>("Some error occured", HttpStatus.INTERNAL_SERVER_ERROR);
	}


	@PutMapping("/updateLeads")
	public ResponseEntity<?> updateLeads(@RequestBody Leads leads,
		@RequestHeader("id") String id, @RequestHeader("Authorization") String token) {

		try {
			log.info("Enter updateLeads");

//			Mono<Leads>[] updatedLeads = new Mono[0];
//			leadsRepo.findById(id)
//					.flatMap(oldLead -> {
//						// Update the oldLeads with values from the leads object
//						oldLead.setLeadTitle(leads.getLeadTitle());
//						oldLead.setStatus(leads.getStatus());
//						oldLead.setAccount(leads.getAccount());
//						oldLead.setDescription(leads.getDescription());
//
//						// Save the updated oldLeads back to the database
//						updatedLeads[0] = leadsRepo.save(oldLead);      // bcz of single obj
//						return updatedLeads[0];
//					})
//					.subscribe(
//							updatedLead -> {
//								// Handle the updated leads if needed
//								// This block will be called once the update is successful
//							},
//							error -> {
//								// Handle errors here
//							}
//					);
			Leads oldLeads = leadsRepo.findById(id).get();
			oldLeads.setLeadTitle(leads.getLeadTitle());
			oldLeads.setStatus(leads.getStatus());
			oldLeads.setAccount(leads.getAccount());
			oldLeads.setDescription(leads.getDescription());

			AuditMessage auditObj = new AuditMessage();

			// history
			if(leads.getLeadTitle() != null) {
				auditObj.setAction(leads.getLeadTitle());

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
				String formattedDate = auditObj.getLocalDateTime().format(formatter);
				auditObj.setDate(formattedDate);

				DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
				String time = auditObj.getLocalDateTime().format(timeFormatter);
				auditObj.setTime(time);

				kafkaTemplate.send(TOPIC, auditObj);

			}
			if(leads.getAccount() != null) {
				auditObj.setAction(leads.getAccount());

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
				String formattedDate = auditObj.getLocalDateTime().format(formatter);
				auditObj.setDate(formattedDate);

				DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
				String time = auditObj.getLocalDateTime().format(timeFormatter);
				auditObj.setTime(time);

				kafkaTemplate.send(TOPIC, auditObj);

			}
			if(leads.getDescription() != null) {
				auditObj.setAction(leads.getDescription());

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
				String formattedDate = auditObj.getLocalDateTime().format(formatter);
				auditObj.setDate(formattedDate);

				DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
				String time = auditObj.getLocalDateTime().format(timeFormatter);
				auditObj.setTime(time);

				kafkaTemplate.send(TOPIC, auditObj);

			}
			Leads updatedLeads = leadsRepo.save(oldLeads);
			log.info("Exit updateLeads ");
			return new ResponseEntity<>(updatedLeads, HttpStatus.OK);

		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@DeleteMapping("/deleteLeads")
	public ResponseEntity<?> deleteLeads(@RequestBody List<String> ids, @RequestHeader("Authorization") String token) {

		try {
			AuditMessage auditObj = new AuditMessage();
			auditObj.setAction("Lead deleted with ids "+ ids);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
			String formattedDate = auditObj.getLocalDateTime().format(formatter);
			auditObj.setDate(formattedDate);

			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
			String time = auditObj.getLocalDateTime().format(timeFormatter);
			auditObj.setTime(time);

			kafkaTemplate.send(TOPIC, auditObj);

			leadsRepo.deleteAllById(ids);
			return new ResponseEntity<>(leadsRepo.findAll(), HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/productSelectedForLeads")
	public ResponseEntity<?> productSelectedForLeads(@RequestHeader("id") String id, @RequestBody List<Products> products,
		@RequestHeader("Authorization") String token)
	{
		try {
			log.info("productSelectedForLeads hit");
			Leads leads = leadsRepo.findById(id).get();
			int i=0;
			ArrayList<Products> newProductList = new ArrayList<>();
			for(Products p: products) {

				Products byProductTitle = productDescriptionRepo.findByProductTitle(products.get(i).getProduct());

				int initialCost = p.getCost() * p.getQuantity();
				Double finalPrice = initialCost - ((p.getDiscount()/100) * initialCost);
				byProductTitle.setEstimatedValue(finalPrice);
				byProductTitle.setQuantity(p.getQuantity());     //
				byProductTitle.setDiscount(p.getDiscount());
				byProductTitle.setProductTitle(p.getProduct());

//				kafkaTemplate.send("audit", "product -> " +p.getProduct()+" added by lead "+leads.getLeadTitle());
//				kafkaTemplate.send("audit", "discount -> " +p.getDiscount()+" added by lead "+leads.getLeadTitle());
				newProductList.add(byProductTitle);
				i++;
			}
			leads.setProducts(newProductList);
			Leads save = leadsRepo.save(leads);

//			String cost = productDescriptionRepo.findByProductTitle(productName).getMrp();
//			int price = Integer.parseInt(cost) * qty;
//			Integer finalPrice = price - (discount/100) * price;
//
//			LeadsTableDto leadsTableDto = new LeadsTableDto();
//			leadsTableDto.setDiscount(discount);
//			leadsTableDto.setQty(qty);
//			leadsTableDto.setPrice(finalPrice);
//			leadsTableDto.setLeadsId(id);
//			leadsTableDtoRepo.save(leadsTableDto);
			return new ResponseEntity<> (save, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/showLeadsTable")
	public ResponseEntity<?> showLeadsTable(@RequestHeader("Authorization") String token) {

		try {
			List<LeadsTableDto> all = leadsTableDtoRepo.findAll();
			Integer sum = 0;
			for(int i=0; i<all.size(); i++) {
				sum += all.get(i).getPrice();
			}
			HashMap<String, Object> hash = new HashMap<>();
			hash.put("data", all);
			hash.put("sum", sum);
			return new ResponseEntity<>(hash, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
 	}


	@GetMapping("/getLeadsData")
	public ResponseEntity<Leads> getLeadsData(@RequestHeader("id") String id, @RequestHeader("Authorization") String token)  {
		try {
			log.info("getLeadsData hit");
			Leads leads = leadsRepo.findById(id).get();
			return new ResponseEntity<> (leads, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}


	@PutMapping("/editLead")          // similar to update lead
	public ResponseEntity<Leads> editLead(@RequestHeader("id") String id, @RequestBody List<Products> products,
		@RequestHeader("Authorization") String token) {
		try {
			log.info("editLead hit");
			Leads existingLead = leadsRepo.findById(id).get();
//			for(int i=0; i<leads.getProducts().size(); i++) {
//
//				if(leads.getProducts().get(i).getProduct() != null) {
//					existingLead.getProducts().get(i).setProduct(leads.getProducts().get(i).getProduct());
//				}
//				if(leads.getProducts().get(i).getQuantity() != null) {
//					existingLead.getProducts().get(i).setQuantity(leads.getProducts().get(i).getQuantity());
//				}
//				if(leads.getProducts().get(i).getDiscount() != null) {
//					existingLead.getProducts().get(i).setDiscount(leads.getProducts().get(i).getDiscount());
//				}
//
//			}
			existingLead.setProducts(products);
			Leads save = leadsRepo.save(existingLead);

			AuditMessage auditObj = new AuditMessage();
			auditObj.setAction("lead edited");
			auditObj.setMessage(existingLead.getLeadTitle()+" edited");

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
			String formattedDate = auditObj.getLocalDateTime().format(formatter);
			auditObj.setDate(formattedDate);

			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
			String time = auditObj.getLocalDateTime().format(timeFormatter);
			auditObj.setTime(time);

			kafkaTemplate.send(TOPIC, auditObj);
			return new ResponseEntity<> (save, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@GetMapping("/findAllLeads")
	public ResponseEntity<?> findAllLeads(@RequestHeader("Authorization") String token) {
		return new ResponseEntity<>(leadsRepo.findAll(), HttpStatus.OK);
	}

	@GetMapping("/findAllLeadsForDashboard")
	public ResponseEntity<?> findAllLeadsForDashboard() {
		HashMap<String, Object> hash = new HashMap<>();
		hash.put("leads", leadsRepo.findAll().size());
		hash.put("leadsName", leadsRepo.findAll().stream().map(Leads::getLeadTitle).collect(Collectors.toList()));
		System.out.println(">>> "+ hash);

		hash.put("deals", dealsRepo.findAll().size());
		hash.put("dealsName", dealsRepo.findAll().stream().map(Deal::getDealName).collect(Collectors.toList()));

		return new ResponseEntity<>(hash, HttpStatus.OK);
	}

	@GetMapping("/getHistory")
	public ResponseEntity<List<AuditMessage>> history(@RequestHeader("Authorization") String token) {

		try {
			log.info("getHistory hit");
			List<AuditMessage> all = auditRepo.findAll();
			return new ResponseEntity<> (all, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


//	@PostMapping("/sendEmail")
//	public ResponseEntity<String> sendEmail(@RequestHeader("toAddress") String toAddress, @RequestHeader("Authorization") String token) {
//
//		try {
//			String message = "demo email for leads";
//			toAddress = "devanshi@costacloud.com";    // remove later
//			Email.sendEmailWithAttachments(toAddress, message, null);
//			return new ResponseEntity<> ("email sent", HttpStatus.OK);
//		} catch (Exception e) {
//			log.info(e.getMessage());
//			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//
//	}


	@GetMapping("/test")
	public String test(@RequestHeader("Authorization") String token)  {

		LinkedList<String> linkedList = new LinkedList<>();
		linkedList.add("1");
		linkedList.add("2");
		linkedList.add("3");
		linkedList.add("4");
		linkedList.add("5");

		System.out.println(linkedList.getLast());
		return "ok";
	}


	@PostMapping("/saveDocument")
	public ResponseEntity<?> saveDocument(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
		try {
			String token = (String) request.getHeader("Authorization");
			String clipToken = token.replace("Bearer ", "");
			String deptName = (String) request.getHeader("deptName");
			String fileUrl = (String) request.getHeader("fileUrl");

			HashMap<String, Object> checkFile = retrievalService.saveDocument(file, clipToken, deptName, fileUrl);
			HashMap<String, Object> json = new HashMap<>();
			if ((boolean) checkFile.get("isSuccess")) {
				json.put("status", HttpStatus.OK);
				json.put("data", checkFile);
				return ResponseEntity.ok(json);
			} else {
				json.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
				throw new CustomException("InternalServerError",HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}catch (Exception e) {

			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/leadStatusUpdate")    // similar to updateLead
	public ResponseEntity<Leads> leadStatusUpdate(@RequestHeader("id") String id, @RequestHeader("status") String status,
		@RequestHeader("Authorization") String token) {
		try {
			Leads lead = leadsRepo.findById(id).get();
			String oldStatus = lead.getStatus();
			lead.setStatus(status);
			leadsRepo.save(lead);

			AuditMessage auditObj = new AuditMessage();
			auditObj.setAction("status updated of leadId "+ lead.getLeadId() + "from "+ oldStatus + " to " + status);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
			String formattedDate = auditObj.getLocalDateTime().format(formatter);
			auditObj.setDate(formattedDate);

			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
			String time = auditObj.getLocalDateTime().format(timeFormatter);
			auditObj.setTime(time);

			kafkaTemplate.send(TOPIC, auditObj);
			return new ResponseEntity<>(lead, HttpStatus.OK);
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@GetMapping("/getLeadsTable")
	public ResponseEntity<?> leadsTable(@RequestHeader("Authorization") String token) {
		try {

			List<Leads> allLeads = leadsRepo.findAll();

			Map<String, Integer> statusToValue = new HashMap<>();
			statusToValue.put("New", 1);
			statusToValue.put("Upside", 2);
			statusToValue.put("Strong Upside", 3);
			statusToValue.put("Commit", 4);
			statusToValue.put("Lost", 6);
			statusToValue.put("WON", 5);


			Comparator<Leads> leadComparator = (lead1, lead2) -> {
				int statusPriority1 = statusToValue.get(lead1.getStatus());
				int statusPriority2 = statusToValue.get(lead2.getStatus());

				if (statusPriority1 != statusPriority2) {
					// If status priorities are different, sort by status priority
					return Integer.compare(statusPriority1, statusPriority2);
				} else {
					// If status priorities are the same, sort by price
					return lead1.getEstimatedValue().compareTo(lead2.getEstimatedValue());
				}

		};

			// Step 3: Sort the list based on the constant number values
			Collections.sort(allLeads, leadComparator);

			return new ResponseEntity<>(allLeads, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/addNote")
	public ResponseEntity<?> addNote(@RequestHeader("id") String id, @RequestBody String note,
		@RequestHeader("Authorization") String token) {
		try {
			log.info("add note hit");
			Leads lead = leadsRepo.findById(id).orElseThrow(() -> new NullPointerException("Lead not Found"));

			if(lead.getNote() != null) {
				lead.getNote().add(note);
				leadsRepo.save(lead);
			} else {
				ArrayList<String> arr = new ArrayList<>();
				arr.add(note);
				lead.setNote(arr);
				leadsRepo.save(lead);
			}

			return new ResponseEntity<>(lead, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@GetMapping("/getNote")          // id = id of lead
	public ResponseEntity<?> getNote(@RequestHeader("id") String id,
		@RequestHeader("Authorization") String token) {
		try {
			Leads leads = leadsRepo.findById(id).get();
			return new ResponseEntity<> (leads.getNote(), HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/paymentMilestone")     // part of LEAD
	public ResponseEntity<?> paymentMilestone(@RequestHeader("id") String id, @RequestBody PaymentMilestone paymentMilestoneObjectFromFrontEnd,
		@RequestHeader("Authorization") String token) {
		try {
			Leads lead = leadsRepo.findById(id).get();

			final Double[] sumOfTotalCost = {0.0};

			ArrayList<PaymentMilestone> newList = new ArrayList<>();

			Double sumOfTotalCost1 = paymentMilestoneObjectFromFrontEnd.getPaymentMilestoneDto().stream()
					.mapToDouble(pm -> {
						Double totalCost = pm.getCost() * pm.getQuantity();
						sumOfTotalCost[0] += totalCost;

						Products product = productDescriptionRepo.findByProductTitle(pm.getName());
						Integer originalQuantity = product.getQuantity();
						product.setQuantity(originalQuantity - pm.getQuantity());

						return totalCost;
					})
					.sum();

			// Batch update products
			List<Products> productsToUpdate = paymentMilestoneObjectFromFrontEnd.getPaymentMilestoneDto().stream()
					.map(pm -> {
						Products product = productDescriptionRepo.findByProductTitle(pm.getName());
						Integer originalQuantity = product.getQuantity();
						product.setQuantity(originalQuantity - pm.getQuantity());
						return product;
					})
					.collect(Collectors.toList());

			productDescriptionRepo.saveAll(productsToUpdate);

//            for(PaymentMilestoneDto pm: paymentMilestoneObjectFromFrontEnd.getPaymentMilestoneDto()) {
//				Double totalCost = pm.getCost() * pm.getQuantity();
//
//				Products product = productDescriptionRepo.findByProductTitle(pm.getName());
//				Integer originalQuantity = product.getQuantity();
//				product.setQuantity(originalQuantity - pm.getQuantity());
//				productDescriptionRepo.save(product);
//
//				sumOfTotalCost += totalCost;
//            }

			paymentMilestoneObjectFromFrontEnd.setTotalCost(sumOfTotalCost1);

			if(lead.getPaymentMilestoneLst() != null) {
				// copy earlier milestones
				for(PaymentMilestone pm: lead.getPaymentMilestoneLst()) {
					newList.add(pm);
				}
				int size = lead.getPaymentMilestoneLst().size();
				paymentMilestoneObjectFromFrontEnd.setMilestoneId(size+1);
				paymentMilestoneObjectFromFrontEnd.setLeadId(lead.getLeadId());

				newList.add(paymentMilestoneObjectFromFrontEnd);
				lead.setPaymentMilestoneLst(newList);
				leadsRepo.save(lead);
				paymentMilestoneRepo.save(paymentMilestoneObjectFromFrontEnd);

			} else {

				paymentMilestoneObjectFromFrontEnd.setMilestoneId(1);
				paymentMilestoneObjectFromFrontEnd.setLeadId(lead.getLeadId());

				newList.add(paymentMilestoneObjectFromFrontEnd);
				lead.setPaymentMilestoneLst(newList);
				leadsRepo.save(lead);
				paymentMilestoneRepo.save(paymentMilestoneObjectFromFrontEnd);

			}

			AuditMessage auditMessage = new AuditMessage();
			auditMessage.setMessage("Payment Milestone added by lead ID "+lead.getLeadId());
			auditMessage.setMilestoneDate(paymentMilestoneObjectFromFrontEnd.getPaymentDate());
			auditMessage.setAction("Milestone created for amount " + sumOfTotalCost1);

			kafkaTemplate.send(TOPIC, auditMessage);

			return new ResponseEntity<>(lead, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@GetMapping("/fetchPaymentMilestone")
	public ResponseEntity<?> fetchPaymentMilestone(@RequestHeader("id") String leadId,
		@RequestHeader("Authorization") String token) {
		try {
			Leads lead = leadsRepo.findById(leadId).get();
			List<PaymentMilestone> listOfPaymentMilestones = lead.getPaymentMilestoneLst();

			if(listOfPaymentMilestones.size() != 0) {
				for (int i = 0; i < listOfPaymentMilestones.size(); i++) {
					listOfPaymentMilestones.get(i).setMilestoneNumber("Milestone-" + (i + 1));
					// OVERDUE check
					if(listOfPaymentMilestones.get(i).getDeliveryDate().toLocalDate().isAfter(LocalDateTime.now().toLocalDate())) {
						listOfPaymentMilestones.get(i).setStatus("Overdue");
					}
				}
				lead.setPaymentMilestoneLst(listOfPaymentMilestones);
				leadsRepo.save(lead);
			}
			return new ResponseEntity<>(listOfPaymentMilestones, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@GetMapping("/viewPaymentMilestone")
	public ResponseEntity<?> viewPaymentMilestone(@RequestHeader("leadId") String leadId,
		@RequestHeader("milestoneId") int milestoneId, @RequestHeader("Authorization") String token) {
		try {
			Leads lead = leadsRepo.findByLeadId(leadId);
			for(PaymentMilestone pm: lead.getPaymentMilestoneLst()) {
				if(pm.getMilestoneId() == milestoneId) {
					return new ResponseEntity<>(pm, HttpStatus.OK);
				}
			}
			return new ResponseEntity<>("Data not found", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/cost")
	public ResponseEntity<?> cost(@RequestHeader("id") String leadId, @RequestBody  HashMap<String, String> paymentMilestone,
		@RequestHeader("Authorization") String token) {
		try {
			List<Leads> all = leadsRepo.findAll();
			int sumService = 0, sumProduct=0;

			for(int i=0; i<all.size(); i++) {
				List<Products> products = all.get(i).getProducts();
				for(int j=0; j<products.size(); j++) {
					if(products.get(j).getProductType().equals("Service"))
						sumService += products.get(j).getCost();
					else {
						sumProduct += products.get(j).getCost();
					}

				}
			}
			Map<String, Object> hash = new HashMap<>();
			hash.put("productSum", sumProduct);
			hash.put("serviceSum", sumService);

			return new ResponseEntity<>(hash, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/editEndDate")          // in lead
	public ResponseEntity<?> editEndDateInLead(@RequestHeader("id") String id, @RequestHeader("endDate") String endDate,
	 @RequestHeader("Authorization") String token) {
		try {
			Leads lead = leadsRepo.findById(id).get();
			lead.setEndDate(endDate);
			leadsRepo.save(lead);
			return new ResponseEntity<>(lead, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/getToken")          // in lead
	@Hidden    // dependency commented in pom.xml
	public ResponseEntity<?> getToken() {
		try {
			String keycloakServerUrl = "http://localhost:8080/auth/realms/master";
			String clientId = "costa_client";
			String clientSecret = "b72yRE2RRJXitkuDpKOdgjkB4L1Opkvw";
			String username = "admin";
			String password = "admin";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			RestTemplate restTemplate = new RestTemplate();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			String params = "username=" + username + "&grant_type=password&client_id="
					+ clientId + "&password=" + password + "&client_secret="
					+ clientSecret;
			HttpEntity<String> httprequest = new HttpEntity<String>(params, headers);

			ResponseEntity<?> responseEntity = restTemplate.postForEntity(keycloakServerUrl+ "/protocol/openid-connect/token",
					httprequest, TokenResponse.class);


			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
			} else {
				System.err.println("Error obtaining access token. Status code: " + responseEntity.getStatusCodeValue());
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR);
	}



	@PostMapping("/readImage")          // for PO
	public ResponseEntity<?> readImage(@RequestHeader("key") String key, @RequestParam("file") MultipartFile file,
		@RequestHeader("bucket") String bucket, @RequestHeader("Authorization") String token) {
		try {
			token = token.replace("Bearer ", "");
			String str = tessaractService.parseIMG(bucket, key, token);
			return new ResponseEntity<>(str, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/addDeal")          // for PO
	public ResponseEntity<?> addDeal(@RequestBody Deal deal,
		@RequestHeader("Authorization") String token) {
		try {
			dealsRepo.save(deal);
			return new ResponseEntity<>(deal, HttpStatus.ACCEPTED);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/viewDeal")          // for PO
	public ResponseEntity<?> viewDeal(@RequestHeader("Authorization") String token) {
		try {
			List<Deal> allLeads = dealsRepo.findAll();
			for(Deal deal: allLeads) {
				if(LocalDateTime.now().toLocalDate().isAfter(deal.getDueDate())) {
					deal.setStatus("Overdue");
				}
			}
			dealsRepo.saveAll(allLeads);
			return new ResponseEntity<>(allLeads, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@GetMapping("/editDeal")          // for PO
	public ResponseEntity<?> editDeal(@RequestHeader String dealId, @RequestBody String transactionId,
      @RequestHeader("Authorization") String token) {
		try {
			Deal deal = dealsRepo.findById(dealId).get();
            deal.setTransactionId(transactionId);
            dealsRepo.save(deal);

			return new ResponseEntity<>(deal, HttpStatus.OK);    // change here
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/editEndDateInDeal")          // in deal
	public ResponseEntity<?> editEndDateInDeal(@RequestHeader("id") String id, @RequestHeader("endDate") String endDate,
		@RequestHeader("Authorization") String token) {
		try {
			Deal deal = dealsRepo.findById(id).get();
			deal.setDueDate(LocalDate.parse(endDate));
			dealsRepo.save(deal);
			return new ResponseEntity<>(deal, HttpStatus.OK);
		} catch (Exception e) {
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/dealFilterSearch")          // for PO
	public ResponseEntity<?> dealFilterSearch(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			 @RequestBody HashMap<String, Object> deal, @RequestHeader("Authorization") String token) {
		try {

			if(deal.get("poNumber") != null) {
				List<Deal> dealObjects = dealsRepo.findByPoNumberContaining((String) deal.get("poNumber"));
				return new ResponseEntity<> (dealObjects, HttpStatus.OK);
			}
			else if (deal.get("dueDate") != null) {
				List<Deal> dealObjects = dealsRepo.findByDueDate(date);
				return new ResponseEntity<> (dealObjects, HttpStatus.OK);
			} else if(deal.get("leadId") != null) {
				List<Deal> dealObjects = dealsRepo.findByLeadId(deal.get("leadId"));
				return new ResponseEntity<> (dealObjects, HttpStatus.OK);
			} else if(deal.get("status") != null) {
				List<Deal> dealObjects = dealsRepo.findByStatus(deal.get("status"));
				return new ResponseEntity<> (dealObjects, HttpStatus.OK);
			}
		return new ResponseEntity<>("error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/uploadPO")          // for PO,  id = mongoDB lead id
	public ResponseEntity<?> uploadPO(@RequestParam MultipartFile file, @RequestHeader("id") String id, @RequestHeader("deptName") String deptName,
		@RequestHeader("PONumber") String poNumber, @RequestHeader("Authorization") String token) throws NotFoundException,IOException {
		try {
			Leads lead = leadsRepo.findById(id).get();
			for(PaymentMilestone pm: lead.getPaymentMilestoneLst()) {
				Deal deal = new Deal();
				deal.setPaymentMilestone(pm);
				deal.setMilestoneId(pm.getId());
				deal.setPoNumber(poNumber);
				deal.setDealName(pm.getName());
				deal.setStatus("In Process");
				deal.setDueDate(LocalDateTime.now().toLocalDate().plusDays(30));
				dealsRepo.save(deal);
			}
			FileDetails fd = new FileDetails();
			fd.setFileUrl(deptName + "/" + lead.getLeadId() + "/" + "PO/" + file.getOriginalFilename());

			lead.setPoUrl(fd.getFileUrl());
			leadsRepo.save(lead);

			// minio
			token = token.replace("Bearer ", "");
			AmazonS3Client awsClient = (AmazonS3Client) awsClientConfigService.awsClientConfiguration(token);
			awsClient.putObject(deptName, "/" + lead.getLeadId() + "/" + "PO/" + file.getOriginalFilename(), file.getInputStream(),  null);

			esearchProducer.send("", file.getOriginalFilename(), fd.getFileUrl());
			boolean b = containsQRCode(file);

			return new ResponseEntity<>(b, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	public static boolean containsQRCode(MultipartFile file) throws NotFoundException {
		try {

			InputStream imageInputStream = file.getInputStream();
			BufferedImage image = ImageIO.read(imageInputStream);

			BinaryBitmap binaryBitmap
					= new BinaryBitmap(new HybridBinarizer(
							new BufferedImageLuminanceSource(image)));
			Result result = new MultiFormatReader().decode(binaryBitmap);

			// If decoding is successful, it's likely a QR code
			if(result.getText() != null)
				return true;
			else
				return false;
//			return result != null;
		} catch (IOException | NotFoundException e) {
			// Handle exceptions (e.g., IOException or NotFoundException)
			e.printStackTrace();
			return false;
		}
	}

	//		// Front page data on front end
//		@PostMapping("/fetchAllProducts")
//		public ResponseEntity<?> getRtiFiles(@RequestHeader("page") int page, @RequestHeader("size") int size,
//				@RequestHeader("Authorization") String token, HttpServletRequest request, @RequestBody FilterSort filterSort ) {
//
//			try {
//
//			log.info("fetchAllFiles >>>");
//			responseObj.clear();
//
//			HashMap<String, String> filter = filterSort.getFilter();
//			HashMap<String, String> sort = filterSort.getSort();
//
//
//
//			// filter sorting
//			String sku="";
////			LocalDateTime createdOnDate=null;
//			String productTitle = "";
//			String description="";
//			String mrp="";
//
//			// 1
//			if(filter == null && sort == null) {
//
//			Pageable paging = PageRequest.of(page, size, Sort.by("date").descending());
//			Page<ProductDescription> findAll = productDescriptionRepo.findAll(paging);
//
//			responseObj.put("length", findAll.getTotalElements());
//			responseObj.put("content", findAll.getContent());
//			log.info("fetchAllFiles <<<");
//
//			return ResponseEntity.ok(responseObj);
//			}
//
//
//			// 2
//			if(filter != null && sort == null) {
//
//
//				if(filter.get("skuNo") != null) {
//					 sku = filter.get("skuNo");
//				}
//				if(filter.get("productTitle") != null) {
//					productTitle = filter.get("productTitle");
//				}
//				if(filter.get("Description") != null) {
//					description = filter.get("Description");
//				}
//				if(filter.get("MRP") != null) {
//					mrp = filter.get("MRP");
//				}
//				if(filter.get("date") != null) {
//
//					 System.out.println(filter.get("date"));
//					 DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//					 LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
//					 LocalDateTime plusDays = date.plusDays(1);
//					 Pageable paging = PageRequest.of(page, size);
//
//					 Page<ProductDescription> filterBySubject = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrpAndDateBetween(
//							 sku,  productTitle,  description,  mrp, date , plusDays,    paging);  // date=from   plusDays=to
//
//						responseObj.put("length", filterBySubject.getTotalElements());
//						responseObj.put("content", filterBySubject.getContent());
//
//						return ResponseEntity.ok(responseObj);
//				}
//
//				Pageable paging = PageRequest.of(page, size);
//				Page<ProductDescription> filterBySubject = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrp(sku,  productTitle,  description, mrp, paging);
//
//				responseObj.put("length", filterBySubject.getTotalElements());
//				responseObj.put("content", filterBySubject.getContent());
//
//				return ResponseEntity.ok(responseObj);
//			}
//
//
//			// 3
//			if(filter == null && sort != null) {
//				String sortType = sort.get("type");
//				String field = sort.get("title");
//
//				// ascending
//				if(sortType.equals("Asc")) {
//				Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
//				Page<ProductDescription> sorted = productDescriptionRepo.findAll(paging);
//
//				responseObj.put("length", sorted.getTotalElements());
//				responseObj.put("content", sorted.getContent());
//				}
//
//				// descending
//				if(sortType.equals("Desc")) {
//					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
//					Page<ProductDescription> sorted = productDescriptionRepo.findAll(paging);
//
//					responseObj.put("length", sorted.getTotalElements());
//					responseObj.put("content", sorted.getContent());
//					}
//
//				return ResponseEntity.ok(responseObj);
//			}
//
//
//			// 4
//			if(filter != null && sort != null) {
//
//
//				if(filter.get("skuNo") != null) {
//					 sku = filter.get("skuNo");
//				}
//				if(filter.get("Description") != null) {
//					description = filter.get("Description");
//				}
//				if(filter.get("productTitle") != null) {
//					productTitle = filter.get("productTitle");
//				}
//				if(filter.get("MRP") != null) {
//					mrp = filter.get("MRP");
//				}
//
//				String sortType = sort.get("type");
//				String field = sort.get("title");
//
//				if(sortType.equals("Asc") && filter.get("date") != null) {
//					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
//					DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//					LocalDateTime date = LocalDateTime.parse(filter.get("createdOnDate") +" 00:00", forPattern);
//					LocalDateTime plusDays = date.plusDays(1);
//
//
//					Page<ProductDescription> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrpAndDateBetween(
//							 sku,  productTitle,  description,  mrp, date , plusDays,  paging);  // date=from
//
////				Page<RTIFile> sorted = rtiFileRepository.findAllBySubjectContainsAndCreatedOnDateContainsAndStatusContainsAndPriorityContains(subject, createdOnDate, status, priority, paging);
//
//				responseObj.put("length", sorted.getTotalElements());
//				responseObj.put("content", sorted.getContent());
//				}
//				if(sortType.equals("Asc") && filter.get("date") == null) {
//					Pageable paging = PageRequest.of(page, size, Sort.by(field).ascending());
//					Page<ProductDescription> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrp(sku, productTitle, description, mrp, paging);
//
//					responseObj.put("length", sorted.getTotalElements());
//					responseObj.put("content", sorted.getContent());
//				}
//
//				if(sortType.equals("Desc") && filter.get("date") != null) {
//
//					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
//					DateTimeFormatter forPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//					LocalDateTime date = LocalDateTime.parse(filter.get("date") +" 00:00", forPattern);
//					LocalDateTime plusDays = date.plusDays(1);
//
//					Page<ProductDescription> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrpAndDateBetween(sku, productTitle, description, mrp, date, plusDays, paging);
//
//					responseObj.put("length", sorted.getTotalElements());
//					responseObj.put("content", sorted.getContent());
//
//
//				}
//				if(sortType.equals("Desc") && filter.get("date") == null) {
//					Pageable paging = PageRequest.of(page, size, Sort.by(field).descending());
//					Page<ProductDescription> sorted = productDescriptionRepo.findBySkuOrProductTitleOrDescriptionOrMrp(sku, productTitle, description, mrp, paging);
//
//					responseObj.put("length", sorted.getTotalElements());
//					responseObj.put("content", sorted.getContent());
//				}
//				return ResponseEntity.ok(responseObj);
//			}
//
//			} catch (Exception e) {
//				e.printStackTrace();
//				throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//			}
//			return ResponseEntity.internalServerError().body("Error in fetching files");
//		}
}

