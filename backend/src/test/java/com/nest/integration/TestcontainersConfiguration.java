package com.nest.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A real SQL Server for integration tests. {@code @ServiceConnection} points the datasource at it,
 * so Flyway migrations and Hibernate schema validation run exactly as in development.
 * Spring caches the test context, so one container serves every integration test class.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final DockerImageName SQL_SERVER = DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest");

    @Bean
    @ServiceConnection
    MSSQLServerContainer<?> sqlServerContainer() {
        return new MSSQLServerContainer<>(SQL_SERVER).acceptLicense();
    }
}
