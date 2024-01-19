package com.crm.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"com.crm.repository2"},
mongoTemplateRef = NewDB2Config.MONGO_TEMPLATE
)
public class NewDB2Config {
    protected static final String MONGO_TEMPLATE = "newdb2MongoTemplate";
}
