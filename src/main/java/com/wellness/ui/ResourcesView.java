package com.wellness.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ResourcesView implements View {
    private final VBox root = new VBox(16);

    public ResourcesView() {
        root.setPadding(new Insets(26));

        Label urgent = new Label("If you are in immediate danger or may act on thoughts of harming yourself, "
                + "call your local emergency number now (112 in India) or go to the nearest hospital. "
                + "If you can, tell someone near you.");
        urgent.setWrapText(true);
        VBox urgentCard = Ui.card(Ui.heading2("In an emergency"), urgent);
        urgentCard.getStyleClass().add("alert-card");

        root.getChildren().addAll(
                Ui.heading("Get help"),
                urgentCard,
                Ui.card(Ui.heading2("Helplines (India)"),
                        line("Tele-MANAS (Government of India, free, 24x7): 14416"),
                        line("KIRAN Mental Health Rehabilitation Helpline (free, 24x7): 1800-599-0019"),
                        line("Emergency services: 112"),
                        Ui.muted("Numbers can change - please confirm them on the official websites. "
                                + "If you live elsewhere, search for your country's mental health or crisis line.")),
                Ui.card(Ui.heading2("Small things that can help right now"),
                        line("• Breathe slowly - use the Breathe screen."),
                        line("• Message or call someone you trust, even just to say hi."),
                        line("• Have water or a snack, and step outside for fresh air."),
                        line("• Write what's on your mind in your journal."),
                        line("• Book an appointment with a doctor or counsellor if low mood or worry lasts "
                                + "for more than two weeks.")));
    }

    private Label line(String t) {
        Label l = new Label(t);
        l.setWrapText(true);
        return l;
    }

    @Override
    public Parent root() {
        return root;
    }
}
