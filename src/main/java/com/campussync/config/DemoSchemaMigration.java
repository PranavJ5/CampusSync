package com.campussync.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.flywaydb.core.Flyway;

public final class DemoSchemaMigration {
    private static final String DATABASE_NAME = "campussync_demo";
    private static final String DATABASE_URL = "jdbc:mysql://127.0.0.1:3306/campussync_demo";
    private static final int DATABASE_PORT = 3306;
    private static final String CONFIRMATION = "--confirm=" + DATABASE_NAME;

    private DemoSchemaMigration() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !CONFIRMATION.equals(args[0])) {
            throw new IllegalArgumentException(
                    "Refusing migration. Pass the exact confirmation argument: " + CONFIRMATION);
        }
        if (System.getenv("CAMPSYNC_DEMO_DB_URL") != null) {
            throw new IllegalStateException("CAMPSYNC_DEMO_DB_URL is not supported; the target URL is fixed.");
        }

        String username = requiredEnvironmentVariable("CAMPSYNC_DEMO_MIGRATION_DB_USERNAME");
        String password = requiredEnvironmentVariable("CAMPSYNC_DEMO_MIGRATION_DB_PASSWORD");
        String expectedServerHostname =
                requiredEnvironmentVariable("CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME");
        verifyEmptyTarget(username, password, expectedServerHostname);

        Flyway flyway = Flyway.configure()
                .dataSource(DATABASE_URL, username, password)
                .locations("classpath:db/demo-migration", "classpath:db/migration")
                .defaultSchema(DATABASE_NAME)
                .schemas(DATABASE_NAME)
                .createSchemas(false)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        flyway.migrate();
        flyway.validate();
        System.out.println("Demo schema migrations completed and validated for " + DATABASE_NAME + ".");
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable is not set: " + name);
        }
        return value;
    }

    private static void verifyEmptyTarget(
            String username, String password, String expectedServerHostname) throws Exception {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, username, password);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT DATABASE(), @@hostname, @@port")) {
            if (!result.next() || !DATABASE_NAME.equals(result.getString(1))) {
                throw new IllegalStateException("Connected schema was not " + DATABASE_NAME + ".");
            }
            String serverHost = result.getString(2);
            int serverPort = result.getInt(3);
            if (!expectedServerHostname.equalsIgnoreCase(serverHost) || serverPort != DATABASE_PORT) {
                throw new IllegalStateException(
                        "Connected MySQL server identity did not match the locally configured demo server.");
            }
            List<String> existingTables = new ArrayList<>();
            try (ResultSet tables = connection.getMetaData().getTables(
                    DATABASE_NAME, null, "%", new String[] {"TABLE"})) {
                while (tables.next()) {
                    existingTables.add(tables.getString("TABLE_NAME"));
                }
            }
            if (!existingTables.isEmpty()) {
                throw new IllegalStateException(
                        "Refusing migration because " + DATABASE_NAME
                                + " is not empty. Inspect it manually; no migration was attempted.");
            }
            String targetIdentity = DATABASE_NAME + "@" + serverHost + ":" + serverPort;
            System.out.println("Empty target verified: " + targetIdentity);
            confirmConnectedTarget(targetIdentity);
        }
    }

    private static void confirmConnectedTarget(String targetIdentity) {
        System.out.println("Verify the MySQL server is the isolated demo instance.");
        System.out.println("Type the exact target identity shown above to authorize migration.");
        Scanner input = new Scanner(System.in);
        if (!input.hasNextLine() || !targetIdentity.equals(input.nextLine())) {
            throw new IllegalStateException("Target confirmation did not match; migration was not attempted.");
        }
    }
}
