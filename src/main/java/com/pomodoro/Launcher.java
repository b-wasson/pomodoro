package com.pomodoro;

/**
 * Plain launcher that does NOT extend javafx.application.Application.
 * Launching the app through this class lets a shaded ("fat") jar run JavaFX
 * from the classpath without the "JavaFX runtime components are missing" error.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
