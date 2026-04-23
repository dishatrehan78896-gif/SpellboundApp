package com.spellbound.app;

import com.spellbound.ui.LoginWindow;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the SpellBound Application.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Initialize & Launch Login Window
            LoginWindow loginWindow = new LoginWindow();
            loginWindow.show(primaryStage);
            
            System.out.println("✨ SpellBound Engine initialized. Magic is ready.");
            
        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE: SpellBound could not initialize.");
            e.printStackTrace();
        }
    }

    /**
     * Standard Main Method
     */
    public static void main(String[] args) {
        // --- LAPTOP DISPLAY FIXES ---
        // These ensure your PowerPoint-style UI doesn't look blurry or "cut off"
        System.setProperty("glass.gtk.uiScale", "1.0"); 
        System.setProperty("prism.allowhidpi", "true"); 
        
        // This starts the JavaFX lifecycle
        launch(args);
    }
}