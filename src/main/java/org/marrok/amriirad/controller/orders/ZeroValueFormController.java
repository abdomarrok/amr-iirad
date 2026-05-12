package org.marrok.amriirad.controller.orders;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.marrok.amriirad.controller.BaseFormController;
import org.marrok.amriirad.core.AppContext;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.*;
import org.marrok.amriirad.service.ZeroValueService;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.service.RevenueOrderService;
import org.marrok.amriirad.util.DialogHelper;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ZeroValueFormController extends BaseFormController implements Initializable {

    @FXML private TextField decisionNumField;
    @FXML private DatePicker decisionDatePicker;
    @FXML private TableView<ZeroValueOrderDetail> selectedOrdersTable;
    @FXML private TableColumn<ZeroValueOrderDetail, String> orderNumCol;
    @FXML private TableColumn<ZeroValueOrderDetail, BigDecimal> amountCol;
    @FXML private TableColumn<ZeroValueOrderDetail, String> debtorCol;
    
    @FXML private TextArea reasonsArea;
    @FXML private TextArea actionsArea;
    @FXML private TextArea opinionArea;
    @FXML private TextArea obsArea;
    @FXML private Label totalLabel;

    private final ZeroValueService zeroService;
    private final RevenueOrderService orderService;
    private final FiscalYearRepository fiscalRepo;
    private final ObservableList<ZeroValueOrderDetail> detailsList = FXCollections.observableArrayList();

    public ZeroValueFormController(ZeroValueService zeroService,
                                 RevenueOrderService orderService,
                                 FiscalYearRepository fiscalRepo,
                                 ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.zeroService = zeroService;
        this.orderService = orderService;
        this.fiscalRepo = fiscalRepo;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        decisionDatePicker.setValue(LocalDate.now());
        setupTable();
        setupFieldsSync();
    }

    private void setupTable() {
        orderNumCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRevenueOrder().getOrderNumber()));
        amountCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getRevenueOrder().getAmount()));
        debtorCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRevenueOrder().getDebtor().getFullName()));
        
        selectedOrdersTable.setItems(detailsList);
    }

    private void setupFieldsSync() {
        selectedOrdersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (oldV != null) saveCurrentFields(oldV);
            if (newV != null) loadFields(newV);
            else clearFields();
        });
    }

    private void saveCurrentFields(ZeroValueOrderDetail detail) {
        detail.setNonCollectionReasons(reasonsArea.getText());
        detail.setEnforcementActions(actionsArea.getText());
        detail.setDeliberativeOpinion(opinionArea.getText());
        detail.setAccountantObservations(obsArea.getText());
    }

    private void loadFields(ZeroValueOrderDetail detail) {
        reasonsArea.setText(detail.getNonCollectionReasons());
        actionsArea.setText(detail.getEnforcementActions());
        opinionArea.setText(detail.getDeliberativeOpinion());
        obsArea.setText(detail.getAccountantObservations());
    }

    private void clearFields() {
        reasonsArea.clear();
        actionsArea.clear();
        opinionArea.clear();
        obsArea.clear();
    }

    @FXML
    private void handleAddOrder() {
        concurrencyManager.runAsync(
            () -> {
                int activeYear = fiscalRepo.findActive().map(FiscalYear::getId).orElse(0);
                return orderService.findAllByFiscalYear(activeYear).stream()
                    .filter(o -> o.getStatus() == OrderStatus.ISSUED || o.getStatus() == OrderStatus.DISPATCHED)
                    .collect(Collectors.toList());
            },
            issuedOrders -> {
                ChoiceDialog<RevenueOrder> dialog = new ChoiceDialog<>(null, issuedOrders);
                dialog.setTitle("إضافة أمر إيراد");
                dialog.setHeaderText("اختر أمر إيراد لتحويله لقيمة منعدمة");
                dialog.showAndWait().ifPresent(order -> {
                    ZeroValueOrderDetail detail = new ZeroValueOrderDetail();
                    detail.setRevenueOrder(order);
                    detailsList.add(detail);
                    updateTotal();
                });
            },
            err -> showError(err.getMessage())
        );
    }

    @FXML
    private void handleRemoveOrder() {
        ZeroValueOrderDetail selected = selectedOrdersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            detailsList.remove(selected);
            updateTotal();
        }
    }

    private void updateTotal() {
        BigDecimal total = detailsList.stream()
            .map(d -> d.getRevenueOrder().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalLabel.setText(String.format("المجموع: %,.2f د.ج", total));
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        // Save last edited fields if something is selected
        ZeroValueOrderDetail current = selectedOrdersTable.getSelectionModel().getSelectedItem();
        if (current != null) saveCurrentFields(current);

        ZeroValueDecision decision = new ZeroValueDecision();
        decision.setDecisionNumber(decisionNumField.getText());
        decision.setDecisionDate(decisionDatePicker.getValue());
        decision.setTotalAmount(detailsList.stream()
            .map(d -> d.getRevenueOrder().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        decision.setDetails(detailsList);
        decision.setCreatedBy(AppContext.getInstance().getCurrentUser());

        concurrencyManager.runAsync(
            () -> {
                int yearId = fiscalRepo.findActive().map(FiscalYear::getId).orElse(0);
                FiscalYear fy = new FiscalYear(); fy.setId(yearId);
                decision.setFiscalYear(fy);
                return zeroService.createDecision(decision);
            },
            res -> {
                org.marrok.amriirad.util.DialogHelper.showInfo("نجاح", "تم حفظ قرار التسوية بنجاح.");
                closeWindow();
                runOnSuccess();
            },
            err -> showError(err.getMessage())
        );
    }

    @Override
    protected boolean validateForm() {
        if (detailsList.isEmpty()) {
            showError("يجب إضافة أمر إيراد واحد على الأقل.");
            return false;
        }
        return true;
    }

    @Override
    protected org.apache.logging.log4j.Logger getLogger() {
        return org.apache.logging.log4j.LogManager.getLogger(ZeroValueFormController.class);
    }
}
