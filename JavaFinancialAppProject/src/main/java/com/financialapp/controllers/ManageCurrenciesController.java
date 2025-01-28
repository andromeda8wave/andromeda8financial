package com.financialapp.controllers;

import com.financialapp.database.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ManageCurrenciesController {

    @FXML
    private TableView<CurrencyRow> currenciesTable;
    @FXML
    private TableColumn<CurrencyRow, String> currencyCodeCol;
    @FXML
    private TableColumn<CurrencyRow, String> currencyNameCol;

    @FXML
    private TextField currencyCodeField;
    @FXML
    private TextField currencyNameField;

    @FXML
    private Button addCurrencyBtn;
    @FXML
    private Button deleteCurrencyBtn;

    @FXML
    public void initialize() {
        currencyCodeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCode()));
        currencyNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));

        loadCurrencies();

        addCurrencyBtn.setOnAction(e -> addCurrency());
        deleteCurrencyBtn.setOnAction(e -> deleteCurrency());
    }

    private void loadCurrencies() {
        currenciesTable.getItems().clear();
        String sql = "SELECT * FROM currencies ORDER BY code";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<CurrencyRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new CurrencyRow(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("name")
                ));
            }
            currenciesTable.getItems().addAll(rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addCurrency() {
        String code = currencyCodeField.getText();
        String name = currencyNameField.getText();
        if (code == null || code.isEmpty() || name == null || name.isEmpty()) {
            showAlert("Validation Error", "Enter both code and name");
            return;
        }

        String sql = "INSERT INTO currencies (code, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB Error", "Could not add currency");
            return;
        }
        currencyCodeField.clear();
        currencyNameField.clear();
        loadCurrencies();
    }

    private void deleteCurrency() {
        CurrencyRow selected = currenciesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Select a currency to delete");
            return;
        }
        String sql = "DELETE FROM currencies WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, selected.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB Error", "Could not delete currency");
            return;
        }
        loadCurrencies();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static class CurrencyRow {
        private long id;
        private String code;
        private String name;

        public CurrencyRow(long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        public long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
    }
}