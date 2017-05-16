package crowler.model;

import java.util.Properties;

/**
 * Created by vasily on 12.04.17.
 */
public class DBCredentials {

    private final String SERVER = "127.0.0.1";
    private final int PORT = 3306;
    private final String DB_NAME = "gb";
    private final String DB_USER = "gb";
    private final String DB_PASS = "1qasde32w";

    private static DBCredentials ourInstance = new DBCredentials();

    public static DBCredentials getInstance() {
        return ourInstance;
    }

    private DBCredentials() {
    }

    public String getSERVER() {
        return SERVER;
    }

    public int getPORT() {
        return PORT;
    }

    public String getDB_NAME() {
        return DB_NAME;
    }

    public String getDB_USER() {
        return DB_USER;
    }

    public String getDB_PASS() {
        return DB_PASS;
    }

    public String getConnectionURL() {
        return "jdbc:mysql://" + SERVER + ":" + PORT + "/" + DB_NAME;
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("user", getDB_USER());
        properties.setProperty("password", getDB_PASS());
        properties.setProperty("useUnicode","true");
        properties.setProperty("characterEncoding", "UTF-8");
        properties.setProperty("useSSL", "false");

        return properties;
    }
}
