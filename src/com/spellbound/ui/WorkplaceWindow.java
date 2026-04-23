package com.spellbound.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import com.spellbound.logic.CorrectionEngine;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class WorkplaceWindow {

    @FXML private TextArea editor;
    @FXML private VBox editorContainer;
    @FXML private Label fileNameLabel;
    @FXML private Label wordCountLabel; 
    @FXML private Label charCountLabel;
    
    @FXML private ScrollPane pdfScrollPane;
    @FXML private StackPane contentStack;

    // --- NEW DESIGN ARCHITECT CONTROLS ---
    @FXML private ComboBox<String> fontPicker;
    @FXML private ComboBox<String> marginPicker;
    @FXML private TextField headingField;
    @FXML private CheckBox pageNumbersToggle;
    @FXML private CheckBox tableMagicToggle;

    private CorrectionEngine engine = new CorrectionEngine();
    private File currentFile;

    /**
     * Helper class to transport user design choices
     */
    public static class DocSettings {
        public float margin = 72f;
        public String font = "Helvetica";
        public boolean showPageNumbers = true;
        public String[] highlights = new String[0];
        public boolean convertToTables = false;
    }

    @FXML
    public void initialize() {
        if (editor != null) {
            editor.textProperty().addListener((obs, oldVal, newVal) -> updateCounts(newVal));
            editor.setStyle("-fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 13px;");
        }
        
        // Default UI Selections
        if (fontPicker != null) fontPicker.getSelectionModel().select("Helvetica");
        if (marginPicker != null) marginPicker.getSelectionModel().select("Standard (1.0 inch)");
    }

    private DocSettings getDocSettings() {
        DocSettings settings = new DocSettings();
        
        // 1. Map Margins
        String marginVal = marginPicker.getValue();
        if (marginVal != null) {
            if (marginVal.contains("Narrow")) settings.margin = 36f; // 0.5 inch
            else if (marginVal.contains("Wide")) settings.margin = 108f; // 1.5 inch
            else settings.margin = 72f; // 1.0 inch
        }

        // 2. Map Font
        settings.font = fontPicker.getValue() != null ? fontPicker.getValue() : "Helvetica";

        // 3. Map Headings
        if (headingField.getText() != null && !headingField.getText().isEmpty()) {
            settings.highlights = headingField.getText().split(",");
        }

        // 4. Map Toggles
        settings.showPageNumbers = pageNumbersToggle.isSelected();
        settings.convertToTables = tableMagicToggle.isSelected();

        return settings;
    }
    @FXML
    private void handleSave() {
        if (currentFile == null || editor.getText().isEmpty()) return;

        DocSettings userChoices = getDocSettings(); // Get the UI choices

        try {
            String originalName = currentFile.getName();
            File saveTarget = new File(currentFile.getParent(), originalName.replace(".pdf", "_Architected.pdf"));

            // PASS THE 4th ARGUMENT HERE
            engine.exportCorrectedPDF(editor.getText(), originalName, saveTarget, userChoices);

            fileNameLabel.setText("✨ ARCHITECTED: " + saveTarget.getName());
            toggleMode(true); 
            renderPDFToImage(saveTarget);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- EXISTING LOGIC BELOW (UNTOUCHED FOR STABILITY) ---

    public void setupWorkplace(File file, Stage stage) {
        if (file == null) return;
        this.currentFile = file;
        fileNameLabel.setText(file.getName().toUpperCase());
        
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            toggleMode(true); 
            renderPDFToImage(file);
            runBackgroundPDFProcess(file, false); 
        } else {
            toggleMode(false);
            loadPlainText(file);
        }
    }

    private void renderPDFToImage(File file) {
        new Thread(() -> {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDFRenderer renderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();
                
                Platform.runLater(() -> {
                    VBox pageContainer = (VBox) pdfScrollPane.getContent();
                    pageContainer.getChildren().clear(); 
                    pageContainer.setStyle("-fx-background-color: #e0e4ef; -fx-padding: 50;");
                });

                for (int i = 0; i < pageCount; i++) {
                    BufferedImage bim = renderer.renderImageWithDPI(i, 200);
                    Image fxImage = SwingFXUtils.toFXImage(bim, null);

                    Platform.runLater(() -> {
                        ImageView pageView = new ImageView(fxImage);
                        pageView.setFitWidth(850); 
                        pageView.setPreserveRatio(true);
                        
                        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
                        shadow.setColor(javafx.scene.paint.Color.rgb(0, 0, 0, 0.4));
                        shadow.setRadius(20);
                        shadow.setOffsetY(10);
                        pageView.setEffect(shadow);

                        ((VBox) pdfScrollPane.getContent()).getChildren().add(pageView);
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void toggleMode(boolean isPdf) {
        Platform.runLater(() -> {
            pdfScrollPane.setVisible(isPdf);
            pdfScrollPane.setManaged(isPdf);
            if (editorContainer != null) {
                editorContainer.setVisible(!isPdf);
                editorContainer.setManaged(!isPdf);
            }
            editor.setVisible(!isPdf);
            editor.setManaged(!isPdf);
            if (contentStack != null) contentStack.requestLayout(); 
        });
    }

    private void runBackgroundPDFProcess(File file, boolean shouldCorrect) {
        new Thread(() -> {
            try (PDDocument document = Loader.loadPDF(file)) { 
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true); 
                String extractedText = stripper.getText(document);
                Platform.runLater(() -> {
                    String result = shouldCorrect ? engine.performMagicCheck(extractedText) : extractedText;
                    editor.setText(result);
                    updateCounts(result);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleCheck() {
        if (currentFile == null) return;
        fileNameLabel.setText("✨ ENCHANTING " + currentFile.getName().toUpperCase() + "...");
        new Thread(() -> {
            try {
                String textToProcess = editor.getText();
                if (textToProcess == null || textToProcess.isEmpty()) {
                    try (PDDocument document = Loader.loadPDF(currentFile)) {
                        textToProcess = new PDFTextStripper().getText(document);
                    }
                }
                String correctedText = engine.performMagicCheck(textToProcess);
                Platform.runLater(() -> {
                    toggleMode(false); 
                    editor.setText(correctedText);
                    editor.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 15px; -fx-padding: 40;");
                    updateCounts(correctedText);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void updateCounts(String text) {
        if (text == null) return;
        String trimmed = text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        Platform.runLater(() -> {
            if (wordCountLabel != null) wordCountLabel.setText(words + " Words");
            if (charCountLabel != null) charCountLabel.setText(text.length() + " Characters");
        });
    }

    private void loadPlainText(File file) {
        new Thread(() -> {
            try {
                String content = Files.readString(file.toPath());
                Platform.runLater(() -> {
                    editor.setText(content);
                    updateCounts(content);
                });
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    @FXML private void handleGoBack() {
        ((Stage) fileNameLabel.getScene().getWindow()).close();
    }

    public void show(Stage stage, File file) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/spellbound/ui/WorkplaceView.fxml"));
            Parent root = loader.load();
            WorkplaceWindow controller = loader.getController();
            controller.setupWorkplace(file, stage);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}