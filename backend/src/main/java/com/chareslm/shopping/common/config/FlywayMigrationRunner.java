package com.chareslm.shopping.common.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Runs packaged database migrations before any application bootstrap runner. */
@Component
public class FlywayMigrationRunner implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationRunner.class);
    private static final String LEGACY_V8_SCRIPT = "V8__audit_log_view_permission.sql";

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
    public void afterPropertiesSet() {
        if (!enabled) {
            return;
        }
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(MigrationVersion.fromVersion(baselineVersion));

        if (!hasLegacyAuditV8()) {
            configuration.load().migrate();
            return;
        }

        log.warn("Detected legacy audit migration at Flyway V8; applying the forward-only merchant compatibility migration");
        Flyway compatibilityFlyway = configuration.validateOnMigrate(false).load();
        compatibilityFlyway.migrate();
        compatibilityFlyway.repair();
        compatibilityFlyway.validate();
        log.info("Legacy Flyway V8 metadata and merchant schema compatibility have been reconciled successfully");
    }

    private boolean hasLegacyAuditV8() {
        String sql = """
                SELECT script
                FROM flyway_schema_history
                WHERE version = '8'
                  AND success = 1
                  AND NOT EXISTS (
                      SELECT 1 FROM flyway_schema_history
                      WHERE version = '13' AND success = 1
                  )
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && LEGACY_V8_SCRIPT.equals(resultSet.getString("script"));
        } catch (SQLException exception) {
            if (isMissingHistoryTable(exception)) {
                return false;
            }
            throw new IllegalStateException("Failed to inspect Flyway V8 migration history", exception);
        }
    }

    private boolean isMissingHistoryTable(SQLException exception) {
        return exception.getErrorCode() == 1146 || "42S02".equals(exception.getSQLState());
    }
}
