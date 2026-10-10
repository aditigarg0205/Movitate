package com.wellness.ui;

import com.wellness.model.MoodEntry;
import com.wellness.model.User;
import com.wellness.service.Services;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

/** Month grid coloured by the day's average mood. Click a day to see its check-ins. */
public class CalendarView implements View {
    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final VBox root = new VBox(16);
    private final GridPane grid = new GridPane();
    private final Label monthTitle = new Label();
    private final VBox detail = new VBox(6);
    private final Button prev = new Button("◀");
    private final Button next = new Button("▶");
    private final Services s;
    private final User user;
    private YearMonth month = YearMonth.now();

    public CalendarView(Services s, User user) {
        this.s = s;
        this.user = user;
        root.setPadding(new Insets(26));

        monthTitle.getStyleClass().add("heading2");
        Button today = new Button("Today");
        prev.setOnAction(e -> {
            month = month.minusMonths(1);
            render();
        });
        next.setOnAction(e -> {
            month = month.plusMonths(1);
            render();
        });
        today.setOnAction(e -> {
            month = YearMonth.now();
            render();
            showDay(LocalDate.now());
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox nav = new HBox(10, prev, monthTitle, next, spacer, today);
        nav.setAlignment(Pos.CENTER_LEFT);

        grid.setHgap(6);
        grid.setVgap(6);

        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().add(Ui.muted("Legend:"));
        for (int m = 1; m <= 5; m++) {
            Label l = new Label(Ui.moodLabel(m));
            l.setStyle("-fx-background-color: " + Ui.moodColor(m) + "; -fx-padding: 3 8; -fx-background-radius: 8;");
            legend.getChildren().add(l);
        }

        detail.getStyleClass().add("card");
        root.getChildren().addAll(Ui.heading("Mood calendar"), Ui.card(nav, grid, legend), detail);
        render();
        showDay(LocalDate.now());
    }

    private void render() {
        grid.getChildren().clear();
        monthTitle.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear());
        next.setDisable(!month.isBefore(YearMonth.now()));

        for (int i = 0; i < 7; i++) {
            Label l = new Label(DAYS[i]);
            l.getStyleClass().add("muted");
            l.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHalignment(l, HPos.CENTER);
            grid.add(l, i, 0);
        }

        SortedMap<LocalDate, Double> data = s.mood.dailyAverages(user.id(), month);
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1; // Monday = 0
        LocalDate today = LocalDate.now();

        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);
            int col = (offset + d - 1) % 7;
            int row = 1 + (offset + d - 1) / 7;
            Double avg = data.get(date);

            Button b = new Button(avg == null ? String.valueOf(d)
                    : d + "\n" + Ui.moodEmoji((int) Math.max(1, Math.min(5, Math.round(avg)))));
            b.setTextAlignment(TextAlignment.CENTER);
            b.setPrefSize(88, 60);
            b.getStyleClass().add("day-cell");
            if (date.equals(today)) b.getStyleClass().add("today");
            if (avg != null) b.setStyle("-fx-background-color: " + Ui.moodColor(avg) + ";");
            b.setDisable(date.isAfter(today));
            b.setOnAction(e -> showDay(date));
            grid.add(b, col, row);
        }
    }

    private void showDay(LocalDate date) {
        detail.getChildren().clear();
        detail.getChildren().add(Ui.heading2(date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))));
        List<MoodEntry> entries = s.mood.forDay(user.id(), date);
        if (entries.isEmpty()) {
            detail.getChildren().add(Ui.muted("No check-ins on this day."));
            return;
        }
        for (MoodEntry e : entries) {
            String note = e.note().isBlank() ? "" : "  -  " + e.note().replace('\n', ' ');
            Label l = new Label(String.format("%02d:%02d   %s%s", e.at().getHour(), e.at().getMinute(),
                    Ui.moodLabel(e.mood()), note));
            l.setWrapText(true);
            detail.getChildren().add(l);
        }
    }

    @Override
    public Parent root() {
        return root;
    }
}
