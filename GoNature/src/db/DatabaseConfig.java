package db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class DatabaseConfig {

    private static final String CONFIG_FILE = "db_config.properties";

    private String dbHost;
    private String dbName;
    private String dbUser;
    private String dbPassword;

    public DatabaseConfig() {
        // Set default configurations for GoNature DB
        this.dbHost = "localhost";
        this.dbName = "gonature";
        this.dbUser = "root";
        this.dbPassword = "Rahaf28803*";
    }

    /**
     * Loads the database configuration parameters from properties file.
     * If the configuration file does not exist, saves the default values.
     */
    public void loadConfig() {
        Properties prop = new Properties();
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            saveDefaultConfig();
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            prop.load(fis);
            this.dbHost = prop.getProperty("db.host", "localhost");
            this.dbName = prop.getProperty("db.name", "gonature_db");
            this.dbUser = prop.getProperty("db.user", "root");
            this.dbPassword = prop.getProperty("db.password", "password");
        } catch (IOException e) {
            System.out.println("Error loading db config, using defaults: " + e.getMessage());
        }
    }

    /**
     * Saves the default configuration properties to file.
     */
    private void saveDefaultConfig() {
        Properties prop = new Properties();
        prop.setProperty("db.host", dbHost);
        prop.setProperty("db.name", dbName);
        prop.setProperty("db.user", dbUser);
        prop.setProperty("db.password", dbPassword);

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            prop.store(fos, "GoNature DB Configuration File");
        } catch (IOException e) {
            System.out.println("Error saving default config: " + e.getMessage());
        }
    }

    public String getDbHost() { return dbHost; }
    public String getDbName() { return dbName; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
}
