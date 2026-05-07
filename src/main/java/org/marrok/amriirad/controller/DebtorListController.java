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
import org.marrok.amriirad.ui.AsyncTableLoader;
import org.marrok.amriirad.util.SceneManager;

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

    private AsyncTableLoader<Debtor> tableLoader;

    private final DebtorRepository debtorRepo;
    private final ConcurrencyManager concurrencyManager;

    public DebtorListController(DebtorRepository debtorRepo, ConcurrencyManager concurrencyManager) {
        this.debtorRepo = debtorRepo;
        this.concurrencyManager = concurrencyManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tableLoader = new AsyncTableLoader<>(concurrencyManager, tableView, loadingIndicator);
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
            return new SimpleStringProperty(type != null ? type.getArabicLabel() : "-");
        });
    }

    private void setupFilters() {
        typeFilterCombo.getItems().addAll(DebtorType.values());
        typeFilterCombo.getItems().add(0, null); // "All"

        // Display Arabic labels in the filter combo
        typeFilterCombo.setConverter(new javafx.util.StringConverter<DebtorType>() {
            @Override
            public String toString(DebtorType type) {
                return type != null ? type.getArabicLabel() : "الكل";
            }

            @Override
            public DebtorType fromString(String string) {
                return null;
            }
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> updatePredicate());
        typeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    private void updatePredicate() {
        FilteredList<Debtor> filteredList = tableLoader.getFilteredList();
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
        tableLoader.load(() -> debtorRepo.findAll());
    }

    @FXML
    private void handleNewDebtor() {
        Stage owner = (Stage) tableView.getScene().getWindow();
        FXMLLoader loader = SceneManager.openModal(owner, "/org/marrok/amriirad/view/debtor-form-view.fxml", "إضافة مدين جديد");
        if (loader != null) {
            DebtorFormController controller = loader.getController();
            controller.initForCreate(() -> loadDataAsync());
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.loadScene(
            (Stage) tableView.getScene().getWindow(),
            "/org/marrok/amriirad/view/dashboard-view.fxml"
        );
    }
}
