package com.wellness.ui;

import com.wellness.model.MoodEntry;
import com.wellness.model.User;
import com.wellness.service.Services;
import com.wellness.service.WellnessService.DailyScore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public class DashboardView implements View {
    private static final String[] TIPS = {
            "Drink a glass of water and stretch your shoulders. Small resets count.",
            "Name three things you can see, two you can hear, one you can feel. It grounds you in the present.",
            "A 10-minute walk outside can lift your mood more than you'd expect.",
            "You don't have to fix everything today. Pick one small, kind thing for yourself.",
            "Try to go to bed and wake up at similar times. Sleep rhythm shapes mood.",
            "Talking to one trusted person can make a heavy day feel lighter.",
            "Write down one thing that went okay today, however small.",
            "Slow breathing, with a longer out-breath than in-breath, helps calm the body."
    };

    private final VBox root = new VBox(18);

    public DashboardView(Services s, User user) {
        root.setPadding(new Insets(26));

        int h = LocalTime.now().getHour();
        String greet = h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
        root.getChildren().add(Ui.heading(greet + ", " + user.displayName()));

        DailyScore ds = s.wellness.dailyScore(user.id());
        Optional<MoodEntry> latest = s.mood.latest(user.id());
        OptionalDouble avg = s.mood.average(user.id(), 7);
        int streak = s.mood.streak(user.id());
        int longest = s.mood.longestStreak(user.id());

        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.setVgap(14);
        stats.add(stat("Latest mood", latest.map(m -> Ui.moodLabel(m.mood())).orElse("No check-ins yet")), 0, 0);
        stats.add(stat("7-day average", avg.isPresent() ? String.format("%.1f / 5", avg.getAsDouble()) : "-"), 1, 0);
        stats.add(stat("🔥 Wellness streak", streak + (streak == 1 ? " day" : " days")
                + "   (best: " + longest + ")"), 0, 1);
        stats.add(stat("Journal entries", String.valueOf(s.journal.count(user.id()))), 1, 1);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        stats.getColumnConstraints().addAll(c1, c2);

        HBox top = new HBox(14, scoreCard(ds), stats);
        HBox.setHgrow(stats, Priority.ALWAYS);
        root.getChildren().add(top);

        root.getChildren().add(Ui.card(Ui.heading2("Mood - last 14 days"), chart(s, user)));

        Label tip = new Label(TIPS[LocalDate.now().getDayOfYear() % TIPS.length]);
        tip.setWrapText(true);
        root.getChildren().add(Ui.card(Ui.heading2("Today's tip"), tip));
    }

    private VBox scoreCard(DailyScore ds) {
        Circle track = new Circle(58);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#d7e8e5"));
        track.setStrokeWidth(12);

        StackPane ring = new StackPane(track);
        if (ds.available()) {
            Arc arc = new Arc(0, 0, 58, 58, 90, -360.0 * ds.score() / 100.0);
            arc.setType(ArcType.OPEN);
            arc.setFill(Color.TRANSPARENT);
            arc.setStroke(Color.web("#4a9d94"));
            arc.setStrokeWidth(12);
            arc.setStrokeLineCap(StrokeLineCap.ROUND);
            ring.getChildren().add(arc);
        }
        Label number = new Label(ds.available() ? String.valueOf(ds.score()) : "-");
        number.getStyleClass().add("score-number");
        ring.getChildren().add(number);
        ring.setMinSize(140, 140);

        Label title = Ui.heading2("Daily wellness score");
        Label label = new Label(ds.label());
        label.getStyleClass().add("stat-value");
        Label msg = Ui.muted(ds.message());
        msg.setMaxWidth(230);

        VBox box = Ui.card(title, ring, label, msg);
        if (ds.available()) {
            Label how = Ui.muted("Mood " + ds.moodPoints() + " + check-in " + ds.checkInPoints()
                    + " + journal " + ds.journalPoints() + " + streak " + ds.streakPoints());
            how.setMaxWidth(230);
            box.getChildren().add(how);
        }
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(270);
        box.setMinWidth(270);
        return box;
    }

    private VBox stat(String title, String value) {
        Label t = new Label(title);
        t.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        v.setWrapText(true);
        VBox b = Ui.card(t, v);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    private Parent chart(Services s, User user) {
        Map<LocalDate, Double> data = s.mood.dailyAverages(user.id(), 14);
        if (data.isEmpty()) return Ui.muted("Your mood chart will appear after your first check-in.");

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(1, 5, 1);
        y.setMinorTickVisible(false);
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((d, v) -> series.getData().add(new XYChart.Data<>(d.format(Ui.DAY), v)));
        chart.getData().add(series);
        return chart;
    }

    @Override
    public Parent root() {
        return root;
    }
}
