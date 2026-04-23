package com.spellbound.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class HomeWindow {

    @FXML private TilePane recentFilesContainer; 
    @FXML private VBox sidebar;                  
    @FXML private Label userEmailLabel; 
    @FXML private Button accountCircle;
    @FXML private TextField searchField; 
    
    private List<File> allRecentFiles = new ArrayList<>();
    private static final String RECENT_FILES_PATH = "recent_files.txt";

    /**
     * static show method to transition from Login
     */
    public static void show(Stage oldStage, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(HomeWindow.class.getResource("/com/spellbound/ui/HomeView.fxml"));
            Parent root = loader.load();

            HomeWindow controller = loader.getController();
            controller.updateAccountUI(email);

            Stage stage = new Stage();
            Scene scene = new Scene(root);
            
            String css = "/com/spellbound/ui/styles.css";
            if (HomeWindow.class.getResource(css) != null) {
                scene.getStylesheets().add(HomeWindow.class.getResource(css).toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("SpellBound Hub");
            stage.centerOnScreen();
            stage.show();

            if (oldStage != null) oldStage.close();
            
        } catch (Exception e) {
            System.err.println("Failed to load HomeView: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        // 1. Inject the Logo into your purple sidebar
        if (sidebar != null) {
            MagicLogo logo = new MagicLogo(80);
            sidebar.getChildren().add(0, logo);
            VBox.setMargin(logo, new Insets(20, 0, 20, 0));
        }

        // 2. Real-time Search Listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        }

        loadRecentFiles();
    }

    // --- FXML EVENT HANDLERS ---

    @FXML 
    private void handleNew() {
        // Creates a fresh blank document in the workplace
        new WorkplaceWindow().show(new Stage(), null); 
    }

    @FXML 
    private void handleBrowseFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Document");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported Files", "*.txt", "*.pdf"));
        
        File selected = fc.showOpenDialog(sidebar.getScene().getWindow());
        if (selected != null) launchWorkplace(selected);
    }

    @FXML 
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        List<File> filtered = allRecentFiles.stream()
                .filter(f -> f.getName().toLowerCase().contains(query))
                .collect(Collectors.toList());
        displayFiles(filtered);
    }

    @FXML 
    private void handleAccountClick() {
        ContextMenu menu = new ContextMenu();
        MenuItem signOut = new MenuItem("Sign Out");
        signOut.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        signOut.setOnAction(e -> handleSignOut());
        
        menu.getItems().add(signOut);
        menu.show(accountCircle, Side.BOTTOM, 0, 5);
    }

    @FXML
    private void handleSignOut() {
        Stage stage = (Stage) sidebar.getScene().getWindow();
        new LoginWindow().show(stage);
    }

    // --- HELPER LOGIC ---

    private void launchWorkplace(File file) {
        saveToRecent(file);
        new WorkplaceWindow().show(new Stage(), file);
    }

    private void displayFiles(List<File> filesToDisplay) {
        if (recentFilesContainer == null) return;
        recentFilesContainer.getChildren().clear();

        for (File doc : filesToDisplay) {
            VBox card = new VBox(10);
            card.setAlignment(Pos.CENTER);
            card.setPrefSize(180, 140); 
            card.getStyleClass().add("file-card");
            card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-cursor: hand; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

            Label icon = new Label(doc.getName().endsWith(".pdf") ? "📕" : "📝");
            icon.setStyle("-fx-font-size: 40px;");
            
            Label name = new Label(doc.getName());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            
            card.getChildren().addAll(icon, name);
            card.setOnMouseClicked(e -> launchWorkplace(doc));
            
            recentFilesContainer.getChildren().add(card);
        }
    }

    private void loadRecentFiles() {
        allRecentFiles.clear();
        File historyFile = new File(RECENT_FILES_PATH);
        if (!historyFile.exists()) return;

        try {
            List<String> paths = Files.readAllLines(historyFile.toPath());
            for (String p : paths) {
                File f = new File(p);
                if (f.exists()) allRecentFiles.add(f);
            }
            displayFiles(allRecentFiles);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void saveToRecent(File f) {
        try {
            List<String> paths = new ArrayList<>();
            File file = new File(RECENT_FILES_PATH);
            if (file.exists()) paths = Files.readAllLines(file.toPath());
            
            paths.remove(f.getAbsolutePath());
            paths.add(0, f.getAbsolutePath());
            
            if (paths.size() > 10) paths = paths.subList(0, 10);
            Files.write(file.toPath(), paths);
            loadRecentFiles();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updateAccountUI(String email) {
        if (userEmailLabel != null) userEmailLabel.setText(email);
        if (accountCircle != null && email != null && !email.isEmpty()) {
            accountCircle.setText(email.substring(0, 1).toUpperCase());
        }
    }
}