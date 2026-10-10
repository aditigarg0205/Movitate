package com.wellness.ui;

import com.wellness.model.MoodEntry;
import com.wellness.model.User;
import com.wellness.service.Services;
import com.wellness.service.WellnessException;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MoodView implements View {
    private final VBox root = new VBox(16);
    private final ListView<MoodEntry> history = new ListView<>();

    public MoodView(Services s, User user) {
        root.setPadding(new Insets(26));

        ToggleGroup group = new ToggleGroup();
        HBox choices = new HBox(10);
        for (int m = 1; m <= 5; m++) {
            ToggleButton b = new ToggleButton(Ui.moodLabel(m));
            b.setUserData(m);
            b.setToggleGroup(group);
            b.getStyleClass().add("mood-btn");
            choices.getChildren().add(b);
        }

        TextArea note = new TextArea();
        note.setPromptText("Anything you want to remember about how you feel? (optional, max 500 characters)");
        note.setPrefRowCount(3);
        note.setWrapText(true);
        Label msg = Ui.message();
        Button save = Ui.primary("Save check-in");

        save.setOnAction(e -> {
            Toggle t = group.getSelectedToggle();
            if (t == null) {
                Ui.error(msg, "Choose how you're feeling first.");
                return;
            }
            try {
                s.mood.log(user.id(), (int) t.getUserData(), note.getText());
                Ui.success(msg, "Saved. Thanks for checking in with yourself.");
                note.clear();
                group.selectToggle(null);
                refresh(s, user);
            } catch (WellnessException | java.io.UncheckedIOException ex) {
                Ui.error(msg, ex.getMessage());
            }
        });

        history.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MoodEntry m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setText(null);
                    return;
                }
                String extra = m.note().isBlank() ? "" : "  -  " + m.note().replace('\n', ' ');
                setText(m.at().format(Ui.DATE_TIME) + "   " + Ui.moodLabel(m.mood()) + extra);
            }
        });
        history.setPrefHeight(320);
        refresh(s, user);

        root.getChildren().addAll(
                Ui.heading("How are you feeling right now?"),
                Ui.card(choices, note, save, msg),
                Ui.heading2("Recent check-ins"),
                history);
    }

    private void refresh(Services s, User user) {
        history.getItems().setAll(s.mood.recent(user.id(), 50));
    }

    @Override
    public Parent root() {
        return root;
    }
}
