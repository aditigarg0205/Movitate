package com.wellness.ui;

import com.wellness.service.AdminService.Overview;
import com.wellness.service.Services;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Anonymous, aggregate statistics only. */
public class AdminOverviewView implements View {
    private final VBox root = new VBox(16);
    private final java.util.List<SunFace> suns = new java.util.ArrayList<>();

    public AdminOverviewView(Services s) {
        root.setPadding(new Insets(26));
        Overview o = s.admin.overview();

        FlowPane stats = new FlowPane(14, 14);
        stats.getChildren().addAll(
                stat("Registered users", String.valueOf(o.users())),
                stat("Active in last 7 days", String.valueOf(o.activeLast7Days())),
                stat("Disabled accounts", String.valueOf(o.disabledUsers())),
                stat("Total mood check-ins", String.valueOf(o.totalCheckIns())),
                stat("Average mood (30 days)", o.averageMood30().isPresent()
                        ? String.format("%.1f / 5", o.averageMood30().getAsDouble()) : "-"),
                stat("Self-checks at/above cut-off", o.selfChecksAtOrAboveCutoff() + " of " + o.selfChecks()));

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        y.setTickUnit(1);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(280);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int m = 1; m <= 5; m++)
            series.getData().add(new XYChart.Data<>(Ui.moodLabel(m), o.moodDistribution30()[m - 1]));
        chart.getData().add(series);

        Label privacy = Ui.muted("Privacy: this portal shows anonymous totals and account details only. "
                + "Journal entries and mood notes are never visible to administrators.");

        root.getChildren().addAll(Ui.heading("Overview"), stats, moodSuns(o.moodDistribution30()),
                Ui.card(Ui.heading2("How users have felt (last 30 days, all check-ins)"), chart),
                Ui.card(privacy));
    }

    /** Happy / calm / sad suns summarising how everyone felt in the last 30 days (anonymous). */
    private VBox moodSuns(int[] d) {
        int happy = d[3] + d[4], calm = d[2], sad = d[0] + d[1];
        int total = Math.max(1, happy + calm + sad);
        HBox row = new HBox(30,
                sunTile(SunFace.Mood.HAPPY, "Happy", "Good or great", happy, total),
                sunTile(SunFace.Mood.CALM, "Calm & meditative", "Feeling okay", calm, total),
                sunTile(SunFace.Mood.SAD, "Sad", "Low or very low", sad, total));
        row.setAlignment(Pos.CENTER);
        return Ui.card(Ui.heading2("Mood suns (last 30 days)"), row);
    }

    private VBox sunTile(SunFace.Mood mood, String title, String hint, int count, int total) {
        SunFace sun = new SunFace(mood, 38);
        suns.add(sun);
        StackPane holder = new StackPane(sun);
        holder.setPrefSize(150, 130);
        holder.setMinSize(150, 130);
        Label t = new Label(title);
        t.getStyleClass().add("heading2");
        Label v = new Label(count + " check-ins  ·  " + Math.round(100.0 * count / total) + "%");
        v.getStyleClass().add("stat-value");
        VBox box = new VBox(6, holder, t, v, Ui.muted(hint));
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(200);
        return box;
    }

    private VBox stat(String title, String value) {
        Label t = new Label(title);
        t.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        VBox b = Ui.card(t, v);
        b.setPrefWidth(230);
        return b;
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void dispose() {
        suns.forEach(SunFace::stop);
    }
}
