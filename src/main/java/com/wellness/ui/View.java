package com.wellness.ui;

import javafx.scene.Parent;

public interface View {
    Parent root();

    /** Called when the user leaves this screen (stop animations, etc.). */
    default void dispose() {}
}
