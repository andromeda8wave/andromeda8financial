package com.financialapp.controllers;

import com.financialapp.database.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ThirdScreenController (Annual Dashboard).
 * Fix: Let user see newly added wallets by providing a refresh mechanism.
 */
public class ThirdScreenController {

    @FXML
    private ComboBox<String> walletFilterBox;
    @FXML
    private DatePicker startFilterDate;
    @FXML
    private DatePicker endFilterDate;
    @FXML
    private Button applyDashboardFilterButton;
    @FXML
    private Button refreshWalletsButton; // A button to re-load wallets

    @FXML
    private PieChart annualExpensesPieChart;
    @FXML
    private LineChart<String, Number> monthlyDynamicsLineChart;
    @FXML
    private CategoryAxis monthAxis;
    @FXML
    private NumberAxis amountAxis;

    @FXML
    public void initialize() {
        loadWalletCombo();
        loadInitialDashboard();

        applyDashboardFilterButton.setOnAction(e -> applyFilter());

        // Refresh button => re-load wallets from DB
        if (refreshWalletsButton != null) {
            refreshWalletsButton.setOnAction(e -> {
                loadWalletCombo();
                // optionally re-apply filter
                applyFilter();
            });
        }
    }

    private void loadWalletCombo() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM wallets ORDER BY name")) {

            ResultSet rs = ps.executeQuery();
            ObservableList<String> walletList = FXCollections.observableArrayList();
            while (rs.next()) {
                walletList.add(rs.getString("name"));
            }
            walletFilterBox.setItems(walletList);

            if (walletList.isEmpty()) {
                // no wallets in DB
                System.out.println("No wallets found. Add them on the second screen (Manage Wallets).");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load wallets for Annual Dashboard.");
        }
    }

    private void loadInitialDashboard() {
        loadExpensePieChart(null, null, null);
        loadLineChartData(null, null, null);
    }

    private void applyFilter() {
        String wallet = walletFilterBox.getValue();
        LocalDate start = startFilterDate.getValue();
        LocalDate end = endFilterDate.getValue();
        loadExpensePieChart(wallet, start, end);
        loadLineChartData(wallet, start, end);
    }

    private void loadExpensePieChart(String walletName, LocalDate start, LocalDate end) {
        annualExpensesPieChart.getData().clear();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT c.name AS category, SUM(t.amount) AS total ")
          .append("FROM transactions t ")
          .append("JOIN categories c ON c.id = t.category_id ")
          .append("JOIN wallets w ON w.id = t.wallet_id ")
          .append("WHERE c.type='EXPENSE' ");

        List<Object> params = new ArrayList<>();

        if (walletName != null && !walletName.isEmpty()) {
            sb.append("AND w.name = ? ");
            params.add(walletName);
        }
        if (start != null) {
            sb.append("AND t.date >= ? ");
            params.add(java.sql.Date.valueOf(start));
        }
        if (end != null) {
            sb.append("AND t.date <= ? ");
            params.add(java.sql.Date.valueOf(end));
        }

        sb.append("GROUP BY c.name");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String catName = rs.getString("category");
                double total = rs.getDouble("total");
                annualExpensesPieChart.getData().add(new PieChart.Data(catName, total));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to load expense data.");
        }
    }

    private void loadLineChartData(String walletName, LocalDate start, LocalDate end) {
        monthlyDynamicsLineChart.getData().clear();
        monthAxis.setLabel("Month (YYYY-MM)");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Incomes");

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT EXTRACT(YEAR FROM t.date) AS yr, EXTRACT(MONTH FROM t.date) AS mon, ")
          .append(" SUM(CASE WHEN c.type='EXPENSE' THEN t.amount ELSE 0 END) AS total_expenses, ")
          .append(" SUM(CASE WHEN c.type='INCOME' THEN t.amount ELSE 0 END) AS total_incomes ")
          .append("FROM transactions t ")
          .append("JOIN categories c ON c.id = t.category_id ")
          .append("JOIN wallets w ON w.id = t.wallet_id ")
          .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (walletName != null && !walletName.isEmpty()) {
            sb.append("AND w.name = ? ");
            params.add(walletName);
        }
        if (start != null) {
            sb.append("AND t.date >= ? ");
            params.add(java.sql.Date.valueOf(start));
        }
        if (end != null) {
            sb.append("AND t.date <= ? ");
            params.add(java.sql.Date.valueOf(end));
        }

        sb.append("GROUP BY yr, mon ORDER BY yr, mon");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int y = rs.getInt("yr");
                int m = rs.getInt("mon");
                double exp = rs.getDouble("total_expenses");
                double inc = rs.getDouble("total_incomes");

                String label = String.format("%04d-%02d", y, m);
                expenseSeries.getData().add(new XYChart.Data<>(label, exp));
                incomeSeries.getData().add(new XYChart.Data<>(label, inc));
            }
            monthlyDynamicsLineChart.getData().addAll(expenseSeries, incomeSeries);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to load monthly data.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}