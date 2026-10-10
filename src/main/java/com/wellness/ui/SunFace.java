package com.wellness.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/** A small animated sun with a face: happy, calm (meditating) or sad (behind a cloud). */
final class SunFace extends Group {
    enum Mood { HAPPY, CALM, SAD }

    private final List<Animation> animations = new ArrayList<>();

    SunFace(Mood mood, double radius) {
        boolean sad = mood == Mood.SAD;
        double r = radius;

        // calm: soft aura rings that breathe in and out
        if (mood == Mood.CALM) {
            for (int i = 0; i < 2; i++) {
                Circle aura = new Circle(r * (1.35 + i * 0.3));
                aura.setFill(Color.web("#cfe9ff", 0.28 - i * 0.1));
                getChildren().add(aura);
                ScaleTransition t = new ScaleTransition(Duration.seconds(4 + i * 1.5), aura);
                t.setToX(1.12); t.setToY(1.12);
                t.setAutoReverse(true);
                t.setCycleCount(Animation.INDEFINITE);
                t.setInterpolator(Interpolator.EASE_BOTH);
                t.play();
                animations.add(t);
            }
        }

        // rays
        Group rays = new Group();
        int n = sad ? 8 : 12;
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(i * 360.0 / n);
            Line l = new Line(Math.cos(a) * r * 1.15, Math.sin(a) * r * 1.15,
                    Math.cos(a) * r * (sad ? 1.35 : 1.55), Math.sin(a) * r * (sad ? 1.35 : 1.55));
            l.setStroke(Color.web(sad ? "#e6cf8f" : "#ffd24d", sad ? 0.7 : 0.9));
            l.setStrokeWidth(r * 0.12);
            l.setStrokeLineCap(StrokeLineCap.ROUND);
            rays.getChildren().add(l);
        }
        getChildren().add(rays);
        if (!sad) {
            RotateTransition spin = new RotateTransition(Duration.seconds(mood == Mood.CALM ? 120 : 40), rays);
            spin.setByAngle(360);
            spin.setCycleCount(Animation.INDEFINITE);
            spin.setInterpolator(Interpolator.LINEAR);
            spin.play();
            animations.add(spin);
        }

        // face
        Circle face = new Circle(r);
        face.setFill(new RadialGradient(0, 0, 0.4, 0.35, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(sad ? "#fff1c9" : "#fff8c4")),
                new Stop(0.65, Color.web(sad ? "#f2d98c" : "#ffe066")),
                new Stop(1, Color.web(sad ? "#e8c46a" : "#ffc533"))));
        getChildren().add(face);

        Color ink = Color.web("#6b4a12");
        double ex = r * 0.38, ey = -r * 0.15;
        switch (mood) {
            case HAPPY -> {
                getChildren().addAll(dot(-ex, ey, r * 0.09, ink), dot(ex, ey, r * 0.09, ink));
                getChildren().add(arc(0, r * 0.05, r * 0.5, r * 0.36, 200, 140, ink, r * 0.07));
                getChildren().addAll(dot(-r * 0.62, r * 0.2, r * 0.13, Color.web("#ff9f80", 0.45)),
                        dot(r * 0.62, r * 0.2, r * 0.13, Color.web("#ff9f80", 0.45)));
                TranslateTransition bob = new TranslateTransition(Duration.seconds(1.4), this);
                bob.setByY(-5);
                bob.setAutoReverse(true);
                bob.setCycleCount(Animation.INDEFINITE);
                bob.setInterpolator(Interpolator.EASE_BOTH);
                bob.play();
                animations.add(bob);
            }
            case CALM -> {
                // closed, peaceful eyes and a gentle smile
                getChildren().add(arc(-ex, ey, r * 0.2, r * 0.12, 200, 140, ink, r * 0.06));
                getChildren().add(arc(ex, ey, r * 0.2, r * 0.12, 200, 140, ink, r * 0.06));
                getChildren().add(arc(0, r * 0.22, r * 0.25, r * 0.14, 200, 140, ink, r * 0.055));
            }
            case SAD -> {
                getChildren().addAll(dot(-ex, ey, r * 0.08, ink), dot(ex, ey, r * 0.08, ink));
                getChildren().add(arc(0, r * 0.5, r * 0.4, r * 0.25, 20, 140, ink, r * 0.07));
                Circle tear = dot(ex + 4, ey + r * 0.28, r * 0.07, Color.web("#7fb8e8", 0.9));
                getChildren().add(tear);
                TranslateTransition drop = new TranslateTransition(Duration.seconds(2.2), tear);
                drop.setByY(r * 0.5);
                drop.setCycleCount(Animation.INDEFINITE);
                drop.play();
                animations.add(drop);
                // cloud covering the lower-right of the sun
                Group cloud = new Group(cloudPart(-r * 0.55, r * 0.55, r * 0.7, r * 0.34),
                        cloudPart(-r * 0.05, r * 0.4, r * 0.62, r * 0.42),
                        cloudPart(r * 0.5, r * 0.58, r * 0.6, r * 0.32));
                getChildren().add(cloud);
                TranslateTransition drift = new TranslateTransition(Duration.seconds(7), cloud);
                drift.setByX(r * 0.25);
                drift.setAutoReverse(true);
                drift.setCycleCount(Animation.INDEFINITE);
                drift.setInterpolator(Interpolator.EASE_BOTH);
                drift.play();
                animations.add(drift);
            }
        }
    }

    private static Circle dot(double x, double y, double rad, Color c) {
        Circle d = new Circle(x, y, rad);
        d.setFill(c);
        return d;
    }

    private static Arc arc(double cx, double cy, double rx, double ry, double start, double len,
                           Color stroke, double width) {
        Arc a = new Arc(cx, cy, rx, ry, start, len);
        a.setType(ArcType.OPEN);
        a.setFill(null);
        a.setStroke(stroke);
        a.setStrokeWidth(width);
        a.setStrokeLineCap(StrokeLineCap.ROUND);
        return a;
    }

    private static Ellipse cloudPart(double cx, double cy, double rx, double ry) {
        Ellipse e = new Ellipse(cx, cy, rx, ry);
        e.setFill(Color.web("#b9c4d0", 0.92));
        return e;
    }

    void stop() {
        animations.forEach(Animation::stop);
    }
}
