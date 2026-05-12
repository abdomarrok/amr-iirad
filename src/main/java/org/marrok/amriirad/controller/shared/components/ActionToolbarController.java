package org.marrok.amriirad.controller.shared.components;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Reusable controller for a standardized action toolbar.
 */
public class ActionToolbarController {

    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button refreshBtn;
    @FXML private Button printBtn;
    @FXML private Button exportBtn;

    private Runnable onAddAction;
    private Runnable onEditAction;
    private Runnable onDeleteAction;
    private Runnable onRefreshAction;
    private Runnable onPrintAction;
    private Runnable onExportAction;

    public void init(Runnable add, Runnable edit, Runnable delete, Runnable refresh, Runnable print, Runnable export) {
        this.onAddAction = add;
        this.onEditAction = edit;
        this.onDeleteAction = delete;
        this.onRefreshAction = refresh;
        this.onPrintAction = print;
        this.onExportAction = export;
        
        updateVisibility();
    }

    public void setAddVisible(boolean visible) {
        setVisible(addBtn, visible);
    }

    public void setEditVisible(boolean visible) {
        setVisible(editBtn, visible);
    }

    public void setDeleteVisible(boolean visible) {
        setVisible(deleteBtn, visible);
    }

    public void setPrintVisible(boolean visible) {
        setVisible(printBtn, visible);
    }

    private void updateVisibility() {
        setVisible(addBtn, onAddAction != null);
        setVisible(editBtn, onEditAction != null);
        setVisible(deleteBtn, onDeleteAction != null);
        setVisible(refreshBtn, onRefreshAction != null);
        setVisible(printBtn, onPrintAction != null);
        setVisible(exportBtn, onExportAction != null);
    }

    private void setVisible(Button btn, boolean visible) {
        btn.setVisible(visible);
        btn.setManaged(visible);
    }

    public void setAddText(String text) {
        addBtn.setText(text);
    }
    
    public void setEditText(String text) {
        editBtn.setText(text);
    }

    public void setDeleteText(String text) {
        deleteBtn.setText(text);
    }

    @FXML
    private void onAdd() {
        if (onAddAction != null) onAddAction.run();
    }

    @FXML
    private void onEdit() {
        if (onEditAction != null) onEditAction.run();
    }

    @FXML
    private void onDelete() {
        if (onDeleteAction != null) onDeleteAction.run();
    }

    @FXML
    private void onRefresh() {
        if (onRefreshAction != null) onRefreshAction.run();
    }

    @FXML
    private void onPrint() {
        if (onPrintAction != null) onPrintAction.run();
    }

    @FXML
    private void onExport() {
        if (onExportAction != null) onExportAction.run();
    }
}
