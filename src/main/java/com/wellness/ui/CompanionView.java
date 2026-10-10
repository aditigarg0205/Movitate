package com.wellness.ui;

import com.wellness.model.User;
import com.wellness.service.CompanionService.Reply;
import com.wellness.service.Services;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Chat with the built-in companion (offline, rule-based; conversation is not saved). */
public class CompanionView implements View {
    private final VBox root = new VBox(12);
    private final VBox messages = new VBox(10);
    private final ScrollPane scroll = new ScrollPane(messages);
    private final TextField input = new TextField();
    private final Services s;
    private final User user;

    public CompanionView(Services s, User user) {
        this.s = s;
        this.user = user;
        root.setPadding(new Insets(26));

        Label note = Ui.muted("This companion is a simple built-in program (no internet, nothing is saved). "
                + "It is not a therapist or a real person.");

        FlowPane chips = new FlowPane(8, 8);
        for (String c : new String[]{"I feel anxious", "I'm feeling sad", "I'm stressed", "I can't sleep",
                "I feel lonely", "I did something good today"}) {
            Button b = new Button(c);
            b.getStyleClass().add("chip");
            b.setOnAction(e -> send(c));
            chips.getChildren().add(b);
        }

        messages.setPadding(new Insets(12));
        scroll.setFitToWidth(true);
        scroll.setMinHeight(300);
        scroll.setPrefHeight(420);
        scroll.getStyleClass().add("chat-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        messages.heightProperty().addListener((o, a, b) -> scroll.setVvalue(1.0));

        input.setPromptText("Type how you're feeling...");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setOnAction(e -> send(input.getText()));
        Button sendBtn = Ui.primary("Send");
        sendBtn.setOnAction(e -> send(input.getText()));
        Button clear = new Button("Clear chat");
        clear.setOnAction(e -> reset());
        HBox row = new HBox(8, input, sendBtn, clear);

        root.getChildren().addAll(Ui.heading("AI companion"), note, chips, scroll, row);
        reset();
    }

    private void reset() {
        messages.getChildren().clear();
        bubble(s.companion.greeting(user.displayName()), false, false);
    }

    private void send(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return;
        input.clear();
        bubble(t, true, false);
        Reply reply = s.companion.reply(user.id(), t);
        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(e -> bubble(reply.text(), false, reply.crisis()));
        pause.play();
    }

    private void bubble(String text, boolean mine, boolean crisis) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(520);
        l.getStyleClass().add(mine ? "bubble-user" : crisis ? "bubble-crisis" : "bubble-bot");
        HBox row = new HBox(l);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messages.getChildren().add(row);
    }

    @Override
    public Parent root() {
        return root;
    }
}
