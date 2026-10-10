package com.wellness.ui;

import com.wellness.model.User;
import com.wellness.service.Services;
import com.wellness.service.WellnessService.Journey;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Optional;
import java.util.SortedMap;

/** How your mood has moved over time, with simple insights. */
public class JourneyView implements View {
    private static final String[] RANGES = {"Last 7 days", "Last 30 days", "Last 90 days"};
    private static final int[] DAYS = {7, 30, 90};

    private final VBox root = new VBox(16);
    private final VBox content = new VBox(16);

    public JourneyView(Services s, User user) {
        root.setPadding(new Insets(26));

        ComboBox<String> range = new ComboBox<>();
        range.getItems().addAll(RANGES);
        range.getSelectionModel().select(1);
        range.setOnAction(e -> build(s, user, DAYS[Math.max(0, range.getSelectionModel().getSelectedIndex())]));

        root.getChildren().addAll(Ui.heading("Mood journey"),
                Ui.muted("See how your mood has moved over time and what stands out."), range, content);
        build(s, user, 30);
    }

    private void build(Services s, User user, int days) {
        content.getChildren().clear();
        Optional<Journey> opt = s.wellness.journey(user.id(), days);
        if (opt.isEmpty()) {
            content.getChildren().add(Ui.card(Ui.muted("No check-ins in this period yet. Your journey starts "
                    + "with your first mood check-in.")));
            return;
        }
        Journey j = opt.get();

        SortedMap<LocalDate, Double> daily = s.mood.dailyAverages(user.id(), days);
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(1, 5, 1);
        y.setMinorTickVisible(false);
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        daily.forEach((d, v) -> series.getData().add(new XYChart.Data<>(d.format(Ui.DAY), v)));
        chart.getData().add(series);

        String trendIcon = switch (j.trend()) {
            case "Improving" -> "📈 ";
            case "Dipping lately" -> "📉 ";
            case "Steady" -> "➖ ";
            default -> "";
        };

        VBox insights = Ui.card(Ui.heading2("Insights"),
                line("Check-ins: " + j.checkIns() + " across " + j.daysWithData() + " day(s)"),
                line(String.format("Average mood: %.1f / 5", j.average())),
                line("Most common mood: " + Ui.moodLabel(j.mostCommonMood())),
                line(String.format("Brightest day: %s (%.1f)", j.bestDay().format(Ui.DAY), j.bestAverage())),
                line(String.format("Toughest day: %s (%.1f)", j.toughestDay().format(Ui.DAY), j.toughestAverage())),
                line("Trend: " + trendIcon + j.trend()),
                line("Journey started: " + j.firstCheckIn().format(Ui.DAY) + " " + j.firstCheckIn().getYear()));

        content.getChildren().addAll(Ui.card(Ui.heading2("Mood over time"), chart), insights);
    }

    private Label line(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        return l;
    }

    @Override
    public Parent root() {
        return root;
    }
}
