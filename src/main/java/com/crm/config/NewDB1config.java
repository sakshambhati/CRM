package com.crm.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"com.crm.repository"},
mongoTemplateRef = NewDB1config.MONGO_TEMPLATE
)
public class NewDB1config {
    protected static final String MONGO_TEMPLATE = "newdb1MongoTemplate";
}
