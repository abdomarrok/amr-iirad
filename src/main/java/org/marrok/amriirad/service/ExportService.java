package org.marrok.amriirad.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.RevenueOrder;

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
}
