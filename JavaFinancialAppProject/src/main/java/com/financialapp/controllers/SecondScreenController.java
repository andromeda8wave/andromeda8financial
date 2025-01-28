package com.financialapp.controllers;

import com.financialapp.database.DatabaseConnection;
import com.financialapp.models.Transaction;
import com.financialapp.util.CsvExporter;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.fxml.FXMLLoader;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SecondScreenController {

    @FXML
    private DatePicker dateField;
    @FXML
    private ComboBox<String> categoryBox;
    @FXML
    private ComboBox<String> subcategoryBox;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> currencyBox;
    @FXML
    private ComboBox<String> walletBox;
    @FXML
    private TextField commentField;

    @FXML
    private Button addTransactionButton;
    @FXML
    private Button manageCategoriesButton;
    @FXML
    private Button manageCurrenciesButton;
    @FXML
    private Button manageWalletsButton;
    @FXML
    private Button exportCsvButton;

    @FXML
    private TableView<Transaction> transactionsTable;
    @FXML
    private TableColumn<Transaction, String> dateCol;
    @FXML
    private TableColumn<Transaction, String> categoryCol;
    @FXML
    private TableColumn<Transaction, String> subcategoryCol;
    @FXML
    private TableColumn<Transaction, String> amountCol;
    @FXML
    private TableColumn<Transaction, String> currencyCol;
    @FXML
    private TableColumn<Transaction, String> walletCol;
    @FXML
    private TableColumn<Transaction, String> commentCol;

    @FXML
    private DatePicker startDateFilter;
    @FXML
    private DatePicker endDateFilter;
    @FXML
    private ComboBox<String> filterCategoryBox;
    @FXML
    private Button applyFilterButton;
    @FXML
    private Button clearFilterButton;

    @FXML
    public void initialize() {
        initComboBoxes();
        initTableColumns();
        loadTransactions(null, null, null);

        addTransactionButton.setOnAction(e -> addTransaction());
        exportCsvButton.setOnAction(e -> exportTransactionsCsv());
        applyFilterButton.setOnAction(e -> applyFilter());
        clearFilterButton.setOnAction(e -> clearFilters()); // <-- FIX

        manageCategoriesButton.setOnAction(e -> openManageCategoriesDialog());
        manageCurrenciesButton.setOnAction(e -> openManageCurrenciesDialog());
        manageWalletsButton.setOnAction(e -> openManageWalletsDialog());

        // Context menu: edit/delete
        transactionsTable.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem editItem = new MenuItem("Edit Transaction");
            editItem.setOnAction(evt -> editTransaction(row.getItem()));

            MenuItem deleteItem = new MenuItem("Delete Transaction");
            deleteItem.setOnAction(evt -> deleteTransaction(row.getItem()));

            contextMenu.getItems().addAll(editItem, deleteItem);

            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });
            return row;
        });
    }

    private void initComboBoxes() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Categories
            ResultSet rsCat = stmt.executeQuery("SELECT name FROM categories ORDER BY name");
            categoryBox.getItems().clear();
            filterCategoryBox.getItems().clear();
            while (rsCat.next()) {
                String name = rsCat.getString("name");
                categoryBox.getItems().add(name);
                filterCategoryBox.getItems().add(name);
            }

            // Subcategories (optional)
            ResultSet rsSub = stmt.executeQuery("SELECT name FROM subcategories ORDER BY name");
            subcategoryBox.getItems().clear();
            while (rsSub.next()) {
                subcategoryBox.getItems().add(rsSub.getString("name"));
            }

            // Currencies
            ResultSet rsCurr = stmt.executeQuery("SELECT code FROM currencies ORDER BY code");
            currencyBox.getItems().clear();
            while (rsCurr.next()) {
                currencyBox.getItems().add(rsCurr.getString("code"));
            }

            // Wallets
            ResultSet rsWall = stmt.executeQuery("SELECT name FROM wallets ORDER BY name");
            walletBox.getItems().clear();
            while (rsWall.next()) {
                walletBox.getItems().add(rsWall.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initTableColumns() {
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDate().toString()));
        categoryCol.setCellValueFactory(cd -> new SimpleStringProperty(getCategoryName(cd.getValue().getCategoryId())));
        subcategoryCol.setCellValueFactory(cd -> new SimpleStringProperty(getSubcategoryName(cd.getValue().getSubcategoryId())));
        amountCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getAmount())));
        currencyCol.setCellValueFactory(cd -> new SimpleStringProperty(getCurrencyCode(cd.getValue().getCurrencyId())));
        walletCol.setCellValueFactory(cd -> new SimpleStringProperty(getWalletName(cd.getValue().getWalletId())));
        commentCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getComment()));
    }

    private void loadTransactions(LocalDate start, LocalDate end, String categoryFilter) {
        transactionsTable.getItems().clear();
        transactionsTable.getItems().addAll(getTransactionsFromDb(start, end, categoryFilter));
    }

    private List<Transaction> getTransactionsFromDb(LocalDate start, LocalDate end, String categoryFilter) {
        List<Transaction> result = new ArrayList<>();

        StringBuilder sb = new StringBuilder("SELECT * FROM transactions WHERE 1=1 ");
        if (start != null) {
            sb.append("AND date >= '").append(Date.valueOf(start)).append("' ");
        }
        if (end != null) {
            sb.append("AND date <= '").append(Date.valueOf(end)).append("' ");
        }
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            long catId = getCategoryIdByName(categoryFilter);
            if (catId != -1) {
                sb.append("AND category_id = ").append(catId).append(" ");
            }
        }
        sb.append("ORDER BY date DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sb.toString())) {

            while (rs.next()) {
                Transaction t = new Transaction();
                t.setId(rs.getLong("id"));
                t.setDate(rs.getDate("date").toLocalDate());
                t.setCategoryId(rs.getLong("category_id"));
                t.setSubcategoryId(rs.getLong("subcategory_id"));
                t.setAmount(rs.getDouble("amount"));
                t.setCurrencyId(rs.getLong("currency_id"));
                t.setWalletId(rs.getLong("wallet_id"));
                t.setComment(rs.getString("comment"));
                result.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void addTransaction() {
        // Validate date
        if (dateField.getValue() == null) {
            showAlert("Validation Error", "Date cannot be empty.");
            return;
        }

        // Validate category
        String catName = categoryBox.getValue();
        if (catName == null || catName.isEmpty()) {
            showAlert("Validation Error", "Category cannot be empty.");
            return;
        }

        // Subcategory (optional)
        String subcatName = subcategoryBox.getValue();

        // Validate amount
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Invalid Amount.");
            return;
        }

        // Validate currency
        String currencyCode = currencyBox.getValue();
        if (currencyCode == null || currencyCode.isEmpty()) {
            showAlert("Validation Error", "Currency cannot be empty.");
            return;
        }

        // Validate wallet
        String walletName = walletBox.getValue();
        if (walletName == null || walletName.isEmpty()) {
            showAlert("Validation Error", "Wallet cannot be empty.");
            return;
        }

        // Comment optional
        String comment = commentField.getText();

        LocalDate date = dateField.getValue();
        long categoryId = getCategoryIdByName(catName);
        long subcategoryId = getSubcategoryIdByName(subcatName); // may be -1
        long currencyId = getCurrencyIdByCode(currencyCode);
        long walletId = getWalletIdByName(walletName);

        String sql = "INSERT INTO transactions (date, category_id, subcategory_id, amount, currency_id, wallet_id, comment) " +
                     "VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            ps.setLong(2, categoryId);

            // If no subcategory, set NULL
            if (subcategoryId == -1) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, subcategoryId);
            }

            ps.setDouble(4, amount);
            ps.setLong(5, currencyId);
            ps.setLong(6, walletId);
            ps.setString(7, comment);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB Error", "Could not add transaction.");
            return;
        }

        loadTransactions(null, null, null);

        // Clear form
        dateField.setValue(null);
        categoryBox.setValue(null);
        subcategoryBox.setValue(null);
        amountField.clear();
        currencyBox.setValue(null);
        walletBox.setValue(null);
        commentField.clear();
    }

    private void exportTransactionsCsv() {
        List<Transaction> transactions = new ArrayList<>(transactionsTable.getItems());
        CsvExporter.exportTransactions(transactions, "transactions_export.csv");
        showAlert("Export CSV", "Transactions exported to transactions_export.csv");
    }

    private void applyFilter() {
        LocalDate start = startDateFilter.getValue();
        LocalDate end = endDateFilter.getValue();
        String cat = filterCategoryBox.getValue();
        loadTransactions(start, end, cat);
    }

    /** 
     * Renamed to clearFilters() to match the button onAction reference. 
     */
    private void clearFilters() {
        startDateFilter.setValue(null);
        endDateFilter.setValue(null);
        filterCategoryBox.setValue(null);
        loadTransactions(null, null, null);
    }

    private void editTransaction(Transaction tx) {
        if (tx == null) return;
        showAlert("Edit Transaction", "Placeholder for editing transaction ID: " + tx.getId());
    }

    private void deleteTransaction(Transaction tx) {
        if (tx == null) return;
        String sql = "DELETE FROM transactions WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tx.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB Error", "Could not delete transaction.");
        }
        loadTransactions(null, null, null);
    }

    private void openManageCategoriesDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageCategoriesDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Manage Categories & Subcategories");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // refresh combos
            initComboBoxes();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open Manage Categories dialog.");
        }
    }

    private void openManageCurrenciesDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageCurrenciesDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Manage Currencies");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            initComboBoxes();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open Manage Currencies dialog.");
        }
    }

    private void openManageWalletsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageWalletsDialog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Manage Wallets");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            initComboBoxes();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open Manage Wallets dialog.");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Helper methods
    private long getCategoryIdByName(String name) {
        if (name == null || name.isEmpty()) return -1;
        String sql = "SELECT id FROM categories WHERE name=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private long getSubcategoryIdByName(String name) {
        if (name == null || name.isEmpty()) return -1;
        String sql = "SELECT id FROM subcategories WHERE name=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private long getCurrencyIdByCode(String code) {
        if (code == null || code.isEmpty()) return -1;
        String sql = "SELECT id FROM currencies WHERE code=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private long getWalletIdByName(String name) {
        if (name == null || name.isEmpty()) return -1;
        String sql = "SELECT id FROM wallets WHERE name=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String getCategoryName(long id) {
        if (id < 0) return "";
        String sql = "SELECT name FROM categories WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getSubcategoryName(long id) {
        if (id <= 0) return "";
        String sql = "SELECT name FROM subcategories WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getCurrencyCode(long id) {
        if (id <= 0) return "";
        String sql = "SELECT code FROM currencies WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("code");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getWalletName(long id) {
        if (id <= 0) return "";
        String sql = "SELECT name FROM wallets WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
}