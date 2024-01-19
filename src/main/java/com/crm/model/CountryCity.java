package com.crm.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "worldcities")
public class CountryCity {

	@Id
	private String id;
	private String city;
	
	private String city_ascii;
	private Double lat;
	private Double lbg;
	private String country;
	private String iso2;
	private String iso3;
	private String admin_name;
	private String capital;
	private Long population;
//	private Long id;

}
