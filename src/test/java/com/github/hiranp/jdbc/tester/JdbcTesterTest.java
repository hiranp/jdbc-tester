package com.github.hiranp.jdbc.tester;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.stream.Stream;

class JdbcTesterTest {

    @Test
    @DisplayName("Test isNullOrEmpty with null input")
    void testIsNullOrEmptyWithNull() {
        assertTrue(JdbcTester.isNullOrEmpty(null));
    }

    @Test
    @DisplayName("Test isNullOrEmpty with empty string")
    void testIsNullOrEmptyWithEmptyString() {
        assertTrue(JdbcTester.isNullOrEmpty(""));
    }

    @Test
    @DisplayName("Test isNullOrEmpty with valid string")
    void testIsNullOrEmptyWithValidString() {
        assertFalse(JdbcTester.isNullOrEmpty("test"));
    }

    @ParameterizedTest
    @MethodSource("provideUrlsForDriverTest")
    @DisplayName("Test driver determination for different URLs")
    void testDetermineDriver(String url, String expectedDriver) throws Exception {
        Method determineDriver = JdbcTester.class.getDeclaredMethod("determineDriver", String.class);
        determineDriver.setAccessible(true);
        assertEquals(expectedDriver, determineDriver.invoke(null, url));
    }

    private static Stream<Arguments> provideUrlsForDriverTest() {
        return Stream.of(
            Arguments.of("jdbc:oracle:thin:@localhost:1521:orcl", "oracle.jdbc.OracleDriver"),
            Arguments.of("jdbc:oracle:thin:@//localhost:1521/service", "oracle.jdbc.OracleDriver"),
            Arguments.of("jdbc:mysql://localhost:3306/test", "com.mysql.cj.jdbc.Driver"),
            Arguments.of("jdbc:postgresql://localhost:5432/test", "org.postgresql.Driver"),
            Arguments.of("jdbc:sqlserver://localhost:1433;databaseName=test", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
        );
    }

    @ParameterizedTest
    @MethodSource("provideUrlsForHostPortTest")
    @DisplayName("Test host and port extraction from URLs")
    void testExtractHostAndPort(String url, String expectedHost, String expectedPort) throws Exception {
        Method extractHostAndPort = JdbcTester.class.getDeclaredMethod("extractHostAndPort", String.class);
        extractHostAndPort.setAccessible(true);
        String[] result = (String[]) extractHostAndPort.invoke(null, url);
        assertEquals(expectedHost, result[0]);
        assertEquals(expectedPort, result[1]);
    }

    private static Stream<Arguments> provideUrlsForHostPortTest() {
        return Stream.of(
            // Oracle formats
            Arguments.of("jdbc:oracle:thin:@//localhost:1521/service", "localhost", "1521"),  // Service name format
            Arguments.of("jdbc:oracle:thin:@localhost:1521:sid", "localhost", "1521"),        // SID format
            Arguments.of("jdbc:oracle:thin:scott/tiger@//myhost:1521/myservicename", "myhost", "1521"),
            Arguments.of("jdbc:oracle:thin:@//myhost.example.com:1521/service", "myhost.example.com", "1521"),
            
            // Other database formats
            Arguments.of("jdbc:mysql://localhost:3306/test", "localhost", "3306"),
            Arguments.of("jdbc:mysql://localhost/test", "localhost", "3306"),  // default port
            Arguments.of("jdbc:mysql://localhost:3306/test?useSSL=false", "localhost", "3306"),
            Arguments.of("jdbc:postgresql://localhost:5432/test", "localhost", "5432"),
            Arguments.of("jdbc:sqlserver://localhost:1433;databaseName=test", "localhost", "1433")
        );
    }

    @Test
    @DisplayName("Test invalid JDBC URL format")
    void testInvalidJdbcUrl() {
        Method extractHostAndPort;
        try {
            extractHostAndPort = JdbcTester.class.getDeclaredMethod("extractHostAndPort", String.class);
            extractHostAndPort.setAccessible(true);
            assertThrows(IllegalArgumentException.class, () -> 
                extractHostAndPort.invoke(null, "invalid:url"));
        } catch (Exception e) {
            fail("Test setup failed");
        }
    }

    @Test
    @DisplayName("Test invalid Oracle JDBC URL formats")
    void testInvalidOracleJdbcUrls() {
        Method extractHostAndPort;
        try {
            extractHostAndPort = JdbcTester.class.getDeclaredMethod("extractHostAndPort", String.class);
            extractHostAndPort.setAccessible(true);
            
            // Test invalid Oracle URLs
            assertThrows(IllegalArgumentException.class, () -> 
                extractHostAndPort.invoke(null, "jdbc:oracle:thin:invalid_format"));
            assertThrows(IllegalArgumentException.class, () -> 
                extractHostAndPort.invoke(null, "jdbc:oracle:thin:@//localhost/noport"));
            assertThrows(IllegalArgumentException.class, () -> 
                extractHostAndPort.invoke(null, "jdbc:oracle:thin:@//localhost"));
        } catch (Exception e) {
            fail("Test setup failed");
        }
    }
}
