package com.wellness.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/** Small shared helpers. */
final class Ui {
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");

    private Ui() {}

    static String moodLabel(int mood) {
        return switch (mood) {
            case 1 -> "😞 Very low";
            case 2 -> "😕 Low";
            case 3 -> "😐 Okay";
            case 4 -> "🙂 Good";
            case 5 -> "😄 Great";
            default -> "?";
        };
    }

    static String moodEmoji(int mood) {
        return switch (mood) {
            case 1 -> "😞";
            case 2 -> "😕";
            case 3 -> "😐";
            case 4 -> "🙂";
            case 5 -> "😄";
            default -> "";
        };
    }

    /** Soft pastel colour for an average mood (1..5). */
    static String moodColor(double avg) {
        int m = (int) Math.max(1, Math.min(5, Math.round(avg)));
        return switch (m) {
            case 1 -> "#f5b7b1";
            case 2 -> "#f8d9a8";
            case 3 -> "#f7efb2";
            case 4 -> "#cfe8c5";
            default -> "#9fd8b4";
        };
    }

    static Label heading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("heading");
        return l;
    }

    static Label heading2(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("heading2");
        return l;
    }

    static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    static Label message() {
        Label l = new Label();
        l.setWrapText(true);
        return l;
    }

    static void error(Label l, String text) {
        l.getStyleClass().setAll("label", "error");
        l.setText(text);
    }

    static void success(Label l, String text) {
        l.getStyleClass().setAll("label", "success");
        l.setText(text);
    }

    static Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("primary");
        return b;
    }

    static VBox card(javafx.scene.Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("card");
        return box;
    }

    static boolean confirm(String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text);
        return a.showAndWait().filter(b -> b == javafx.scene.control.ButtonType.OK).isPresent();
    }
}
