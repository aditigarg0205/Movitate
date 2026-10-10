package com.wellness;

import com.wellness.model.User;
import com.wellness.service.Services;
import com.wellness.ui.AdminMainView;
import com.wellness.ui.LoginView;
import com.wellness.ui.MainView;
import com.wellness.ui.View;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.nio.file.Path;

public class App extends Application {
    private Services services;
    private Scene scene;
    private Stage stage;
    private View currentView;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".mindease");
            services = new Services(dir);
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, "Could not open data storage:\n" + e.getMessage()).showAndWait();
            return;
        }
        stage.setTitle("MindEase - Mental Wellness Portal");
        stage.setMinWidth(980);
        stage.setMinHeight(660);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        setView(new LoginView(services, this::showMain));
    }

    private void showMain(User user) {
        setView(user.isAdmin()
                ? new AdminMainView(services, user, this::showLogin)
                : new MainView(services, user, this::showLogin));
    }

    private void setView(View view) {
        if (currentView != null) currentView.dispose();
        currentView = view;
        if (scene == null) {
            scene = new Scene(view.root(), 1100, 720);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(view.root());
        }
    }

    @Override
    public void stop() {
        if (currentView != null) currentView.dispose();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
