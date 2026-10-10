package com.wellness.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Sidebar + page area for the user portal. */
public class Shell implements View {
    private final StackPane root = new StackPane();
    private final BorderPane layout = new BorderPane();
    private final CalmBackground background = new CalmBackground();
    private final Map<Button, Supplier<View>> pages = new LinkedHashMap<>();
    private View current;
    private Button active;

    public Shell(String brandText, String hiText, String sidebarStyle,
                 LinkedHashMap<String, Supplier<View>> pageDefs, Runnable onLogout) {
        for (Map.Entry<String, Supplier<View>> e : pageDefs.entrySet())
            pages.put(new Button(e.getKey()), e.getValue());

        Label brand = new Label(brandText);
        brand.getStyleClass().add("brand");
        Label hi = new Label(hiText);
        hi.getStyleClass().add("hi");

        VBox side = new VBox(4, brand, hi);
        side.getStyleClass().addAll("sidebar", sidebarStyle);
        side.setPadding(new Insets(20, 12, 20, 12));
        side.setPrefWidth(230);
        side.setMinWidth(230);
        Region gap = new Region();
        gap.setMinHeight(12);
        side.getChildren().add(gap);

        for (Button b : pages.keySet()) {
            b.getStyleClass().add("nav");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setOnAction(e -> show(b));
            side.getChildren().add(b);
        }

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        Button logout = new Button("⎋  Log out");
        logout.getStyleClass().add("nav");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.setOnAction(e -> onLogout.run());
        side.getChildren().addAll(grow, logout);

        layout.setLeft(side);
        root.getChildren().addAll(background, layout);
        show(pages.keySet().iterator().next());
    }

    private void show(Button b) {
        if (current != null) current.dispose();
        if (active != null) active.getStyleClass().remove("active");
        active = b;
        b.getStyleClass().add("active");
        current = pages.get(b).get();
        ScrollPane sp = new ScrollPane(current.root());
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("page-scroll");
        layout.setCenter(sp);
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void dispose() {
        if (current != null) current.dispose();
        background.stop();
    }
}
