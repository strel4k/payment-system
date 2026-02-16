package com.example.transaction.config;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;


@Configuration
@Profile("sharding")
public class ShardingSphereConfig {

    @Value("classpath:shardingsphere-config.yaml")
    private Resource shardingConfigFile;

    @Bean
    public DataSource dataSource() throws SQLException, IOException {
        return YamlShardingSphereDataSourceFactory.createDataSource(
                shardingConfigFile.getFile()
        );
    }
}