package com.study.blog.tools;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.RepairResult;
import org.flywaydb.core.api.output.ValidateResult;

import java.util.Locale;
import java.util.Optional;

public final class FlywayCommandRunner {

    private FlywayCommandRunner() {
    }

    public static void main(String[] args) {
        String command = args.length > 0 ? args[0].trim().toLowerCase(Locale.ROOT) : "info";
        Flyway flyway = Flyway.configure()
                .dataSource(requiredEnv("DB_URL"), requiredEnv("DB_USERNAME"), requiredEnv("DB_PASSWORD"))
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .baselineDescription("Initial schema")
                .locations("classpath:db/migration")
                .load();

        switch (command) {
            case "info" -> printInfo(flyway.info().all());
            case "validate" -> runValidate(flyway.validateWithResult());
            case "migrate" -> runMigrate(flyway.migrate());
            case "repair" -> runRepair(flyway.repair());
            default -> throw new IllegalArgumentException("Unsupported command: " + command);
        }
    }

    private static void printInfo(MigrationInfo[] infoResults) {
        for (MigrationInfo infoResult : infoResults) {
            String version = Optional.ofNullable(infoResult.getVersion())
                    .map(Object::toString)
                    .orElse("-");
            String description = Optional.ofNullable(infoResult.getDescription()).orElse("-");
            String state = Optional.ofNullable(infoResult.getState())
                    .map(Object::toString)
                    .orElse("-");
            System.out.printf("%-12s %-45s %s%n", version, description, state);
        }
    }

    private static void runValidate(ValidateResult validateResult) {
        if (!validateResult.validationSuccessful) {
            throw new IllegalStateException("Flyway validation failed: " + validateResult.errorDetails.errorMessage);
        }
        System.out.println("Flyway validation successful.");
    }

    private static void runMigrate(MigrateResult migrateResult) {
        System.out.println("Target schema version: " + migrateResult.targetSchemaVersion);
        System.out.println("Migrations executed: " + migrateResult.migrationsExecuted);
        System.out.println("Initial schema version: " + migrateResult.initialSchemaVersion);
    }

    private static void runRepair(RepairResult repairResult) {
        System.out.println("Flyway repair completed.");
        System.out.println("Repair actions: " + repairResult.repairActions);
    }

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }
}
