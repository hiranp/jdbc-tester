# JDBC Tester v25.01.29

A command-line utility for testing JDBC database connections and executing simple queries.

## Features
- Automatic JDBC driver detection
- Network connectivity testing
- Support for Oracle, MySQL, PostgreSQL, and SQL Server
- Environment variable support for credentials
- Custom SQL query execution
- Detailed error reporting

## Requirements
- Java 8 or later
- Maven 3.6+ (for building)

## Installation

### Using Pre-built JAR
Download the latest release from the releases page.

### Building from Source
```bash
git clone https://github.com/hiranp/jdbc-tester.git
cd jdbc-tester
mvn clean package
```

## Usage

### Basic Syntax
```bash
java -jar jdbc-tester.jar -url=<jdbc_url> [-usr=<username>] [-pwd=<password>] [-sqlf=<sql_file>]
```

### Database Examples

#### Oracle
```bash
# Using SID
java -jar jdbc-tester.jar -url='jdbc:oracle:thin:@localhost:1521:orcl' -usr=$DB_USER -pwd=$DB_PASS

# Using Service Name
java -jar jdbc-tester.jar -url='jdbc:oracle:thin:@//localhost:1521/service' -usr=$DB_USER -pwd=$DB_PASS
```

#### MySQL
```bash
java -jar jdbc-tester.jar -url='jdbc:mysql://localhost:3306/testdb' -usr=$DB_USER -pwd=$DB_PASS
```

#### PostgreSQL
```bash
java -jar jdbc-tester.jar -url='jdbc:postgresql://localhost:5432/testdb' -usr=$DB_USER -pwd=$DB_PASS
```

#### SQL Server
```bash
java -jar jdbc-tester.jar -url='jdbc:sqlserver://localhost:1433;databaseName=testdb' -usr=$DB_USER -pwd=$DB_PASS
```

### Custom SQL Query
```bash
java -jar jdbc-tester.jar -url='jdbc:mysql://localhost:3306/testdb' -usr=$DB_USER -pwd=$DB_PASS -sqlf=/path/to/query.sql
```

## Environment Variables
- `DB_USER`: Database username
- `DB_PASS`: Database password

## Troubleshooting

### Common Issues
1. Network Connectivity: Ensure the database host is reachable
2. Driver Missing: Verify JDBC driver is in classpath
3. Authentication: Check credentials and permissions
4. URL Format: Verify correct JDBC URL format for your database

### Exit Codes
- 0: Success
- 1: General error
- 2: Network error
- 3: Database error

## License
MIT License

## Contributing
Pull requests are welcome. For major changes, please open an issue first.