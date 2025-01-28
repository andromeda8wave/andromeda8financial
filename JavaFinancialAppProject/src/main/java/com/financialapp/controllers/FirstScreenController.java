package com.financialapp.controllers;

import com.financialapp.database.DatabaseConnection;
import com.financialapp.models.SummaryRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the first screen (two charts + Plan-Fact-Deviation table).
 * Now excludes INCOME categories and supports a real plan-edit dialog on double-click.
 */
public class FirstScreenController {

    @FXML
    private ComboBox<String> monthCombo;   // from your newly added combos
    @FXML
    private ComboBox<Integer> yearCombo;
    @FXML
    private Button applyPeriodButton;

    @FXML
    private PieChart expensesPieChart;
    @FXML
    private BarChart<String, Number> planActualBarChart;
    @FXML
    private CategoryAxis planActualCategoryAxis;
    @FXML
    private NumberAxis planActualNumberAxis;

    @FXML
    private TableView<SummaryRow> summaryTable;
    @FXML
    private TableColumn<SummaryRow, String> categoryColumn;
    @FXML
    private TableColumn<SummaryRow, Double> planColumn;
    @FXML
    private TableColumn<SummaryRow, Double> actualColumn;
    @FXML
    private TableColumn<SummaryRow, Double> deviationColumn;

    // We'll track the selected period:
    private Integer selectedYear;
    private Integer selectedMonth; // 1..12

    @FXML
    public void initialize() {
        // Table columns
        categoryColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCategory()));
        planColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getPlan()));
        actualColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getActual()));
        deviationColumn.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getDeviation()));

        // Populate combos with month/year
        initMonthCombo();
        initYearCombo();

        // Default to current date
        LocalDate now = LocalDate.now();
        selectedMonth = now.getMonthValue(); // e.g. 1..12
        selectedYear = now.getYear();
        monthCombo.setValue(getMonthName(selectedMonth));
        yearCombo.setValue(selectedYear);

        // "Apply Period" button => refresh
        applyPeriodButton.setOnAction(e -> {
            // read month/year from combos
            String mName = monthCombo.getValue();
            Integer yVal = yearCombo.getValue();
            if (mName == null || yVal == null) {
                // fallback
                selectedMonth = now.getMonthValue();
                selectedYear = now.getYear();
            } else {
                selectedMonth = monthNameToNumber(mName);
                selectedYear = yVal;
            }
            loadExpensesPieChart();
            loadPlanActualBarChart();
            loadSummaryTable();
        });

        // First load with defaults:
        loadExpensesPieChart();
        loadPlanActualBarChart();
        loadSummaryTable();

        // Double-click row => open plan-edit dialog
        summaryTable.setRowFactory(tv -> {
            TableRow<SummaryRow> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2 && !row.isEmpty()) {
                    SummaryRow item = row.getItem();
                    openEditPlanDialog(item.getCategory(), item.getPlan());
                }
            });
            return row;
        });
    }

    // -------------- Queries: exclude INCOME categories ---------------

    private void loadExpensesPieChart() {
        expensesPieChart.getData().clear();
        if (selectedMonth == null || selectedYear == null) {
            LocalDate now = LocalDate.now();
            selectedMonth = now.getMonthValue();
            selectedYear = now.getYear();
        }

        String sql = 
            "SELECT c.name AS category, SUM(t.amount) AS total " +
            "FROM transactions t " +
            "JOIN categories c ON t.category_id = c.id " +
            "WHERE c.type='EXPENSE' " + // Exclude INCOME
            "  AND EXTRACT(MONTH FROM t.date)=? " +
            "  AND EXTRACT(YEAR FROM t.date)=? " +
            "GROUP BY c.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedMonth);
            ps.setInt(2, selectedYear);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String cat = rs.getString("category");
                double total = rs.getDouble("total");
                expensesPieChart.getData().add(new PieChart.Data(cat, total));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPlanActualBarChart() {
        planActualBarChart.getData().clear();
        planActualCategoryAxis.setLabel("Category");
        planActualNumberAxis.setLabel("Amount");

        if (selectedMonth == null || selectedYear == null) {
            LocalDate now = LocalDate.now();
            selectedMonth = now.getMonthValue();
            selectedYear = now.getYear();
        }

        XYChart.Series<String, Number> planSeries = new XYChart.Series<>();
        planSeries.setName("Plan");

        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual");

        String sql = 
            "SELECT c.id AS cat_id, c.name AS cat_name, " +
            "       COALESCE(p.plan_amount, 0) AS plan_amount, " +
            "       (SELECT COALESCE(SUM(t.amount),0) FROM transactions t " +
            "        WHERE t.category_id = c.id " +
            "          AND EXTRACT(MONTH FROM t.date)=? " +
            "          AND EXTRACT(YEAR FROM t.date)=?) AS actual_amount " +
            "FROM categories c " +
            "LEFT JOIN plans p ON p.category_id = c.id AND p.plan_month=? AND p.plan_year=? " +
            "WHERE c.type='EXPENSE' " + // Exclude INCOME
            "ORDER BY c.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, selectedMonth);
            ps.setInt(2, selectedYear);
            ps.setInt(3, selectedMonth);
            ps.setInt(4, selectedYear);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String catName = rs.getString("cat_name");
                double plan = rs.getDouble("plan_amount");
                double actual = rs.getDouble("actual_amount");

                planSeries.getData().add(new XYChart.Data<>(catName, plan));
                actualSeries.getData().add(new XYChart.Data<>(catName, actual));
            }
            planActualBarChart.getData().addAll(planSeries, actualSeries);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSummaryTable() {
        summaryTable.getItems().clear();
        if (selectedMonth == null || selectedYear == null) {
            LocalDate now = LocalDate.now();
            selectedMonth = now.getMonthValue();
            selectedYear = now.getYear();
        }

        List<SummaryRow> rows = new ArrayList<>();
        double totalPlan = 0;
        double totalActual = 0;

        String sql = 
            "SELECT c.id AS cat_id, c.name AS category, " +
            "       COALESCE(p.plan_amount, 0) AS plan_amount, " +
            "       (SELECT COALESCE(SUM(t.amount),0) FROM transactions t " +
            "        WHERE t.category_id = c.id " +
            "          AND EXTRACT(MONTH FROM t.date)=? " +
            "          AND EXTRACT(YEAR FROM t.date)=?) as actual_amount " +
            "FROM categories c " +
            "LEFT JOIN plans p ON p.category_id = c.id AND p.plan_month=? AND p.plan_year=? " +
            "WHERE c.type='EXPENSE' " + // Exclude INCOME
            "ORDER BY c.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, selectedMonth);
            ps.setInt(2, selectedYear);
            ps.setInt(3, selectedMonth);
            ps.setInt(4, selectedYear);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String catName = rs.getString("category");
                double planVal = rs.getDouble("plan_amount");
                double actualVal = rs.getDouble("actual_amount");

                totalPlan += planVal;
                totalActual += actualVal;

                rows.add(new SummaryRow(catName, planVal, actualVal));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        summaryTable.getItems().addAll(rows);

        // Totals row
        double devTotal = totalPlan - totalActual;
        SummaryRow totalRow = new SummaryRow("TOTAL", totalPlan, totalActual);
        totalRow.setDeviation(devTotal);
        summaryTable.getItems().add(totalRow);
    }

    // -------------- Double-click => open real plan-edit dialog --------------

    private void openEditPlanDialog(String categoryName, double currentPlan) {
        try {
            // Load the EditPlanDialog.fxml
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/EditPlanDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            // Get controller
            EditPlanController controller = loader.getController();
            // Set data for the dialog
            controller.setCategoryName(categoryName);
            controller.setCurrentPlan(currentPlan);
            controller.setSelectedMonth(selectedMonth);
            controller.setSelectedYear(selectedYear);
            // We'll pass 'this' so we can refresh after saving
            controller.setParentController(this);

            // Show dialog
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Edit Plan for " + categoryName);
            dialogStage.setScene(new javafx.scene.Scene(root));
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.showAndWait();

            // After dialog closes, refresh charts & table
            loadExpensesPieChart();
            loadPlanActualBarChart();
            loadSummaryTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------- Month/Year Combo Helpers --------------

    private void initMonthCombo() {
        ObservableList<String> months = FXCollections.observableArrayList(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        );
        monthCombo.setItems(months);
    }

    private void initYearCombo() {
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int thisYear = java.time.LocalDate.now().getYear();
        for (int y = thisYear - 5; y <= thisYear + 5; y++) {
            years.add(y);
        }
        yearCombo.setItems(years);
    }

    private String getMonthName(int m) {
        switch (m) {
            case 1:  return "January";
            case 2:  return "February";
            case 3:  return "March";
            case 4:  return "April";
            case 5:  return "May";
            case 6:  return "June";
            case 7:  return "July";
            case 8:  return "August";
            case 9:  return "September";
            case 10: return "October";
            case 11: return "November";
            case 12: return "December";
            default: return "";
        }
    }

    private int monthNameToNumber(String name) {
        switch (name) {
            case "January":   return 1;
            case "February":  return 2;
            case "March":     return 3;
            case "April":     return 4;
            case "May":       return 5;
            case "June":      return 6;
            case "July":      return 7;
            case "August":    return 8;
            case "September": return 9;
            case "October":   return 10;
            case "November":  return 11;
            case "December":  return 12;
            default:          return 0;
        }
    }

    // Called by EditPlanController after plan is updated (optional approach)
    public void refreshDataAfterPlanEdit() {
        loadExpensesPieChart();
        loadPlanActualBarChart();
        loadSummaryTable();
    }
}