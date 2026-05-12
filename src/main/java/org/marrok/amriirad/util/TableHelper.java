package org.marrok.amriirad.util;

import javafx.scene.control.*;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Utility for standardizing TableView behaviors (context menus, double-click, etc.).
 */
public class TableHelper {

    public static <T> void setupActionContextMenu(TableView<T> table, Runnable onEdit, Runnable onDelete) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("تعديل");
        editItem.setGraphic(new FontIcon("fas-edit"));
        editItem.setOnAction(e -> {
            if (table.getSelectionModel().getSelectedItem() != null) {
                onEdit.run();
            }
        });
        
        MenuItem deleteItem = new MenuItem("حذف");
        deleteItem.setGraphic(new FontIcon("fas-trash"));
        deleteItem.setStyle("-fx-text-fill: #e74c3c;"); // red-500
        deleteItem.setOnAction(e -> {
            if (table.getSelectionModel().getSelectedItem() != null) {
                onDelete.run();
            }
        });
        
        contextMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
        
        // Disable items if no selection
        contextMenu.setOnShowing(e -> {
            boolean hasSelection = table.getSelectionModel().getSelectedItem() != null;
            editItem.setDisable(!hasSelection);
            deleteItem.setDisable(!hasSelection);
        });

        table.setContextMenu(contextMenu);
        
        // Double-click to edit pattern
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    onEdit.run();
                }
            });
            return row;
        });
    }
}
