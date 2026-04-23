package com.spellbound.ui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class LoginWindow {

    @FXML private TextField emailField;
    @FXML private Button loginButton;
    @FXML private VBox brandingBox; 

    @FXML
    public void initialize() {
        if (brandingBox != null) {
            MagicLogo logo = new MagicLogo(100); 
            brandingBox.getChildren().add(0, logo); 
            VBox.setMargin(logo, new Insets(0, 0, 30, 0));
        }
    }

    public void show(Stage stage) {
        try {
            if (stage == null) stage = new Stage();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/spellbound/ui/LoginView.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            String cssPath = "/com/spellbound/ui/styles.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            try {
                if (!stage.isShowing()) {
                    stage.initStyle(StageStyle.DECORATED); 
                }
            } catch (IllegalStateException e) { /* Ignore */ }
            
            stage.setScene(scene);
            stage.setTitle("SpellBound — Connect");
            stage.setResizable(false);
            stage.sizeToScene(); 
            stage.centerOnScreen();
            
            root.setOpacity(0);
            stage.show();
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(800), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) { 
            System.err.println("Critical Error: Login window failed to initialize.");
            e.printStackTrace(); 
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();

        // Basic Validation
        if (email.isEmpty()) {
            emailField.setPromptText("Please enter your email");
            emailField.setStyle("-fx-border-color: #f98d72; -fx-border-width: 2px; -fx-background-radius: 5; -fx-border-radius: 5;");
            return;
        }

        Stage currentStage = (Stage) emailField.getScene().getWindow();
        
        // Fade out before switching
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), emailField.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            HomeWindow.show(currentStage, email);
        });
        fadeOut.play();
    }
}