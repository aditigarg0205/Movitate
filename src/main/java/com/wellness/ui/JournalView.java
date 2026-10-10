package com.wellness.ui;

import com.wellness.model.JournalEntry;
import com.wellness.model.User;
import com.wellness.service.Services;
import com.wellness.service.WellnessException;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class JournalView implements View {
    private final HBox root = new HBox(18);
    private final Services s;
    private final User user;
    private final ListView<JournalEntry> list = new ListView<>();
    private final TextField search = new TextField();
    private final TextField title = new TextField();
    private final TextArea body = new TextArea();
    private final Label msg = Ui.message();
    private JournalEntry editing;
    private boolean loading;

    public JournalView(Services s, User user) {
        this.s = s;
        this.user = user;
        root.setPadding(new Insets(26));

        search.setPromptText("Search entries...");
        search.textProperty().addListener((o, a, b) -> refresh());
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JournalEntry e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null : e.title() + "\n" + e.updatedAt().format(Ui.DATE_TIME));
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, e) -> {
            if (!loading && e != null) open(e);
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        Button newBtn = new Button("+ New entry");
        newBtn.setOnAction(e -> clearEditor());
        VBox left = new VBox(10, Ui.heading("Journal"), newBtn, search, list);
        left.setPrefWidth(300);
        left.setMinWidth(260);

        title.setPromptText("Title");
        body.setPromptText("Write freely. This is only visible to you on this computer.");
        body.setWrapText(true);
        VBox.setVgrow(body, Priority.ALWAYS);
        Button save = Ui.primary("Save");
        Button delete = new Button("Delete");
        delete.getStyleClass().add("danger");
        save.setOnAction(e -> save());
        delete.setOnAction(e -> delete());
        VBox right = new VBox(10, title, body, new HBox(10, save, delete), msg);
        right.getStyleClass().add("card");
        HBox.setHgrow(right, Priority.ALWAYS);
        right.setMinHeight(520);

        root.getChildren().addAll(left, right);
        refresh();
    }

    private void refresh() {
        loading = true;
        try {
            list.getItems().setAll(s.journal.list(user.id(), search.getText()));
            if (editing != null) {
                for (JournalEntry e : list.getItems()) {
                    if (e.id().equals(editing.id())) {
                        list.getSelectionModel().select(e);
                        break;
                    }
                }
            }
        } finally {
            loading = false;
        }
    }

    private void open(JournalEntry e) {
        editing = e;
        title.setText(e.title());
        body.setText(e.body());
        msg.setText("");
    }

    private void clearEditor() {
        editing = null;
        title.clear();
        body.clear();
        msg.setText("");
        list.getSelectionModel().clearSelection();
    }

    private void save() {
        try {
            if (editing == null) editing = s.journal.create(user.id(), title.getText(), body.getText());
            else editing = s.journal.update(user.id(), editing.id(), title.getText(), body.getText());
            Ui.success(msg, "Saved.");
            refresh();
        } catch (WellnessException | java.io.UncheckedIOException ex) {
            Ui.error(msg, ex.getMessage());
        }
    }

    private void delete() {
        if (editing == null) {
            Ui.error(msg, "Select an entry to delete.");
            return;
        }
        if (Ui.confirm("Delete this entry permanently?")) {
            try {
                s.journal.delete(user.id(), editing.id());
                clearEditor();
                refresh();
            } catch (java.io.UncheckedIOException ex) {
                Ui.error(msg, ex.getMessage());
            }
        }
    }

    @Override
    public Parent root() {
        return root;
    }
}
