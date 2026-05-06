package org.marrok.amriirad.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.util.DatabaseConnection;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to handle compilation and displaying of JasperReports.
 * Copied/Adapted from GstockDz.
 */
public class ReportService {

    private static final Logger logger = LogManager.getLogger(ReportService.class);

    // Cache for compiled reports to avoid recompilation overhead
    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    public ReportService() {
        // Ensure temp dir is set for JasperReports compilation
        System.setProperty("jasper.reports.compile.temp", System.getProperty("java.io.tmpdir"));
    }

    /**
     * Compiles (or retrieves from cache) a JasperReport.
     * 
     * @param reportPath Path to the .jrxml file in resources (e.g., "/org/marrok/amriirad/report/annexe1_order.jrxml")
     * @return Compiled JasperReport object
     * @throws JRException If compilation fails
     */
    public JasperReport compileReport(String reportPath) throws JRException {
        if (reportCache.containsKey(reportPath)) {
            logger.debug("Retrieving report from cache: {}", reportPath);
            return reportCache.get(reportPath);
        }

        logger.info("Compiling report: {}", reportPath);
        try (InputStream reportStream = getClass().getResourceAsStream(reportPath)) {
            if (reportStream == null) {
                throw new JRException("Report file not found: " + reportPath);
            }
            JasperReport report = JasperCompileManager.compileReport(reportStream);
            reportCache.put(reportPath, report);
            return report;
        } catch (Exception e) {
            logger.error("Failed to compile report: " + reportPath, e);
            throw new JRException("Error compiling report", e);
        }
    }

    /**
     * Generates and displays a report using a Database Connection.
     */
    public void showReport(String reportPath, Map<String, Object> parameters) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            JasperReport jasperReport = compileReport(reportPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
            viewReport(jasperPrint);
        } catch (Exception e) {
            logger.error("Error generating report: " + reportPath, e);
            throw new RuntimeException("خطأ في توليد التقرير", e);
        }
    }

    /**
     * Generates and displays a report using a JRDataSource (e.g., Bean Collection).
     */
    public void showReport(String reportPath, Map<String, Object> parameters, JRDataSource dataSource) {
        try {
            JasperPrint jasperPrint = generateReport(reportPath, parameters, dataSource);
            viewReport(jasperPrint);
        } catch (Exception e) {
            logger.error("Error generating report with data source: " + reportPath, e);
            throw new RuntimeException("خطأ في توليد التقرير", e);
        }
    }

    public JasperPrint generateReport(String reportPath, Map<String, Object> parameters, JRDataSource dataSource)
            throws JRException {
        JasperReport jasperReport = compileReport(reportPath);
        return JasperFillManager.fillReport(jasperReport, parameters, dataSource);
    }

    public void viewReport(JasperPrint jasperPrint) {
        // Run on Swing Event Dispatch Thread
        javax.swing.SwingUtilities.invokeLater(() -> {
            JasperViewer viewer = new JasperViewer(jasperPrint, false); // false = dispose on close, don't exit app
            viewer.setTitle("طباعة المستند - نظام إيراد");
            viewer.setVisible(true);
            viewer.toFront();
            viewer.requestFocus();
        });
    }

    public void printReport(JasperPrint jasperPrint, boolean withPrintDialog) throws JRException {
        JasperPrintManager.printReport(jasperPrint, withPrintDialog);
    }
}
