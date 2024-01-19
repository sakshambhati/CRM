package com.crm.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.crm.model.CountryCity;

public interface CountryCityRepo extends MongoRepository<CountryCity, String>{

//	List<CountryCity> findAllByCountryIgnoreCase(String country);

	List<CountryCity> findAllByCountryIgnoreCaseAndCityStartingWithIgnoreCase(String country, String pattern);

	List<CountryCity> findAllByCountryStartingWithIgnoreCase(String country);

	List<CountryCity> findAllByCountryIgnoreCase(String country);

	

}
