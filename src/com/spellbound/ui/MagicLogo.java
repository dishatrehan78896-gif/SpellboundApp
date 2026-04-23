package com.spellbound.ui;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;

/**
 * The animated heart of SpellBound. 
 * Featuring a rotating halo and a pulsating magical 'S' swirl.
 */
public class MagicLogo extends StackPane {

    public MagicLogo(double size) {
        // 1. Theme Gradient (Deep Purple to Soft Coral)
        LinearGradient magicGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#5131a9")), 
            new Stop(1, Color.web("#f98d72"))  
        );

        // 2. The Rotating Halo
        Circle halo = new Circle(size / 2.2);
        halo.setFill(Color.TRANSPARENT);
        halo.setStroke(magicGradient);
        halo.setStrokeWidth(size * 0.05);
        halo.setOpacity(0.5);
        halo.getStrokeDashArray().addAll(size * 0.2, size * 0.1); // Creates a "dashed" magical ring
        
        DropShadow haloGlow = new DropShadow();
        haloGlow.setColor(Color.web("#5131a9", 0.4));
        haloGlow.setRadius(size * 0.2);
        halo.setEffect(haloGlow);

        // 3. The Magical 'S' Swirl
        SVGPath swirl = new SVGPath();
        // Refined SVG path for a more elegant, centered 'S'
        swirl.setContent("M20,15 C35,5 45,25 30,30 C15,35 25,55 40,45"); 
        swirl.setStroke(magicGradient);
        swirl.setStrokeWidth(size * 0.08);
        swirl.setFill(Color.TRANSPARENT);
        swirl.setStrokeLineCap(StrokeLineCap.ROUND);
        
        // Accurate Scaling
        double scaleFactor = size / 60.0;
        swirl.setScaleX(scaleFactor);
        swirl.setScaleY(scaleFactor);

        // 4. ANIMATION: Continuous Rotation
        RotateTransition rotate = new RotateTransition(Duration.seconds(10), halo);
        rotate.setByAngle(360);
        rotate.setCycleCount(Timeline.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.play();

        // 5. ANIMATION: Pulsating "Breathing" Effect
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.5), swirl);
        pulse.setFromX(scaleFactor);
        pulse.setFromY(scaleFactor);
        pulse.setToX(scaleFactor * 1.1);
        pulse.setToY(scaleFactor * 1.1);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();

        // 6. INTERACTION: Reaction on Hover
        this.setOnMouseEntered(e -> {
            rotate.setRate(4.0); // Speed up rotation when hovered
            halo.setOpacity(1.0);
        });
        this.setOnMouseExited(e -> {
            rotate.setRate(1.0); // Return to gentle spin
            halo.setOpacity(0.5);
        });

        // Assemble
        this.getChildren().addAll(halo, swirl);
        this.setMinWidth(size);
        this.setMinHeight(size);
        this.setMaxSize(size, size);
    }
}