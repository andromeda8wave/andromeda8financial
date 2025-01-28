package com.financialapp.controllers;

import com.financialapp.database.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ManageWalletsController {

    @FXML
    private TableView<WalletRow> walletsTable;
    @FXML
    private TableColumn<WalletRow, String> walletNameCol;

    @FXML
    private TextField walletNameField;
    @FXML
    private Button addWalletBtn;
    @FXML
    private Button deleteWalletBtn;

    @FXML
    public void initialize() {
        walletNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));

        loadWallets();

        addWalletBtn.setOnAction(e -> addWallet());
        deleteWalletBtn.setOnAction(e -> deleteWallet());
    }

    private void loadWallets() {
        walletsTable.getItems().clear();
        String sql = "SELECT * FROM wallets ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<WalletRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new WalletRow(
                    rs.getLong("id"),
                    rs.getString("name")
                ));
            }
            walletsTable.getItems().addAll(rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addWallet() {
        String name = walletNameField.getText();
        if (name == null || name.isEmpty()) {
            showAlert("Validation Error", "Enter wallet name");
            return;
        }

        String sql = "INSERT INTO wallets (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB Error", "Could not add wallet");
            return;
        }
        walletNameField.clear();
        loadWallets();
    }

    private void deleteWallet() {
        WalletRow selected = walletsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Select a wallet to delete");
            return;
        }
        String sql = "DELETE FROM wallets WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, selected.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB Error", "Could not delete wallet");
            return;
        }
        loadWallets();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static class WalletRow {
        private long id;
        private String name;

        public WalletRow(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() { return id; }
        public String getName() { return name; }
    }
}