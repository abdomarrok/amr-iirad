package org.marrok.amriirad.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the HikariCP connection pool.
 * Supports Local (MariaDB4j embedded) and Server (remote MariaDB) modes.
 */
public class DatabaseConnection {

    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);

    // Connection parameters (defaults for local embedded mode)
    private static String DATABASE_NAME = "amr_iirad";
    private static String DATABASE_HOST = "localhost";
    private static int    DATABASE_PORT = 3307;
    private static String DATABASE_USER = "root";
    private static String DATABASE_PASSWORD = "";

    private static HikariDataSource dataSource;
    private static EmbeddedDatabase embeddedDatabase;

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    /**
     * Initializes the connection pool based on the given AppMode.
     * For LOCAL mode, starts the embedded MariaDB4j instance first.
     */
    public static void initialize(AppMode mode) throws Exception {
        if (mode == AppMode.LOCAL) {
            logger.info("Starting embedded MariaDB4j database...");
            embeddedDatabase = new EmbeddedDatabase();
            embeddedDatabase.start();
            DATABASE_PORT = embeddedDatabase.getPort();
            logger.info("Embedded DB started on port {}", DATABASE_PORT);
        }
        initializePool();
        // Ensure target database exists
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE IF NOT EXISTS `" + DATABASE_NAME + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            s.execute("USE `" + DATABASE_NAME + "`;");
        }
        // Re-initialize pool pointing to the named database
        dataSource.close();
        initializePool();
    }

    /**
     * Configures server connection parameters (used by ServerConfigController).
     */
    public static void configure(String host, int port, String user, String password) {
        DATABASE_HOST     = host;
        DATABASE_PORT     = port;
        DATABASE_USER     = user;
        DATABASE_PASSWORD = password;
    }

    private static void initializePool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + DATABASE_HOST + ":" + DATABASE_PORT + "/" + DATABASE_NAME
                + "?createDatabaseIfNotExist=true&characterEncoding=UTF-8&useUnicode=true");
        config.setUsername(DATABASE_USER);
        config.setPassword(DATABASE_PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("AmrIirad-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        logger.info("HikariCP pool initialized: {}", config.getPoolName());
    }

    // =========================================================================
    // CONNECTION ACCESS
    // =========================================================================

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not initialized.");
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP pool shut down.");
        }
        if (embeddedDatabase != null) {
            embeddedDatabase.stop();
            logger.info("Embedded MariaDB4j stopped.");
        }
    }

    // =========================================================================
    // BACKUP / RESTORE  (progress callback interface)
    // =========================================================================

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(double progress, String message);
    }

    /**
     * Backup database to a GZIP-compressed SQL file.
     */
    public static void backupDatabase(String backupPath, ProgressCallback callback) throws SQLException, IOException {
        if (!backupPath.endsWith(".gz")) backupPath += ".gz";
        logger.info("backupDatabase to {}", backupPath);

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.util.zip.GZIPOutputStream gzipOut = new java.util.zip.GZIPOutputStream(
                     new java.io.FileOutputStream(backupPath));
             java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                     gzipOut, java.nio.charset.StandardCharsets.UTF_8)) {

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                while (rs.next()) tables.add(rs.getString(1));
            }

            writer.write("-- Amr-Iirad Backup\n-- Created: " + java.time.LocalDateTime.now() + "\n\n");
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

            int i = 0;
            for (String table : tables) {
                i++;
                if (callback != null) callback.onProgress((double) i / tables.size(), "تصدير: " + table);
                writeTableToSQL(conn, table, writer);
            }

            writer.write("\nSET FOREIGN_KEY_CHECKS=1;\n");
            if (callback != null) callback.onProgress(1.0, "اكتمل النسخ الاحتياطي");
        }
    }

    public static void backupDatabase(String backupPath) throws SQLException, IOException {
        backupDatabase(backupPath, null);
    }

    private static void writeTableToSQL(Connection conn, String tableName, java.io.Writer writer)
            throws SQLException, IOException {
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rsCreate = stmt.executeQuery("SHOW CREATE TABLE `" + tableName + "`")) {
                if (rsCreate.next()) {
                    writer.write("\n-- Table: `" + tableName + "`\n");
                    writer.write("DROP TABLE IF EXISTS `" + tableName + "`;\n");
                    writer.write(rsCreate.getString(2) + ";\n\n");
                }
            }
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM `" + tableName + "`")) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder("INSERT INTO `" + tableName + "` VALUES(");
                    for (int i = 1; i <= cols; i++) {
                        if (rs.getObject(i) == null) {
                            sb.append("NULL");
                        } else {
                            sb.append("'").append(rs.getString(i).replace("'", "''")).append("'");
                        }
                        if (i < cols) sb.append(", ");
                    }
                    sb.append(");\n");
                    writer.write(sb.toString());
                }
                writer.write("\n");
            }
        }
    }

    // =========================================================================
    // IMPORT
    // =========================================================================

    public static void importDatabase(String sqlFilePath, ProgressCallback callback) throws Exception {
        logger.info("importDatabase: {}", sqlFilePath);
        boolean isGzipped = isGzipFile(sqlFilePath);
        long totalLines = countLines(sqlFilePath, isGzipped);

        try (Connection conn = getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 BufferedReader br = createReader(sqlFilePath, isGzipped)) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=0;");
                StringBuilder query = new StringBuilder();
                String line;
                long lineCount = 0;
                while ((line = br.readLine()) != null) {
                    lineCount++;
                    if (callback != null && lineCount % 100 == 0) {
                        callback.onProgress(totalLines > 0 ? (double) lineCount / totalLines : 0,
                                "جاري الاستيراد... " + lineCount + " سطر");
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--") || line.startsWith("#")) continue;
                    query.append(line).append(" ");
                    if (line.endsWith(";")) {
                        try { stmt.execute(query.toString().trim()); } catch (SQLException e) {
                            logger.warn("SQL error (line {}): {}", lineCount, e.getMessage());
                        }
                        query.setLength(0);
                    }
                }
                stmt.execute("SET FOREIGN_KEY_CHECKS=1;");
                conn.commit();
                if (callback != null) callback.onProgress(1.0, "اكتمل الاستيراد");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public static void importDatabase(String sqlFilePath) throws Exception {
        importDatabase(sqlFilePath, null);
    }

    private static boolean isGzipFile(String filePath) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
            byte[] magic = new byte[2];
            if (fis.read(magic) == 2) return (magic[0] == (byte) 0x1f && magic[1] == (byte) 0x8b);
        } catch (IOException e) { logger.warn("Cannot detect file type."); }
        return false;
    }

    private static BufferedReader createReader(String filePath, boolean isGzipped) throws IOException {
        if (isGzipped) {
            return new BufferedReader(new java.io.InputStreamReader(
                    new java.util.zip.GZIPInputStream(new java.io.FileInputStream(filePath)),
                    java.nio.charset.StandardCharsets.UTF_8));
        }
        return new BufferedReader(new FileReader(filePath, java.nio.charset.StandardCharsets.UTF_8));
    }

    private static long countLines(String filePath, boolean isGzipped) {
        long count = 0;
        try (BufferedReader br = createReader(filePath, isGzipped)) {
            while (br.readLine() != null) count++;
        } catch (IOException e) { return 0; }
        return count;
    }
}
