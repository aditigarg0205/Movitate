package com.wellness.ui;

import com.wellness.model.User;
import com.wellness.service.GrowthService.Achievement;
import com.wellness.service.GrowthService.Level;
import com.wellness.service.Services;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** Personal growth: level, this week's small wins, and achievement badges. */
public class GrowthView implements View {
    private final VBox root = new VBox(16);

    public GrowthView(Services s, User user) {
        root.setPadding(new Insets(26));
        root.getChildren().addAll(Ui.heading("My growth"),
                Ui.muted("Small steps add up. Here's what you've built so far."));

        Level lv = s.growth.level(user.id());
        ProgressBar bar = new ProgressBar(lv.progress());
        bar.setPrefWidth(420);
        String next = lv.nextAt() == null ? "Top level reached!"
                : lv.points() + " / " + lv.nextAt() + " points to the next level";
        root.getChildren().add(Ui.card(Ui.heading2(levelEmoji(lv.name()) + " Level: " + lv.name()), bar, Ui.muted(next),
                Ui.muted("Points come from check-ins, journal entries, self-checks and streaks.")));

        VBox wins = Ui.card(Ui.heading2("This week's small wins"));
        for (String w : s.growth.weeklyWins(user.id())) {
            Label l = new Label("✓  " + w);
            l.setWrapText(true);
            wins.getChildren().add(l);
        }
        int streak = s.mood.streak(user.id());
        int longest = s.mood.longestStreak(user.id());
        wins.getChildren().add(Ui.muted("🔥 Current streak: " + streak + " day(s)   |   Longest: " + longest + " day(s)"));
        root.getChildren().add(wins);

        List<Achievement> all = s.growth.achievements(user.id());
        long unlocked = all.stream().filter(Achievement::unlocked).count();
        root.getChildren().add(Ui.heading2("Achievements (" + unlocked + " / " + all.size() + ")"));

        FlowPane flow = new FlowPane(14, 14);
        for (Achievement a : all) flow.getChildren().add(card(a));
        root.getChildren().add(flow);
    }

    private VBox card(Achievement a) {
        Label emoji = new Label(a.emoji());
        emoji.getStyleClass().add("badge-emoji");
        Label title = Ui.heading2(a.title());
        HBox head = new HBox(10, emoji, title);
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label desc = Ui.muted(a.description());
        ProgressBar bar = new ProgressBar(a.progress());
        bar.setMaxWidth(Double.MAX_VALUE);
        Label count = new Label(a.unlocked() ? "Unlocked ✓" : Math.min(a.current(), a.target()) + " / " + a.target());
        count.getStyleClass().add(a.unlocked() ? "success" : "muted");

        VBox box = Ui.card(head, desc, bar, count);
        box.setPrefWidth(260);
        box.setMinWidth(260);
        box.getStyleClass().add(a.unlocked() ? "badge-unlocked" : "badge-locked");
        return box;
    }

    private static String levelEmoji(String name) {
        return switch (name) {
            case "Seedling" -> "🌱";
            case "Sprout" -> "🌿";
            case "Blooming" -> "🌸";
            case "Flourishing" -> "🌳";
            default -> "🌟";
        };
    }

    @Override
    public Parent root() {
        return root;
    }
}
