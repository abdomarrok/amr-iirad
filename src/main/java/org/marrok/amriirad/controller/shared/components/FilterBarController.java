package org.marrok.amriirad.controller.shared.components;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Reusable controller for a standardized filter and search bar.
 */
public class FilterBarController {

    @FXML private TextField searchField;
    @FXML private HBox customFiltersBox;

    public TextField getSearchField() {
        return searchField;
    }

    public void addFilter(Node node) {
        customFiltersBox.getChildren().add(node);
    }

    public void clearCustomFilters() {
        customFiltersBox.getChildren().clear();
    }
    
    public void setSearchPrompt(String prompt) {
        searchField.setPromptText(prompt);
    }
}
