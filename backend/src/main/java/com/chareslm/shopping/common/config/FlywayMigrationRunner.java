package com.chareslm.shopping.common.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/** Runs packaged database migrations before any application bootstrap runner. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FlywayMigrationRunner implements ApplicationRunner {
    private final DataSource dataSource;
    private final boolean enabled;
    private final boolean baselineOnMigrate;
    private final String baselineVersion;

    public FlywayMigrationRunner(DataSource dataSource,
                                 @Value("${spring.flyway.enabled:true}") boolean enabled,
                                 @Value("${spring.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate,
                                 @Value("${spring.flyway.baseline-version:2}") String baselineVersion) {
        this.dataSource = dataSource;
        this.enabled = enabled;
        this.baselineOnMigrate = baselineOnMigrate;
        this.baselineVersion = baselineVersion;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
                .load()
                .migrate();
    }
}
