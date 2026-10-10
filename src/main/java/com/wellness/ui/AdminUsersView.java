package com.wellness.ui;

import com.wellness.service.AdminService.UserRow;
import com.wellness.service.Services;
import com.wellness.service.WellnessException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.UncheckedIOException;
import java.util.function.Function;

/** Manage accounts: disable/enable, reset password, delete. No access to personal content. */
public class AdminUsersView implements View {
    private final VBox root = new VBox(14);
    private final TableView<UserRow> table = new TableView<>();
    private final Label msg = Ui.message();
    private final Services s;

    public AdminUsersView(Services s) {
        this.s = s;
        root.setPadding(new Insets(26));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No users have registered yet."));
        table.setPrefHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
        addColumn("Username", r -> r.user().username());
        addColumn("Name", r -> r.user().displayName());
        addColumn("Joined", r -> r.user().createdAt().format(Ui.DAY) + " " + r.user().createdAt().getYear());
        addColumn("Check-ins", r -> String.valueOf(r.checkIns()));
        addColumn("Journal entries", r -> String.valueOf(r.journalEntries()));
        addColumn("Last check-in", r -> r.lastCheckIn() == null ? "-" : r.lastCheckIn().format(Ui.DATE_TIME));
        addColumn("Status", r -> r.user().disabled() ? "Disabled" : "Active");

        Button toggle = new Button("Disable / enable");
        Button reset = new Button("Reset password");
        Button delete = new Button("Delete user");
        delete.getStyleClass().add("danger");
        Button refresh = new Button("Refresh");

        toggle.setOnAction(e -> act(row -> s.admin.setDisabled(row.user().id(), !row.user().disabled())));
        reset.setOnAction(e -> act(row -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Reset password");
            d.setHeaderText("New temporary password for " + row.user().username());
            d.setContentText("Password (8+ chars, letter and number):");
            d.showAndWait().ifPresent(pw -> {
                s.admin.resetPassword(row.user().id(), pw);
                Ui.success(msg, "Password updated. Tell the user to log in with the new password.");
            });
        }));
        delete.setOnAction(e -> act(row -> {
            if (Ui.confirm("Delete " + row.user().username() + " and ALL of their data? This cannot be undone.")) {
                s.admin.deleteUserAndData(row.user().id());
                Ui.success(msg, "User deleted.");
            }
        }));
        refresh.setOnAction(e -> load());

        root.getChildren().addAll(Ui.heading("Users"),
                Ui.muted("You can manage accounts here. Journal text and mood notes are not shown to admins."),
                table, new HBox(10, toggle, reset, delete, refresh), msg);
        load();
    }

    private void addColumn(String title, Function<UserRow, String> value) {
        TableColumn<UserRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        table.getColumns().add(c);
    }

    private void load() {
        table.getItems().setAll(s.admin.userRows());
    }

    private void act(java.util.function.Consumer<UserRow> action) {
        UserRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            Ui.error(msg, "Select a user in the table first.");
            return;
        }
        try {
            action.accept(row);
            load();
        } catch (WellnessException | UncheckedIOException ex) {
            Ui.error(msg, ex.getMessage());
        }
    }

    @Override
    public Parent root() {
        return root;
    }
}
