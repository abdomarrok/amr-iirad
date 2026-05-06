package org.marrok.amriirad.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.DebtorType;
import org.marrok.amriirad.repository.DebtorRepository;

import java.net.URL;
import java.util.ResourceBundle;

public class DebtorListController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DebtorListController.class);

    @FXML private TableView<Debtor> tableView;
    @FXML private TableColumn<Debtor, Integer> colId;
    @FXML private TableColumn<Debtor, String> colFullName;
    @FXML private TableColumn<Debtor, String> colIdNumber;
    @FXML private TableColumn<Debtor, String> colType;
    @FXML private TableColumn<Debtor, String> colPhone;
    @FXML private TableColumn<Debtor, String> colAddress;

    @FXML private TextField searchField;
    @FXML private ComboBox<DebtorType> typeFilterCombo;
    @FXML private ProgressIndicator loadingIndicator;

    private ObservableList<Debtor> masterList;
    private FilteredList<Debtor> filteredList;

    private final DebtorRepository debtorRepo;

    public DebtorListController(DebtorRepository debtorRepo) {
        this.debtorRepo = debtorRepo;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initColumns();
        setupFilters();
        setupTableInteraction();
        loadDataAsync();
    }

    private void initColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colIdNumber.setCellValueFactory(new PropertyValueFactory<>("idNumber"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        
        colType.setCellValueFactory(cell -> {
            var type = cell.getValue().getDebtorType();
            String arType = "-";
            if (type != null) {
                switch(type) {
                    case INDIVIDUAL: arType = "شخص طبيعي"; break;
                    case COMPANY: arType = "شخص معنوي (شركة)"; break;
                    case STATE_ENTITY: arType = "هيئة عمومية"; break;
                }
            }
            return new SimpleStringProperty(arType);
        });
    }

    private void setupFilters() {
        typeFilterCombo.getItems().addAll(DebtorType.values());
        typeFilterCombo.getItems().add(0, null); // "All"

        searchField.textProperty().addListener((obs, oldV, newV) -> updatePredicate());
        typeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    private void updatePredicate() {
        if (filteredList == null) return;
        
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        DebtorType type = typeFilterCombo.getValue();

        filteredList.setPredicate(debtor -> {
            if (type != null && debtor.getDebtorType() != type) {
                return false;
            }
            if (search.isEmpty()) return true;

            String name = debtor.getFullName() != null ? debtor.getFullName().toLowerCase() : "";
            String idNum = debtor.getIdNumber() != null ? debtor.getIdNumber().toLowerCase() : "";
            String phone = debtor.getPhone() != null ? debtor.getPhone().toLowerCase() : "";

            return name.contains(search) || idNum.contains(search) || phone.contains(search);
        });
    }

    private void setupTableInteraction() {
        tableView.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                Debtor selected = tableView.getSelectionModel().getSelectedItem();
                // TODO: Open edit modal
                logger.info("Double clicked debtor: {}", selected.getFullName());
            }
        });
    }

    private void loadDataAsync() {
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        
        ConcurrencyManager.getInstance().runAsync(
            () -> debtorRepo.findAll(),
            debtors -> {
                masterList = FXCollections.observableArrayList(debtors);
                filteredList = new FilteredList<>(masterList, p -> true);
                SortedList<Debtor> sortedList = new SortedList<>(filteredList);
                sortedList.comparatorProperty().bind(tableView.comparatorProperty());
                tableView.setItems(sortedList);
                
                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);
            },
            err -> {
                logger.error("Failed to load debtors", err);
                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);
            }
        );
    }

    @FXML
    private void handleNewDebtor() {
        Stage owner = (Stage) tableView.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = org.marrok.amriirad.util.GeneralUtil.openModal(owner, "/org/marrok/amriirad/view/debtor-form-view.fxml", "إضافة مدين جديد");
        if (loader != null) {
            DebtorFormController controller = loader.getController();
            controller.initForCreate(() -> loadDataAsync());
        }
    }

    @FXML
    private void handleBack() {
        org.marrok.amriirad.util.GeneralUtil.loadScene(
            (Stage) tableView.getScene().getWindow(),
            "/org/marrok/amriirad/view/dashboard-view.fxml"
        );
    }
}
