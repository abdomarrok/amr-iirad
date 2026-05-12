package org.marrok.amriirad.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.model.User;
import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.BudgetChapter;
import org.marrok.amriirad.model.ZeroValueDecision;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

/**
 * Service for exporting financial data to various formats (CSV, etc.).
 */
public class ExportService {
    private static final Logger logger = LogManager.getLogger(ExportService.class);

    /**
     * Standardized file chooser for CSV exports.
     */
    public File chooseCSVFile(javafx.stage.Window owner, String baseName) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("تصدير البيانات / Export Data");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName(baseName + "_" + java.time.LocalDate.now() + ".csv");
        return fileChooser.showSaveDialog(owner);
    }

    /**
     * Export a list of revenue orders to a CSV file.
     * Includes BOM for correct Arabic rendering in Excel.
     */
    public void exportOrdersToCSV(List<RevenueOrder> orders, File file) throws IOException {
        logger.info("Exporting {} orders to CSV: {}", orders.size(), file.getAbsolutePath());
        
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM for Excel compatibility
            writer.write('\ufeff');

            // Header
            StringJoiner header = new StringJoiner(",");
            header.add("Order Number")
                  .add("Issue Date")
                  .add("Debtor")
                  .add("Object (AR)")
                  .add("Object (FR)")
                  .add("Budget Code")
                  .add("Amount")
                  .add("Status");
            writer.write(header.toString() + "\n");

            // Data
            for (RevenueOrder order : orders) {
                StringJoiner row = new StringJoiner(",");
                row.add(escapeCsv(order.getOrderNumber()))
                   .add(escapeCsv(order.getIssueDate() != null ? order.getIssueDate().toString() : ""))
                   .add(escapeCsv(order.getDebtor() != null ? order.getDebtor().getFullName() : ""))
                   .add(escapeCsv(order.getObjectAr()))
                   .add(escapeCsv(order.getObjectFr()))
                   .add(escapeCsv(order.getBudgetChapter() != null ? order.getBudgetChapter().getCode() : ""))
                   .add(order.getAmount() != null ? order.getAmount().toString() : "0.00")
                   .add(escapeCsv(order.getStatus() != null ? order.getStatus().name() : ""));
                writer.write(row.toString() + "\n");
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Export a list of users to a CSV file.
     */
    public void exportUsersToCSV(List<User> users, File file) throws IOException {
        logger.info("Exporting {} users to CSV: {}", users.size(), file.getAbsolutePath());
        
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM for Excel compatibility
            writer.write('\ufeff');

            // Header
            StringJoiner header = new StringJoiner(",");
            header.add("ID")
                  .add("Username")
                  .add("Full Name")
                  .add("Role")
                  .add("Status");
            writer.write(header.toString() + "\n");

            // Data
            for (User user : users) {
                StringJoiner row = new StringJoiner(",");
                row.add(String.valueOf(user.getId()))
                   .add(escapeCsv(user.getUsername()))
                   .add(escapeCsv(user.getFullName()))
                   .add(escapeCsv(user.getRoleName() != null ? user.getRoleName() : ""))
                   .add(user.isActive() ? "نشط" : "مجمد");
                writer.write(row.toString() + "\n");
            }
        }
    }

    /**
     * Export a list of debtors to a CSV file.
     */
    public void exportDebtorsToCSV(List<Debtor> debtors, File file) throws IOException {
        logger.info("Exporting {} debtors to CSV: {}", debtors.size(), file.getAbsolutePath());
        
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM for Excel compatibility
            writer.write('\ufeff');

            // Header
            StringJoiner header = new StringJoiner(",");
            header.add("ID")
                  .add("Full Name")
                  .add("ID Number")
                  .add("Type")
                  .add("Phone")
                  .add("Address");
            writer.write(header.toString() + "\n");

            // Data
            for (Debtor debtor : debtors) {
                StringJoiner row = new StringJoiner(",");
                row.add(String.valueOf(debtor.getId()))
                   .add(escapeCsv(debtor.getFullName()))
                   .add(escapeCsv(debtor.getIdNumber()))
                   .add(escapeCsv(debtor.getDebtorType() != null ? debtor.getDebtorType().getArabicLabel() : ""))
                   .add(escapeCsv(debtor.getPhone()))
                   .add(escapeCsv(debtor.getAddress()));
                writer.write(row.toString() + "\n");
            }
        }
    }

    /**
     * Export a list of budget chapters to a CSV file.
     */
    public void exportBudgetChaptersToCSV(List<BudgetChapter> chapters, File file) throws IOException {
        logger.info("Exporting {} budget chapters to CSV: {}", chapters.size(), file.getAbsolutePath());
        
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM for Excel compatibility
            writer.write('\ufeff');

            // Header
            StringJoiner header = new StringJoiner(",");
            header.add("Code")
                  .add("Label (AR)")
                  .add("Label (FR)");
            writer.write(header.toString() + "\n");

            // Data
            for (BudgetChapter chapter : chapters) {
                StringJoiner row = new StringJoiner(",");
                row.add(escapeCsv(chapter.getCode()))
                   .add(escapeCsv(chapter.getLabelAr()))
                   .add(escapeCsv(chapter.getLabelFr()));
                writer.write(row.toString() + "\n");
            }
        }
    }

    /**
     * Export a list of zero value decisions to a CSV file.
     */
    public void exportZeroValueDecisionsToCSV(List<ZeroValueDecision> decisions, File file) throws IOException {
        logger.info("Exporting {} zero value decisions to CSV: {}", decisions.size(), file.getAbsolutePath());

        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');

            StringJoiner header = new StringJoiner(",");
            header.add("Decision Number")
                  .add("Decision Date")
                  .add("Total Amount")
                  .add("Created By")
                  .add("Details Count");
            writer.write(header.toString() + "\n");

            for (ZeroValueDecision decision : decisions) {
                StringJoiner row = new StringJoiner(",");
                row.add(escapeCsv(decision.getDecisionNumber()))
                   .add(escapeCsv(decision.getDecisionDate() != null ? decision.getDecisionDate().toString() : ""))
                   .add(decision.getTotalAmount() != null ? decision.getTotalAmount().toString() : "0.00")
                   .add(escapeCsv(decision.getCreatedBy()))
                   .add(String.valueOf(decision.getDetails() != null ? decision.getDetails().size() : 0));
                writer.write(row.toString() + "\n");
            }
        }
    }
}
