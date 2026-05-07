package org.marrok.amriirad.controller.settings;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.AuditLog;
import org.marrok.amriirad.service.AuditLogService;
import org.marrok.amriirad.core.ConcurrencyManager;

import java.time.format.DateTimeFormatter;

public class AuditLogController {
    private static final Logger logger = LogManager.getLogger(AuditLogController.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogService auditLogService;
    private final ConcurrencyManager concurrencyManager;

    @FXML private org.marrok.amriirad.controller.shared.TopBarController topBarController;
    @FXML private TableView<AuditLog> logTable;
    @FXML private TableColumn<AuditLog, Long> colId;
    @FXML private TableColumn<AuditLog, String> colTable;
    @FXML private TableColumn<AuditLog, Integer> colRecordId;
    @FXML private TableColumn<AuditLog, String> colAction;
    @FXML private TableColumn<AuditLog, String> colUser;
    @FXML private TableColumn<AuditLog, String> colDate;
    @FXML private TableColumn<AuditLog, String> colDetails;
    @FXML private TextField searchField;

    public AuditLogController(AuditLogService auditLogService, ConcurrencyManager concurrencyManager) {
        this.auditLogService = auditLogService;
        this.concurrencyManager = concurrencyManager;
    }

    @FXML
    public void initialize() {
        if (topBarController != null) {
            topBarController.setBackVisible(true);
        }
        setupColumns();
        loadLogs();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTable.setCellValueFactory(new PropertyValueFactory<>("tableName"));
        colRecordId.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("performedBy"));
        
        colDate.setCellValueFactory(cellData -> {
            var date = cellData.getValue().getPerformedAt();
            return new javafx.beans.property.SimpleStringProperty(date != null ? date.format(formatter) : "");
        });
        
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
    }

    private void loadLogs() {
        concurrencyManager.runAsync(
            () -> auditLogService.getAllLogs(),
            logs -> logTable.setItems(FXCollections.observableArrayList(logs)),
            err -> logger.error("Failed to load audit logs", err)
        );
    }

    @FXML
    private void handleRefresh() {
        loadLogs();
    }
}
