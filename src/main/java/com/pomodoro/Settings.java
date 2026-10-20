package com.pomodoro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * User-editable settings, persisted to ~/.pomodoro/settings.properties.
 */
public class Settings {

    private int focusMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int longBreakInterval = 4;
    private boolean autoStartNext = true;
    private boolean darkMode = false;

    private static Path file() {
        return Paths.get(System.getProperty("user.home"), ".pomodoro", "settings.properties");
    }

    public static Settings load() {
        Settings s = new Settings();
        Path p = file();
        try {
            if (Files.exists(p)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(p)) {
                    props.load(in);
                }
                s.focusMinutes = parseInt(props, "focusMinutes", s.focusMinutes);
                s.shortBreakMinutes = parseInt(props, "shortBreakMinutes", s.shortBreakMinutes);
                s.longBreakMinutes = parseInt(props, "longBreakMinutes", s.longBreakMinutes);
                s.longBreakInterval = parseInt(props, "longBreakInterval", s.longBreakInterval);
                s.autoStartNext = parseBool(props, "autoStartNext", s.autoStartNext);
                s.darkMode = parseBool(props, "darkMode", s.darkMode);
            }
        } catch (IOException ignored) {
            // fall back to defaults
        }
        return s;
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("focusMinutes", Integer.toString(focusMinutes));
        props.setProperty("shortBreakMinutes", Integer.toString(shortBreakMinutes));
        props.setProperty("longBreakMinutes", Integer.toString(longBreakMinutes));
        props.setProperty("longBreakInterval", Integer.toString(longBreakInterval));
        props.setProperty("autoStartNext", Boolean.toString(autoStartNext));
        props.setProperty("darkMode", Boolean.toString(darkMode));
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            try (OutputStream out = Files.newOutputStream(p)) {
                props.store(out, "Pomodoro settings");
            }
        } catch (IOException ignored) {
            // best effort
        }
    }

    private static int parseInt(Properties props, String key, int def) {
        try {
            return Integer.parseInt(props.getProperty(key, Integer.toString(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean parseBool(Properties props, String key, boolean def) {
        String v = props.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    public int getFocusMinutes() { return focusMinutes; }
    public void setFocusMinutes(int v) { this.focusMinutes = v; }

    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public void setShortBreakMinutes(int v) { this.shortBreakMinutes = v; }

    public int getLongBreakMinutes() { return longBreakMinutes; }
    public void setLongBreakMinutes(int v) { this.longBreakMinutes = v; }

    public int getLongBreakInterval() { return longBreakInterval; }
    public void setLongBreakInterval(int v) { this.longBreakInterval = v; }

    public boolean isAutoStartNext() { return autoStartNext; }
    public void setAutoStartNext(boolean v) { this.autoStartNext = v; }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean v) { this.darkMode = v; }
}
