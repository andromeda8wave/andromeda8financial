package com.financialapp.controllers;

import com.financialapp.database.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.*;

/**
 * Controller for EditPlanDialog.fxml
 * Allows user to set a new plan value for the given category & month/year.
 */
public class EditPlanController {

    @FXML
    private Label categoryLabel;
    @FXML
    private Label currentPlanLabel;
    @FXML
    private TextField newPlanField;
    @FXML
    private Button saveButton;

    private String categoryName;
    private double currentPlan;
    private int selectedMonth;
    private int selectedYear;
    private FirstScreenController parentController; // so we can refresh

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> savePlan());
    }

    /**
     * Called from FirstScreenController to set the category name and current plan.
     */
    public void setCategoryName(String name) {
        this.categoryName = name;
        categoryLabel.setText(name);
    }

    public void setCurrentPlan(double plan) {
        this.currentPlan = plan;
        currentPlanLabel.setText(String.valueOf(plan));
    }

    public void setSelectedMonth(int month) {
        this.selectedMonth = month;
    }

    public void setSelectedYear(int year) {
        this.selectedYear = year;
    }

    public void setParentController(FirstScreenController controller) {
        this.parentController = controller;
    }

    /**
     * Attempt to save a new plan for (categoryName, selectedMonth, selectedYear).
     */
    private void savePlan() {
        double newPlan;
        try {
            newPlan = Double.parseDouble(newPlanField.getText());
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a numeric plan value.");
            return;
        }

        // Find category_id
        long catId = getCategoryIdByName(categoryName);
        if (catId == -1) {
            showAlert("Error", "Could not find category in DB: " + categoryName);
            return;
        }

        // Check if plan row exists
        long planId = findExistingPlanId(catId, selectedMonth, selectedYear);

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (planId == -1) {
                // Insert
                String insertSql = "INSERT INTO plans (category_id, plan_amount, plan_month, plan_year) " +
                                   "VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setLong(1, catId);
                    ps.setDouble(2, newPlan);
                    ps.setInt(3, selectedMonth);
                    ps.setInt(4, selectedYear);
                    ps.executeUpdate();
                }
            } else {
                // Update
                String updateSql = "UPDATE plans SET plan_amount=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setDouble(1, newPlan);
                    ps.setLong(2, planId);
                    ps.executeUpdate();
                }
            }

            showAlert("Success", "Plan updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB Error", "Could not save plan.");
            return;
        }

        // Close dialog
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private long getCategoryIdByName(String catName) {
        if (catName == null) return -1;
        String sql = "SELECT id FROM categories WHERE name=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, catName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private long findExistingPlanId(long catId, int month, int year) {
        String sql = "SELECT id FROM plans WHERE category_id=? AND plan_month=? AND plan_year=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, catId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}