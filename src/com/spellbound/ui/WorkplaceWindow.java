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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class WorkplaceWindow {

    
    @FXML private TextFlow editorFlow; 
    @FXML private TextArea editor; 
    @FXML private VBox editorContainer;
    @FXML private Label fileNameLabel;
    @FXML private Label wordCountLabel; 
    @FXML private Label charCountLabel;
    @FXML private ScrollPane pdfScrollPane;
    @FXML private StackPane contentStack;
    @FXML private ComboBox<String> fontPicker;
    @FXML private ComboBox<String> marginPicker;
    @FXML private TextField headingField;
    @FXML private CheckBox pageNumbersToggle;
    @FXML private ScrollPane magicOverlay;

    private CorrectionEngine engine = new CorrectionEngine();
    private File currentFile;
    private String rawTextContent = ""; 

    public static class DocSettings {
        public float margin = 72f;
        public String font = "Helvetica";
        public boolean showPageNumbers = true;
        public String[] highlights = new String[0];
    }

    

    @FXML
    private void handleGoBack(ActionEvent event) {
        Stage stage = (Stage) editorFlow.getScene().getWindow();
        HomeWindow.show(stage, "user@spellbound.com"); 
    }

    @FXML
    private void handleNewDocument() {
        this.currentFile = null;
        this.rawTextContent = "";
        fileNameLabel.setText("NEW UNTITLED DOCUMENT");
        if (editorFlow != null) editorFlow.getChildren().clear();
        toggleMode(false); 
    }
    @FXML
    private void handleCheck() {
       
        String input = (editor != null && !editor.getText().isEmpty()) ? editor.getText() : rawTextContent;
        
        if (input == null || input.trim().isEmpty()) {
            System.out.println("DEBUG: No text found in editor or rawTextContent");
            return;
        }

        
        String corrected = engine.performMagicCheck(input);
        this.rawTextContent = corrected; 

        Platform.runLater(() -> {
            editorFlow.getChildren().clear();
            String[] words = corrected.split("\\s+");
            
            for (String word : words) {
                Text textNode = new Text(word + " ");
                textNode.setStyle("-fx-font-family: 'Poppins'; -fx-font-size: 15px;");

                String clean = word.replaceAll("[.,!?;]", "");
                if (engine.isACorrectedWord(clean)) {
                    textNode.setFill(Color.web("#00c853")); // GREEN
                    textNode.setStyle("-fx-font-family: 'Poppins'; -fx-font-size: 15px; -fx-font-weight: bold;");
                } else {
                    textNode.setFill(Color.BLACK);
                }
                editorFlow.getChildren().add(textNode);
            }

            
            editor.setVisible(false);
            editor.setManaged(false);
            
            
            magicOverlay.setVisible(true);
            magicOverlay.setManaged(true);
            magicOverlay.setOpacity(1.0); 
            
           
            pdfScrollPane.setVisible(false);
            pdfScrollPane.setManaged(false);
            
           
            editorContainer.setVisible(true);
            editorContainer.setManaged(true);

            updateCounts(corrected);
        });
    }
    private void toggleMode(boolean isPdf) {
        Platform.runLater(() -> {
            pdfScrollPane.setVisible(isPdf);
            pdfScrollPane.setManaged(isPdf);
            editorContainer.setVisible(!isPdf);
            editorContainer.setManaged(!isPdf);

            if (!isPdf && editorFlow != null) {
                editorFlow.requestFocus();
            }
            contentStack.requestLayout(); 
        });
    }

    @FXML
    private void handleSave() {
    	((VBox) pdfScrollPane.getContent()).getChildren().clear();
    	System.gc(); 
        if (rawTextContent.isEmpty()) return;

        DocSettings userChoices = getDocSettings();
        try {
            String baseDir = (currentFile != null) ? currentFile.getParent() : System.getProperty("user.home") + "/Documents";
            String name = (currentFile != null) ? currentFile.getName().replace(".pdf", "") : "Untitled_Enchantment";
            File saveTarget = new File(baseDir, name + "_Architected.pdf");

            
            engine.exportCorrectedPDF(rawTextContent, name, saveTarget, userChoices);
            
            fileNameLabel.setText("✨ ARCHITECTED: " + saveTarget.getName());
            toggleMode(true); 
            renderPDFToImage(saveTarget);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private DocSettings getDocSettings() {
        DocSettings settings = new DocSettings();
        String marginVal = marginPicker.getValue();
        if (marginVal != null) {
            if (marginVal.contains("Narrow")) settings.margin = 36f;
            else if (marginVal.contains("Wide")) settings.margin = 108f;
            else settings.margin = 72f;
        }
        settings.font = fontPicker.getValue() != null ? fontPicker.getValue() : "Helvetica";
        if (headingField.getText() != null && !headingField.getText().isEmpty()) {
            settings.highlights = headingField.getText().split(",");
        }
        settings.showPageNumbers = pageNumbersToggle.isSelected();
        return settings;
    }

    public void setupWorkplace(File file, Stage stage) {
        if (file == null) {
            handleNewDocument();
            return;
        }
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
                Platform.runLater(() -> ((VBox) pdfScrollPane.getContent()).getChildren().clear());

                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    BufferedImage bim = renderer.renderImageWithDPI(i, 200);
                    Image fxImage = SwingFXUtils.toFXImage(bim, null);
                    Platform.runLater(() -> {
                        ImageView pageView = new ImageView(fxImage);
                        pageView.setFitWidth(750);
                        pageView.setPreserveRatio(true);
                        ((VBox) pdfScrollPane.getContent()).getChildren().add(pageView);
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }
   
    private void runBackgroundPDFProcess(File file, boolean shouldCorrect) {
        new Thread(() -> {
            try (PDDocument document = Loader.loadPDF(file)) { 
                String text = new PDFTextStripper().getText(document);
                this.rawTextContent = shouldCorrect ? engine.performMagicCheck(text) : text;
                Platform.runLater(() -> {
                    updateEditorFlow(rawTextContent);
                    updateCounts(rawTextContent);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
   
    private void updateCounts(String text) {
        if (text == null) return;
        int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        Platform.runLater(() -> {
            if (wordCountLabel != null) wordCountLabel.setText(words + " Words");
            if (charCountLabel != null) charCountLabel.setText(text.length() + " Characters");
        });
    }

    private void updateEditorFlow(String text) {
        editorFlow.getChildren().clear();
        Text t = new Text(text);
        t.setStyle("-fx-font-family: 'Poppins'; -fx-font-size: 15px;");
        editorFlow.getChildren().add(t);
    }
    
    private void loadPlainText(File file) {
        try {
            this.rawTextContent = Files.readString(file.toPath());
            updateEditorFlow(rawTextContent);
            updateCounts(rawTextContent);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
