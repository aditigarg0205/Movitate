package com.wellness.ui;

import com.wellness.model.AssessmentResult;
import com.wellness.model.User;
import com.wellness.service.AssessmentService;
import com.wellness.service.AssessmentService.Kind;
import com.wellness.service.Services;
import com.wellness.service.WellnessException;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class AssessmentView implements View {
    private final VBox root = new VBox(16);
    private final VBox form = new VBox(12);
    private final Label result = Ui.message();
    private final ListView<String> history = new ListView<>();
    private final List<ToggleGroup> groups = new ArrayList<>();

    public AssessmentView(Services s, User user) {
        root.setPadding(new Insets(26));

        ComboBox<Kind> kind = new ComboBox<>();
        kind.getItems().addAll(Kind.values());
        kind.getSelectionModel().selectFirst();
        kind.setOnAction(e -> build(kind.getValue()));

        Button submit = Ui.primary("See my result");
        submit.setOnAction(e -> {
            Kind k = kind.getValue();
            int[] answers = new int[groups.size()];
            for (int i = 0; i < groups.size(); i++) {
                Toggle t = groups.get(i).getSelectedToggle();
                if (t == null) {
                    Ui.error(result, "Please answer every question.");
                    return;
                }
                answers[i] = (int) t.getUserData();
            }
            try {
                AssessmentResult r = s.assessment.submit(user.id(), k, answers);
                Ui.success(result, "Score: " + r.score() + " / 6\n" + AssessmentService.interpret(r.score()));
                refresh(s, user);
            } catch (WellnessException | java.io.UncheckedIOException ex) {
                Ui.error(result, ex.getMessage());
            }
        });

        history.setPrefHeight(200);
        build(kind.getValue());
        refresh(s, user);

        root.getChildren().addAll(
                Ui.heading("Self-check"),
                Ui.muted("Quick, well-known screening questions. This is not a diagnosis and does not replace "
                        + "professional advice."),
                kind,
                Ui.card(form, submit, result),
                Ui.heading2("Previous results"),
                history);
    }

    private void build(Kind k) {
        form.getChildren().clear();
        groups.clear();
        result.setText("");
        form.getChildren().add(new Label("Over the last 2 weeks, how often have you been bothered by:"));
        for (String q : k.questions) {
            Label ql = new Label(q);
            ql.getStyleClass().add("question");
            ToggleGroup g = new ToggleGroup();
            HBox row = new HBox(14);
            for (int i = 0; i < AssessmentService.ANSWERS.length; i++) {
                RadioButton rb = new RadioButton(AssessmentService.ANSWERS[i]);
                rb.setUserData(i);
                rb.setToggleGroup(g);
                row.getChildren().add(rb);
            }
            groups.add(g);
            form.getChildren().addAll(ql, row);
        }
    }

    private void refresh(Services s, User user) {
        history.getItems().clear();
        for (AssessmentResult r : s.assessment.history(user.id())) {
            history.getItems().add(r.at().format(Ui.DATE_TIME) + "   " + Kind.valueOf(r.kind()).title
                    + "   score " + r.score() + "/6");
        }
    }

    @Override
    public Parent root() {
        return root;
    }
}
