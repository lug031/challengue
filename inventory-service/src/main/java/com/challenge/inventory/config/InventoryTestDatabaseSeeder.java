package com.challenge.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * Inicializa el esquema H2 de Inventario en el perfil {@code test}.
 */
@Component
@Profile("test")
public class InventoryTestDatabaseSeeder implements ApplicationRunner {

    private final Resource schema;
    private final Resource data;
    private final String r2dbcUrl;
    private final String username;
    private final String password;

    public InventoryTestDatabaseSeeder(
            @Value("classpath:inventory/schema.sql") Resource schema,
            @Value("classpath:inventory/data.sql") Resource data,
            @Value("${spring.r2dbc.url}") String r2dbcUrl,
            @Value("${spring.r2dbc.username:sa}") String username,
            @Value("${spring.r2dbc.password:}") String password
    ) {
        this.schema = schema;
        this.data = data;
        this.r2dbcUrl = r2dbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        String jdbcUrl = toJdbcUrl(r2dbcUrl);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema, data);
        populator.setContinueOnError(false);

        try (Connection connection = dataSource.getConnection()) {
            populator.populate(connection);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo inicializar la base de datos de Inventario (test)", ex);
        }
    }

    private static String toJdbcUrl(String r2dbcUrl) {
        String prefix = "r2dbc:h2:mem:///";
        if (r2dbcUrl != null && r2dbcUrl.startsWith(prefix)) {
            String remainder = r2dbcUrl.substring(prefix.length());
            int semicolonIdx = remainder.indexOf(';');
            String dbName = semicolonIdx >= 0 ? remainder.substring(0, semicolonIdx) : remainder;
            String params = semicolonIdx >= 0 ? remainder.substring(semicolonIdx) : "";
            return "jdbc:h2:mem:" + dbName + params;
        }
        return r2dbcUrl.replaceFirst("^r2dbc:h2:", "jdbc:h2:");
    }
}

