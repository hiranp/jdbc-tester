package com.github.hiranp.jdbc.tester;

import java.sql.*;
import java.util.Arrays;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

public class JdbcTester {
    private static final Logger LOGGER = Logger.getLogger(JdbcTester.class.getName());
    private static final String VERSION = "25.01.29";
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static boolean debugMode = false;

    static {
        // Configure logger
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(handler);
        LOGGER.setUseParentHandlers(false);
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static String determineDriver(String url) {
        if (url.startsWith("jdbc:oracle")) {
            return "oracle.jdbc.OracleDriver";
        } else if (url.startsWith("jdbc:mysql")) {
            return "com.mysql.cj.jdbc.Driver";
        } else if (url.startsWith("jdbc:postgresql")) {
            return "org.postgresql.Driver";
        } else if (url.startsWith("jdbc:sqlserver")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }
        throw new IllegalArgumentException("Unsupported JDBC URL: " + url);
    }

    private static String[] extractHostAndPort(String jdbcUrl) {
        LOGGER.log(Level.FINE, "Parsing JDBC URL: {0}", jdbcUrl);
        try {
            if (jdbcUrl.startsWith("jdbc:oracle:thin:")) {
                return parseOracleUrl(jdbcUrl);
            } else {
                return parseStandardUrl(jdbcUrl);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse JDBC URL: " + jdbcUrl, e);
            throw new IllegalArgumentException("Invalid JDBC URL format: " + jdbcUrl, e);
        }
    }

    private static String[] parseOracleUrl(String jdbcUrl) {
        String urlPart;
        if (jdbcUrl.contains("@//")) {
            // Handle service name format
            urlPart = jdbcUrl.substring(jdbcUrl.indexOf("@//") + 3);
            LOGGER.log(Level.FINE, "Parsing Oracle service name format. URL part: {0}", urlPart);
            
            int colonIndex = urlPart.indexOf(':');
            if (colonIndex == -1) {
                throw new IllegalArgumentException("Missing port number in Oracle URL");
            }
            
            String host = urlPart.substring(0, colonIndex);
            String remaining = urlPart.substring(colonIndex + 1);
            
            int slashIndex = remaining.indexOf('/');
            if (slashIndex == -1) {
                throw new IllegalArgumentException("Missing service name in Oracle URL");
            }
            
            String portStr = remaining.substring(0, slashIndex);
            LOGGER.log(Level.FINE, "Extracted host: {0}", host);
            LOGGER.log(Level.FINE, "Extracted port: {0}", portStr);
            
            // Validate port number
            try {
                Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number: " + portStr);
            }
            
            return new String[]{host, portStr};
        } else if (jdbcUrl.contains("@")) {
            // Handle SID format
            urlPart = jdbcUrl.substring(jdbcUrl.indexOf("@") + 1);
            int colonIndex = urlPart.indexOf(':');
            if (colonIndex == -1) {
                throw new IllegalArgumentException("Invalid Oracle JDBC URL format. Expected format: jdbc:oracle:thin:@host:port:sid");
            }
            String host = urlPart.substring(0, colonIndex);
            String remaining = urlPart.substring(colonIndex + 1);
            int nextColonIndex = remaining.indexOf(':');
            if (nextColonIndex == -1) {
                throw new IllegalArgumentException("Invalid Oracle JDBC URL format. Missing port or SID.");
            }
            String port = remaining.substring(0, nextColonIndex);
            return new String[]{host, port};
        }
        throw new IllegalArgumentException("Unsupported Oracle URL format");
    }

    private static String[] parseStandardUrl(String jdbcUrl) {
        try {
            String cleanUrl = jdbcUrl.replace("jdbc:", "");
            if (cleanUrl.contains("?")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.indexOf("?"));
            }
            LOGGER.log(Level.FINE, "Parsing standard URL: {0}", cleanUrl);
            
            URI uri = new URI(cleanUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            
            if (port == -1) {
                port = getDefaultPort(jdbcUrl);
            }
            
            LOGGER.log(Level.FINE, "Extracted host: {0}", host);
            LOGGER.log(Level.FINE, "Extracted/default port: {0}", port);
            
            return new String[]{host, String.valueOf(port)};
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Invalid standard JDBC URL format: " + jdbcUrl, e);
            throw new IllegalArgumentException("Invalid standard JDBC URL format: " + jdbcUrl, e);
        }
    }

    private static int getDefaultPort(String jdbcUrl) {
        if (jdbcUrl.contains("mysql")) return 3306;
        if (jdbcUrl.contains("oracle")) return 1521;
        if (jdbcUrl.contains("postgresql")) return 5432;
        if (jdbcUrl.contains("sqlserver")) return 1433;
        return -1;
    }

    private static boolean testNetworkConnectivity(String host, int port) {
        System.out.println("\n1. Testing network connectivity to " + host + ":" + port);
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            System.out.println("✓ Network connection successful");
            return true;
        } catch (IOException e) {
            System.out.println("✗ Network connection failed: " + e.getMessage());
            return false;
        }
    }

    public static void main(String... args) {
        String usr = null, pwd = null, url = null, sql = null;

        // Process command line arguments
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg.equals("-debug")) {
                    debugMode = true;
                    LOGGER.setLevel(Level.FINE);
                    Arrays.stream(LOGGER.getHandlers()).forEach(h -> h.setLevel(Level.FINE));
                    continue;
                }
                if (arg.startsWith("-url=")) {
                    url = arg.substring(5);
                } else if (arg.startsWith("-usr=")) {
                    usr = arg.substring(5);
                } else if (arg.startsWith("-pwd=")) {
                    pwd = arg.substring(5);
                } else if (arg.startsWith("-sqlf=")) {
                    sql = arg.substring(6);
                }
            }

            if (!isNullOrEmpty(url)) {
                try {
                    // Handle environment variables
                    if (usr != null && usr.startsWith("$")) {
                        usr = System.getenv(usr.substring(1));
                    }
                    if (pwd != null && pwd.startsWith("$")) {
                        pwd = System.getenv(pwd.substring(1));
                    }

                    String[] hostPort = extractHostAndPort(url);
                    String host = hostPort[0];
                    
                    try {
                        int port = Integer.parseInt(hostPort[1]);
                        // Test network connectivity first
                        if (testNetworkConnectivity(host, port)) {
                            String drv = determineDriver(url);
                            new JdbcTester().run(url, drv, usr, pwd, sql);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("ERROR: Invalid port number format: " + hostPort[1]);
                        System.err.println("Please check the JDBC URL format");
                        e.printStackTrace();
                    }
                } catch (Exception ex) {
                    System.err.println("ERROR: Failed to process JDBC URL: " + url);
                    System.err.println("Cause: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                printUsage(args);
            }
        } else {
            printUsage(args);
        }
    }

    private static void printUsage(String[] args) {
        System.out.println("Missing or incorrect arguments: " + Arrays.toString(args));
        System.out.printf("JDBC Tester v%s - Test JDBC connectivity and execute a query%n%n", VERSION);
        System.out.println("Usage: -url=jdbc_url [-usr=jdbc_usr] [-pwd=jdbc_pwd] [-sqlf=/path_to/sql_statement] [-debug]\n");
        System.out.println("Examples:");
        System.out.println("  Oracle:");
        System.out.println("    java -jar jdbc-tester.jar -url='jdbc:oracle:thin:@//localhost:1521/service' -usr=$DB_USER -pwd=$DB_PASS  // Service name format (recommended)");
        System.out.println("    java -jar jdbc-tester.jar -url='jdbc:oracle:thin:@localhost:1521:sid' -usr=$DB_USER -pwd=$DB_PASS       // SID format (legacy)");
        System.out.println("  MySQL:");
        System.out.println("    java -jar jdbc-tester.jar -url='jdbc:mysql://testmysql.local:3306/testdb' -usr=$DB_USER -pwd=$DB_PASS");
        System.out.println("  PostgreSQL:");
        System.out.println("    java -jar jdbc-tester.jar -url='jdbc:postgresql://localhost:5432/testdb' -usr=$DB_USER -pwd=$DB_PASS");
        System.out.println("  SQL Server:");
        System.out.println("    java -jar jdbc-tester.jar -url='jdbc:sqlserver://localhost:1433;databaseName=testdb' -usr=$DB_USER -pwd=$DB_PASS\n");
    }

    private static void handleSQLException(SQLException se, String context) {
        System.err.println("\n❌ Database error occurred during: " + context);
        int errorCount = 1;
        do {
            System.err.println("\nError #" + errorCount++);
            System.err.println("SQL State: " + se.getSQLState());
            System.err.println("Error Code: " + se.getErrorCode());
            System.err.println("Message: " + se.getMessage());
            System.err.println("Cause: " + se.getCause());
            
            // Print stack trace for debugging
            se.printStackTrace(System.err);
            
            se = se.getNextException();
        } while (se != null);
    }

    private void run(String url, String drv, String usr, String pwd, String sql) {
        try {
            System.out.println("\n2. Testing JDBC connectivity");
            System.out.println("Loading driver: " + drv);
            Class.forName(drv);

            System.out.println("Connecting to database URL: " + url);
            try (Connection con = DriverManager.getConnection(url, usr, pwd)) {
                System.out.println("✓ Database connection successful");

                System.out.println("\n3. Executing test query");
                if (sql != null) {
                    executeCustomQuery(con, sql);
                } else {
                    executeDefaultQuery(con, url);
                }
            } catch (SQLException se) {
                handleSQLException(se, "database operations");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("\n❌ JDBC Driver not found: " + drv);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void executeCustomQuery(Connection con, String sqlFile) throws SQLException {
        try (BufferedReader br = new BufferedReader(new FileReader(sqlFile))) {
            String sql = br.lines().collect(Collectors.joining(" "));
            System.out.println("Executing custom query: " + sql);
            executeQuery(con, sql);
        } catch (IOException ex) {
            System.out.println("✗ Cannot read SQL file: " + sqlFile);
        }
    }

    private String getTimestampQuery(String url) {
        if (url.startsWith("jdbc:oracle")) {
            return "SELECT SYSDATE FROM DUAL";
        } else if (url.startsWith("jdbc:sqlserver")) {
            return "SELECT GETDATE()";
        } else {
            // MySQL and PostgreSQL use standard syntax
            return "SELECT CURRENT_TIMESTAMP";
        }
    }
    
    private void executeDefaultQuery(Connection con, String url) throws SQLException {
        String timestampQuery = getTimestampQuery(url);
        executeQuery(con, timestampQuery);
    }

    private void executeQuery(Connection con, String sql) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                System.out.println("✓ Query executed successfully. Result: " + rs.getTimestamp(1));
            } else {
                System.out.println("⚠ Query returned no results");
            }
        } catch (SQLException se) {
            handleSQLException(se, "executing query: " + sql);
            throw se; // Re-throw to handle in the calling method
        }
    }
}