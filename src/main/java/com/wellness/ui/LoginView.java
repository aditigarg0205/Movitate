package com.wellness.ui;

import com.wellness.model.User;
import com.wellness.service.Services;
import com.wellness.service.WellnessException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.UncheckedIOException;
import java.util.function.Consumer;

public class LoginView implements View {
    private final StackPane root;
    private final CalmBackground background = new CalmBackground();

    public LoginView(Services s, Consumer<User> onLogin) {
        Label title = new Label("MindEase");
        title.getStyleClass().add("app-title");
        Label sub = Ui.muted("A calm place to check in with yourself.");

        Tab userTab = new Tab("Log in", loginForm(s, onLogin, false));
        Tab registerTab = new Tab("Create account", registerForm(s, onLogin));
        Tab adminTab = new Tab("Admin portal",
                s.auth.adminExists() ? loginForm(s, onLogin, true) : adminSetupForm(s, onLogin));
        TabPane tabs = new TabPane(userTab, registerTab, adminTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox card = Ui.card(title, sub, tabs);
        card.setMaxWidth(440);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        root = new StackPane(background, card);
        root.setAlignment(Pos.CENTER);
    }

    private VBox loginForm(Services s, Consumer<User> onLogin, boolean admin) {
        TextField user = new TextField();
        user.setPromptText(admin ? "Admin username" : "Username");
        PasswordField pw = new PasswordField();
        pw.setPromptText("Password");
        Label msg = Ui.message();
        Button go = Ui.primary(admin ? "Log in as admin" : "Log in");
        go.setMaxWidth(Double.MAX_VALUE);

        Runnable action = () -> {
            try {
                onLogin.accept(s.auth.login(user.getText(), pw.getText(), admin));
            } catch (WellnessException | UncheckedIOException e) {
                Ui.error(msg, e.getMessage());
                pw.clear();
            }
        };
        go.setOnAction(e -> action.run());
        pw.setOnAction(e -> action.run());

        VBox box = new VBox(10, user, pw, go, msg);
        box.setPadding(new Insets(14, 0, 0, 0));
        return box;
    }

    private VBox registerForm(Services s, Consumer<User> onLogin) {
        TextField user = new TextField();
        user.setPromptText("Username (3-20 letters/numbers/_)");
        TextField name = new TextField();
        name.setPromptText("Your name (shown in the app)");
        PasswordField pw = new PasswordField();
        pw.setPromptText("Password (8+ chars, letter and number)");
        PasswordField pw2 = new PasswordField();
        pw2.setPromptText("Repeat password");
        Label msg = Ui.message();
        Button go = Ui.primary("Create account");
        go.setMaxWidth(Double.MAX_VALUE);

        go.setOnAction(e -> {
            if (!pw.getText().equals(pw2.getText())) {
                Ui.error(msg, "Passwords do not match.");
                return;
            }
            try {
                s.auth.register(user.getText(), name.getText(), pw.getText());
                onLogin.accept(s.auth.login(user.getText(), pw.getText(), false));
            } catch (WellnessException | UncheckedIOException ex) {
                Ui.error(msg, ex.getMessage());
            }
        });

        VBox box = new VBox(10, user, name, pw, pw2, go, msg);
        box.setPadding(new Insets(14, 0, 0, 0));
        return box;
    }

    /** Shown only until the first administrator exists. */
    private VBox adminSetupForm(Services s, Consumer<User> onLogin) {
        Label info = Ui.muted("First-time setup: no administrator exists yet. Create the admin account "
                + "for this computer. Admins see anonymous statistics and manage accounts, never journals.");
        TextField user = new TextField();
        user.setPromptText("Admin username");
        TextField name = new TextField();
        name.setPromptText("Admin name");
        PasswordField pw = new PasswordField();
        pw.setPromptText("Password (8+ chars, letter and number)");
        PasswordField pw2 = new PasswordField();
        pw2.setPromptText("Repeat password");
        Label msg = Ui.message();
        Button go = Ui.primary("Create admin account");
        go.setMaxWidth(Double.MAX_VALUE);

        go.setOnAction(e -> {
            if (!pw.getText().equals(pw2.getText())) {
                Ui.error(msg, "Passwords do not match.");
                return;
            }
            try {
                s.auth.createFirstAdmin(user.getText(), name.getText(), pw.getText());
                onLogin.accept(s.auth.login(user.getText(), pw.getText(), true));
            } catch (WellnessException | UncheckedIOException ex) {
                Ui.error(msg, ex.getMessage());
            }
        });

        VBox box = new VBox(10, info, user, name, pw, pw2, go, msg);
        box.setPadding(new Insets(14, 0, 0, 0));
        return box;
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void dispose() {
        background.stop();
    }
}
