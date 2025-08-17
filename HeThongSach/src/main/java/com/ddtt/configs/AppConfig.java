package com.ddtt.configs;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@Factory
public class AppConfig {

    @Singleton
    public DSLContext dsl(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
    
    @Singleton
    public Cloudinary cloudinary(
        @Value("${cloudinary.cloud_name:}") String cloudName,
        @Value("${cloudinary.api_key:}") String apiKey,
        @Value("${cloudinary.api_secret:}") String apiSecret
    ) {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
    }
}
