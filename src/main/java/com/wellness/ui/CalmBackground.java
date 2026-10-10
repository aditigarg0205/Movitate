package com.wellness.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/** Sunny sky: a glowing sun with slowly turning rays, drifting clouds and warm light (no clicks captured). */
final class CalmBackground extends Pane {
    private final List<Animation> animations = new ArrayList<>();

    CalmBackground() {
        setMouseTransparent(true);

        // ---- warm glow behind the sun ----
        Circle glow = new Circle(260);
        glow.centerXProperty().bind(widthProperty().multiply(0.88));
        glow.centerYProperty().bind(heightProperty().multiply(0.16));
        glow.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff2a8", 0.85)), new Stop(0.5, Color.web("#ffe08a", 0.35)),
                new Stop(1, Color.web("#ffd36b", 0.0))));
        getChildren().add(glow);
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(6), glow);
        pulse.setFromX(1.0); pulse.setFromY(1.0); pulse.setToX(1.08); pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();
        animations.add(pulse);

        // ---- rotating rays ----
        Group rays = new Group();
        for (int i = 0; i < 16; i++) {
            double a = Math.toRadians(i * 22.5);
            double r1 = 70, r2 = (i % 2 == 0) ? 120 : 100;
            Line l = new Line(Math.cos(a) * r1, Math.sin(a) * r1, Math.cos(a) * r2, Math.sin(a) * r2);
            l.setStroke(Color.web("#ffd24d", 0.75));
            l.setStrokeWidth(5);
            l.setStrokeLineCap(StrokeLineCap.ROUND);
            rays.getChildren().add(l);
        }
        rays.layoutXProperty().bind(widthProperty().multiply(0.88));
        rays.layoutYProperty().bind(heightProperty().multiply(0.16));
        getChildren().add(rays);
        RotateTransition spin = new RotateTransition(Duration.seconds(90), rays);
        spin.setByAngle(360);
        spin.setCycleCount(Animation.INDEFINITE);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.play();
        animations.add(spin);

        // ---- the sun ----
        Circle sun = new Circle(58);
        sun.centerXProperty().bind(widthProperty().multiply(0.88));
        sun.centerYProperty().bind(heightProperty().multiply(0.16));
        sun.setFill(new RadialGradient(0, 0, 0.4, 0.4, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff8c4")), new Stop(0.6, Color.web("#ffe066")),
                new Stop(1, Color.web("#ffc533"))));
        getChildren().add(sun);

        // ---- drifting clouds: x%, y%, scale, seconds ----
        double[][] clouds = {{0.12, 0.14, 1.0, 38}, {0.46, 0.30, 0.75, 52}, {0.70, 0.62, 1.2, 46}, {0.22, 0.78, 0.9, 60}};
        for (double[] c : clouds) {
            Group cloud = cloud(c[2]);
            cloud.layoutXProperty().bind(widthProperty().multiply(c[0]));
            cloud.layoutYProperty().bind(heightProperty().multiply(c[1]));
            getChildren().add(cloud);
            TranslateTransition t = new TranslateTransition(Duration.seconds(c[3]), cloud);
            t.setByX(90);
            t.setAutoReverse(true);
            t.setCycleCount(Animation.INDEFINITE);
            t.setInterpolator(Interpolator.EASE_BOTH);
            t.play();
            animations.add(t);
        }

        // ---- soft warm light at the bottom ----
        Circle warm = new Circle(260);
        warm.centerXProperty().bind(widthProperty().multiply(0.15));
        warm.centerYProperty().bind(heightProperty().multiply(1.0));
        warm.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffd9a0", 0.55)), new Stop(1, Color.web("#ffd9a0", 0.0))));
        getChildren().add(warm);
    }

    private static Group cloud(double scale) {
        Color white = Color.web("#ffffff", 0.80);
        Ellipse base = new Ellipse(0, 18, 70, 22);
        Ellipse left = new Ellipse(-35, 4, 34, 24);
        Ellipse mid = new Ellipse(0, -8, 40, 30);
        Ellipse right = new Ellipse(36, 6, 30, 22);
        for (Ellipse e : new Ellipse[]{base, left, mid, right}) e.setFill(white);
        Group g = new Group(base, left, mid, right);
        g.setScaleX(scale);
        g.setScaleY(scale);
        return g;
    }

    void stop() {
        animations.forEach(Animation::stop);
    }
}
