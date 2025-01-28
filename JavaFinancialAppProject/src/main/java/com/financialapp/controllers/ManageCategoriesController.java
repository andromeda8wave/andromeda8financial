package com.financialapp.controllers;

import com.financialapp.database.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Manage Categories Dialog.
 * Allows users to create, rename, and delete categories and their subcategories.
 */
public class ManageCategoriesController {

    // --- FXML Components ---

    @FXML
    private TableView<CategoryRow> categoriesTable;
    @FXML
    private TableColumn<CategoryRow, String> catNameCol;
    @FXML
    private TableColumn<CategoryRow, String> catTypeCol;

    @FXML
    private TableView<SubcategoryRow> subcategoriesTable;
    @FXML
    private TableColumn<SubcategoryRow, String> subcatNameCol;

    @FXML
    private TextField catNameField;
    @FXML
    private ComboBox<String> catTypeBox;

    @FXML
    private Button addCategoryBtn;
    @FXML
    private Button renameCategoryBtn;
    @FXML
    private Button deleteCategoryBtn;

    @FXML
    private TextField subcatNameField;
    @FXML
    private Button addSubcatBtn;
    @FXML
    private Button renameSubcatBtn;
    @FXML
    private Button deleteSubcatBtn;

    // --- Initialization ---

    @FXML
    public void initialize() {
        // Initialize ComboBox for Category Types
        catTypeBox.setItems(FXCollections.observableArrayList("INCOME", "EXPENSE"));

        // Set up Categories Table Columns
        catNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        catTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));

        // Set up Subcategories Table Columns
        subcatNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        // Load Categories from Database
        loadCategories();

        // Add listener to Categories Table to load corresponding Subcategories
        categoriesTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadSubcategories(newValue.getId());
                    } else {
                        subcategoriesTable.getItems().clear();
                    }
                }
        );

        // Set up Button Actions
        addCategoryBtn.setOnAction(e -> addCategory());
        renameCategoryBtn.setOnAction(e -> renameCategory());
        deleteCategoryBtn.setOnAction(e -> deleteCategory());

        addSubcatBtn.setOnAction(e -> addSubcategory());
        renameSubcatBtn.setOnAction(e -> renameSubcategory());
        deleteSubcatBtn.setOnAction(e -> deleteSubcategory());

        // Disable Rename and Delete buttons when no selection
        categoriesTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean disable = newValue == null;
                    renameCategoryBtn.setDisable(disable);
                    deleteCategoryBtn.setDisable(disable);
                }
        );

        subcategoriesTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean disable = newValue == null;
                    renameSubcatBtn.setDisable(disable);
                    deleteSubcatBtn.setDisable(disable);
                }
        );

        // Initially disable Rename and Delete buttons
        renameCategoryBtn.setDisable(true);
        deleteCategoryBtn.setDisable(true);
        renameSubcatBtn.setDisable(true);
        deleteSubcatBtn.setDisable(true);
    }

    // --- CRUD Operations for Categories ---

    /**
     * Loads all categories from the database and populates the Categories TableView.
     */
    private void loadCategories() {
        categoriesTable.getItems().clear();
        String sql = "SELECT * FROM categories ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<CategoryRow> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(new CategoryRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("type")
                ));
            }
            categoriesTable.getItems().addAll(categories);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load categories.");
        }
    }

    /**
     * Adds a new category to the database.
     */
    private void addCategory() {
        String name = catNameField.getText().trim();
        String type = catTypeBox.getValue();

        if (name.isEmpty() || type == null || type.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter both Category Name and Type.");
            return;
        }

        String sql = "INSERT INTO categories (name, type) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, type);
            ps.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Category added successfully.");

            // Refresh Categories Table
            loadCategories();

            // Clear input fields
            catNameField.clear();
            catTypeBox.setValue(null);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add category. It might already exist.");
        }
    }

    /**
     * Renames the selected category.
     */
    private void renameCategory() {
        CategoryRow selectedCategory = categoriesTable.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a category to rename.");
            return;
        }

        String newName = catNameField.getText().trim();
        String newType = catTypeBox.getValue();

        if (newName.isEmpty() || newType == null || newType.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter both new Category Name and Type.");
            return;
        }

        String sql = "UPDATE categories SET name = ?, type = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setString(2, newType);
            ps.setLong(3, selectedCategory.getId());
            ps.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Category renamed successfully.");

            // Refresh Categories Table
            loadCategories();

            // Clear input fields
            catNameField.clear();
            catTypeBox.setValue(null);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to rename category. It might already exist.");
        }
    }

    /**
     * Deletes the selected category from the database.
     * Prevents deletion if the category has associated subcategories.
     */
    private void deleteCategory() {
        CategoryRow selectedCategory = categoriesTable.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a category to delete.");
            return;
        }

        // Check if category has subcategories
        String checkSql = "SELECT COUNT(*) AS count FROM subcategories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {

            ps.setLong(1, selectedCategory.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt("count") > 0) {
                showAlert(Alert.AlertType.WARNING, "Cannot Delete", "Category has subcategories. Delete them first.");
                return;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to verify category dependencies.");
            return;
        }

        // Confirm Deletion
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to delete the selected category?");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String deleteSql = "DELETE FROM categories WHERE id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(deleteSql)) {

                    ps.setLong(1, selectedCategory.getId());
                    ps.executeUpdate();

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Category deleted successfully.");

                    // Refresh Categories Table
                    loadCategories();

                    // Clear Subcategories Table
                    subcategoriesTable.getItems().clear();

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete category.");
                }
            }
        });
    }

    // --- CRUD Operations for Subcategories ---

    /**
     * Loads all subcategories for the given category ID and populates the Subcategories TableView.
     *
     * @param categoryId The ID of the selected category.
     */
    private void loadSubcategories(long categoryId) {
        subcategoriesTable.getItems().clear();
        String sql = "SELECT * FROM subcategories WHERE category_id = ? ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, categoryId);
            ResultSet rs = ps.executeQuery();

            List<SubcategoryRow> subcategories = new ArrayList<>();
            while (rs.next()) {
                subcategories.add(new SubcategoryRow(
                        rs.getLong("id"),
                        rs.getLong("category_id"),
                        rs.getString("name")
                ));
            }
            subcategoriesTable.getItems().addAll(subcategories);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load subcategories.");
        }
    }

    /**
     * Adds a new subcategory under the selected category.
     */
    private void addSubcategory() {
        CategoryRow selectedCategory = categoriesTable.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            showAlert(Alert.AlertType.WARNING, "No Category Selected", "Please select a category to add a subcategory.");
            return;
        }

        String subcatName = subcatNameField.getText().trim();
        if (subcatName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a Subcategory Name.");
            return;
        }

        String sql = "INSERT INTO subcategories (category_id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, selectedCategory.getId());
            ps.setString(2, subcatName);
            ps.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Subcategory added successfully.");

            // Refresh Subcategories Table
            loadSubcategories(selectedCategory.getId());

            // Clear input field
            subcatNameField.clear();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add subcategory. It might already exist.");
        }
    }

    /**
     * Renames the selected subcategory.
     */
    private void renameSubcategory() {
        SubcategoryRow selectedSubcat = subcategoriesTable.getSelectionModel().getSelectedItem();
        if (selectedSubcat == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a subcategory to rename.");
            return;
        }

        String newName = subcatNameField.getText().trim();
        if (newName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a new Subcategory Name.");
            return;
        }

        String sql = "UPDATE subcategories SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setLong(2, selectedSubcat.getId());
            ps.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Subcategory renamed successfully.");

            // Refresh Subcategories Table
            loadSubcategories(selectedSubcat.getCategoryId());

            // Clear input field
            subcatNameField.clear();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to rename subcategory. It might already exist.");
        }
    }

    /**
     * Deletes the selected subcategory from the database.
     */
    private void deleteSubcategory() {
        SubcategoryRow selectedSubcat = subcategoriesTable.getSelectionModel().getSelectedItem();
        if (selectedSubcat == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a subcategory to delete.");
            return;
        }

        // Confirm Deletion
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to delete the selected subcategory?");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String sql = "DELETE FROM subcategories WHERE id = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setLong(1, selectedSubcat.getId());
                    ps.executeUpdate();

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Subcategory deleted successfully.");

                    // Refresh Subcategories Table
                    loadSubcategories(selectedSubcat.getCategoryId());

                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete subcategory.");
                }
            }
        });
    }

    // --- Helper Methods ---

    /**
     * Displays an alert dialog.
     *
     * @param type    The type of alert.
     * @param title   The title of the alert.
     * @param message The content message.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // --- Inner Classes for Table Rows ---

    /**
     * Represents a Category row in the Categories TableView.
     */
    public static class CategoryRow {
        private long id;
        private String name;
        private String type;

        public CategoryRow(long id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * Represents a Subcategory row in the Subcategories TableView.
     */
    public static class SubcategoryRow {
        private long id;
        private long categoryId;
        private String name;

        public SubcategoryRow(long id, long categoryId, String name) {
            this.id = id;
            this.categoryId = categoryId;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public long getCategoryId() {
            return categoryId;
        }

        public String getName() {
            return name;
        }
    }
}