package org.marrok.amriirad.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Responsible for initializing and maintaining the database schema.
 * All DDL statements are idempotent (CREATE TABLE IF NOT EXISTS).
 */
public class DatabaseSchemaManager {

    private static final Logger logger = LogManager.getLogger(DatabaseSchemaManager.class);

    public static void runMigrations() {
        logger.info("Running database schema migrations...");
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // -- app_user --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_user (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    username    VARCHAR(50) NOT NULL UNIQUE,
                    password    VARCHAR(255) NOT NULL,
                    full_name   VARCHAR(100),
                    role        ENUM('ADMIN', 'OPERATOR') NOT NULL DEFAULT 'OPERATOR',
                    is_active   BOOLEAN DEFAULT TRUE,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Seed default admin (password: admin) - in production, this would be hashed
            stmt.execute("""
                INSERT IGNORE INTO app_user (username, password, full_name, role)
                VALUES ('admin', 'admin', 'System Administrator', 'ADMIN')
            """);

            // -- fiscal_year --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS fiscal_year (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    year_label  VARCHAR(10) NOT NULL UNIQUE,
                    is_active   BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // -- debtor (المدين / الملزم بالدفع) --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS debtor (
                    id              INT AUTO_INCREMENT PRIMARY KEY,
                    full_name       VARCHAR(255) NOT NULL,
                    id_number       VARCHAR(50),
                    address         TEXT,
                    phone           VARCHAR(30),
                    bank_account    VARCHAR(100),
                    cnas_number     VARCHAR(50),
                    nif_number      VARCHAR(50),
                    debtor_type     ENUM('INDIVIDUAL', 'COMPANY', 'STATE_ENTITY') NOT NULL DEFAULT 'INDIVIDUAL',
                    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // -- budget_hierarchy (القسم / المادة / الفقرة) --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS budget_chapter (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    code        VARCHAR(20) NOT NULL UNIQUE,
                    label_ar    VARCHAR(255) NOT NULL,
                    label_fr    VARCHAR(255),
                    parent_id   INT,
                    level       TINYINT NOT NULL COMMENT '1=Titre, 2=Chapitre, 3=Article, 4=Paragraphe',
                    FOREIGN KEY (parent_id) REFERENCES budget_chapter(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // -- revenue_order (أمر الإيراد - Annexe 1 & 2) --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS revenue_order (
                    id                  INT AUTO_INCREMENT PRIMARY KEY,
                    order_number        VARCHAR(20) NOT NULL,
                    fiscal_year_id      INT NOT NULL,
                    issue_date          DATE,
                    debtor_id           INT NOT NULL,
                    budget_chapter_id   INT NOT NULL,
                    object_ar           TEXT COMMENT 'موضوع الإيراد',
                    amount              DECIMAL(18,2) NOT NULL,
                    amount_in_words_ar  TEXT,
                    status              ENUM('DRAFT','ISSUED','DISPATCHED','CANCELLED','REDUCED') NOT NULL DEFAULT 'DRAFT',
                    created_by          VARCHAR(100),
                    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    is_deleted          TINYINT(1) NOT NULL DEFAULT 0,
                    deleted_at          DATETIME DEFAULT NULL,
                    deleted_by          VARCHAR(100) DEFAULT NULL,
                    UNIQUE KEY uq_order_number_year (order_number, fiscal_year_id),
                    FOREIGN KEY (fiscal_year_id)    REFERENCES fiscal_year(id)      ON DELETE RESTRICT,
                    FOREIGN KEY (debtor_id)         REFERENCES debtor(id)           ON DELETE RESTRICT,
                    FOREIGN KEY (budget_chapter_id) REFERENCES budget_chapter(id)   ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // -- revenue_order_cancellation (أمر الإلغاء / التخفيض - Annexe 3 & 4) --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS revenue_order_cancellation (
                    id                  INT AUTO_INCREMENT PRIMARY KEY,
                    original_order_id   INT NOT NULL,
                    cancellation_type   ENUM('FULL_CANCEL', 'REDUCTION') NOT NULL,
                    cancellation_number VARCHAR(20),
                    cancellation_date   DATE NOT NULL,
                    reason_ar           TEXT NOT NULL,
                    reduced_amount      DECIMAL(18,2) COMMENT 'Populated only for REDUCTION type',
                    created_by          VARCHAR(100),
                    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_deleted          TINYINT(1) NOT NULL DEFAULT 0,
                    FOREIGN KEY (original_order_id) REFERENCES revenue_order(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // -- dispatch_slip (بوردرو الإرسال - Annexe 5) --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dispatch_slip (
                    id              INT AUTO_INCREMENT PRIMARY KEY,
                    slip_number     VARCHAR(20) NOT NULL,
                    fiscal_year_id  INT NOT NULL,
                    dispatch_date   DATE,
                    treasury_ref    VARCHAR(100),
                    total_amount    DECIMAL(18,2) NOT NULL,
                    created_by      VARCHAR(100),
                    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
                    FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // -- dispatch_slip_order (link table) --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dispatch_slip_order (
                    slip_id     INT NOT NULL,
                    order_id    INT NOT NULL,
                    PRIMARY KEY (slip_id, order_id),
                    FOREIGN KEY (slip_id)  REFERENCES dispatch_slip(id)  ON DELETE CASCADE,
                    FOREIGN KEY (order_id) REFERENCES revenue_order(id)  ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // -- institution_info --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS institution_info (
                    id              INT PRIMARY KEY DEFAULT 1,
                    name_ar         VARCHAR(255) NOT NULL,
                    name_fr         VARCHAR(255),
                    authorizing_officer_ar VARCHAR(255),
                    treasury_account_ar   VARCHAR(255),
                    rib_number      VARCHAR(50),
                    logo_path       VARCHAR(255),
                    address_ar      TEXT,
                    wilaya_ar       VARCHAR(100),
                    last_updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Seed default institution if not exists
            stmt.execute("""
                INSERT IGNORE INTO institution_info (id, name_ar, authorizing_officer_ar)
                VALUES (1, 'المدرسة العليا للقضاء', 'الأمين العام')
            """);

            // -- audit_log --
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                    table_name  VARCHAR(100) NOT NULL,
                    record_id   INT,
                    action      ENUM('INSERT','UPDATE','DELETE') NOT NULL,
                    performed_by VARCHAR(100),
                    performed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    details     TEXT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
            
            // -- Migration for existing debtor table --
            stmt.execute("ALTER TABLE debtor ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100)");
            stmt.execute("ALTER TABLE debtor ADD COLUMN IF NOT EXISTS cnas_number VARCHAR(50)");
            stmt.execute("ALTER TABLE debtor ADD COLUMN IF NOT EXISTS nif_number VARCHAR(50)");

            // -- Summary view --
            stmt.execute("""
                CREATE OR REPLACE VIEW v_revenue_order_summary AS
                SELECT
                    ro.id,
                    ro.order_number,
                    fy.year_label,
                    ro.issue_date,
                    d.full_name     AS debtor_name,
                    bc.code         AS budget_code,
                    bc.label_ar     AS budget_label,
                    ro.amount,
                    ro.status
                FROM revenue_order ro
                JOIN fiscal_year   fy ON ro.fiscal_year_id    = fy.id
                JOIN debtor        d  ON ro.debtor_id         = d.id
                JOIN budget_chapter bc ON ro.budget_chapter_id = bc.id;
            """);

            // 9. Seed Sample Data (Optional)
            stmt.execute("""
                INSERT IGNORE INTO budget_chapter (id, code, label_ar, label_fr, level) VALUES
                (1, '01', 'الإيرادات الجبائية', 'Recettes Fiscales', 1),
                (2, '01.01', 'الضرائب المباشرة', 'Impôts Directs', 2),
                (3, '02', 'إيرادات الأملاك', 'Produits du Domaine', 1);
            """);

            // 10. Auto-seed Fiscal Year if empty or none active
            boolean hasActive = false;
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM fiscal_year WHERE is_active = TRUE")) {
                if (rs.next() && rs.getInt(1) > 0) hasActive = true;
            }

            if (!hasActive) {
                try (var rsCount = stmt.executeQuery("SELECT id FROM fiscal_year ORDER BY year_label DESC LIMIT 1")) {
                    if (rsCount.next()) {
                        // Activate latest existing
                        stmt.execute("UPDATE fiscal_year SET is_active = TRUE WHERE id = " + rsCount.getInt(1));
                        logger.info("Automatically activated existing fiscal year ID: {}", rsCount.getInt(1));
                    } else {
                        // Create and activate current year
                        String currentYear = String.valueOf(java.time.LocalDate.now().getYear());
                        stmt.execute("INSERT INTO fiscal_year (year_label, is_active) VALUES ('" + currentYear + "', TRUE)");
                        logger.info("Auto-seeded and activated current fiscal year: {}", currentYear);
                    }
                }
            }

            logger.info("Schema migrations completed successfully.");

        } catch (SQLException e) {
            logger.error("Schema migration failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
