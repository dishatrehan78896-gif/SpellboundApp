module SpellboundApp { // Change this from com.spellbound
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;
    requires org.apache.pdfbox; 
    requires javafx.swing;
    opens com.spellbound.ui to javafx.fxml;
    opens com.spellbound.app to javafx.fxml;
    opens com.spellbound.logic to javafx.base;

    exports com.spellbound.app;
    exports com.spellbound.ui;
    exports com.spellbound.logic;
}