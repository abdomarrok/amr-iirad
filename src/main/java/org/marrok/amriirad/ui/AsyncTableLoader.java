package org.marrok.amriirad.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Utility to handle the common pattern of:
 * 1. Showing a loading indicator
 * 2. Fetching data asynchronously
 * 3. Creating ObservableList -> FilteredList -> SortedList
 * 4. Binding to TableView
 */
public class AsyncTableLoader<T> {

    private static final Logger logger = LogManager.getLogger(AsyncTableLoader.class);

    private final ConcurrencyManager concurrencyManager;
    private final TableView<T> tableView;
    private final ProgressIndicator loadingIndicator;

    private ObservableList<T> masterList;
    private FilteredList<T> filteredList;

    public AsyncTableLoader(ConcurrencyManager concurrencyManager, TableView<T> tableView, ProgressIndicator loadingIndicator) {
        this.concurrencyManager = concurrencyManager;
        this.tableView = tableView;
        this.loadingIndicator = loadingIndicator;
    }

    /**
     * Loads data using the fetcher and populates the table.
     * 
     * @param fetcher The background task to fetch data
     * @param onSuccess Optional callback after table is populated
     */
    public void load(Callable<List<T>> fetcher, Consumer<List<T>> onSuccess) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
            loadingIndicator.setManaged(true);
        }

        concurrencyManager.runAsync(
            fetcher,
            items -> {
                if (masterList == null) {
                    masterList = FXCollections.observableArrayList(items);
                    filteredList = new FilteredList<>(masterList, p -> true);
                    SortedList<T> sortedList = new SortedList<>(filteredList);
                    
                    // Bind sorting
                    sortedList.comparatorProperty().bind(tableView.comparatorProperty());
                    tableView.setItems(sortedList);
                } else {
                    // Updating existing list preserves FilteredList predicate and tableView binding
                    masterList.setAll(items);
                    tableView.refresh();
                }

                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                }

                if (onSuccess != null) {
                    onSuccess.accept(items);
                }
            },
            err -> {
                logger.error("Failed to load table data", err);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                }
            }
        );
    }

    public void load(Callable<List<T>> fetcher) {
        load(fetcher, null);
    }

    public ObservableList<T> getMasterList() {
        return masterList;
    }

    public FilteredList<T> getFilteredList() {
        return filteredList;
    }
}
