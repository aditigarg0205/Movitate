package com.wellness.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/** Guided breathing: inhale 4s, hold 4s, exhale 6s. */
public class BreathingView implements View {
    private final VBox root = new VBox(18);
    private final Timeline timeline;
    private final Label phase = new Label("Press start");
    private final Label cycles = Ui.muted("Cycles completed: 0");
    private int done;

    public BreathingView() {
        root.setPadding(new Insets(26));
        root.setAlignment(Pos.TOP_CENTER);

        Circle circle = new Circle(70);
        circle.getStyleClass().add("breath-circle");
        phase.getStyleClass().add("heading2");
        StackPane stage = new StackPane(circle, phase);
        stage.setMinHeight(300);

        KeyValue small = new KeyValue(circle.scaleXProperty(), 1.0, Interpolator.EASE_BOTH);
        KeyValue smallY = new KeyValue(circle.scaleYProperty(), 1.0, Interpolator.EASE_BOTH);
        KeyValue big = new KeyValue(circle.scaleXProperty(), 1.7, Interpolator.EASE_BOTH);
        KeyValue bigY = new KeyValue(circle.scaleYProperty(), 1.7, Interpolator.EASE_BOTH);

        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> phase.setText("Breathe in..."), small, smallY),
                new KeyFrame(Duration.seconds(4), e -> phase.setText("Hold"), big, bigY),
                new KeyFrame(Duration.seconds(8), e -> phase.setText("Breathe out..."), big, bigY),
                new KeyFrame(Duration.seconds(14), e -> {
                    done++;
                    cycles.setText("Cycles completed: " + done);
                }, small, smallY));
        timeline.setCycleCount(Timeline.INDEFINITE);

        Button start = Ui.primary("Start");
        Button stop = new Button("Stop");
        start.setOnAction(e -> {
            done = 0;
            cycles.setText("Cycles completed: 0");
            timeline.playFromStart();
        });
        stop.setOnAction(e -> reset(circle));

        root.getChildren().addAll(
                Ui.heading("Breathe"),
                Ui.muted("Follow the circle: in for 4 seconds, hold for 4, out for 6. Do a few rounds, "
                        + "and let your shoulders drop."),
                stage, new javafx.scene.layout.HBox(10, start, stop) {{ setAlignment(Pos.CENTER); }}, cycles);
    }

    private void reset(Circle c) {
        timeline.stop();
        c.setScaleX(1);
        c.setScaleY(1);
        phase.setText("Press start");
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void dispose() {
        timeline.stop();
    }
}
