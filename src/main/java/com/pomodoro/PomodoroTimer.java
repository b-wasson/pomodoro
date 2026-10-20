package com.pomodoro;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;

/**
 * The Pomodoro state machine: focus -> short break (x N) -> long break -> repeat.
 * Ticks once per second on the JavaFX application thread.
 */
public class PomodoroTimer {

    public enum Phase {
        FOCUS("Focus"),
        SHORT_BREAK("Short Break"),
        LONG_BREAK("Long Break");

        public final String label;
        Phase(String label) { this.label = label; }
    }

    private final Settings settings;
    private final Timeline ticker;

    private final ObjectProperty<Phase> phase = new SimpleObjectProperty<>(Phase.FOCUS);
    private final IntegerProperty remainingSeconds = new SimpleIntegerProperty(0);
    private final IntegerProperty totalSeconds = new SimpleIntegerProperty(0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final IntegerProperty completedFocusSessions = new SimpleIntegerProperty(0);

    private Runnable onPhaseComplete;

    public PomodoroTimer(Settings settings) {
        this.settings = settings;
        this.ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        this.ticker.setCycleCount(Animation.INDEFINITE);
        setPhase(Phase.FOCUS, false);
    }

    public void setOnPhaseComplete(Runnable r) { this.onPhaseComplete = r; }

    private void tick() {
        if (remainingSeconds.get() > 0) {
            remainingSeconds.set(remainingSeconds.get() - 1);
        }
        if (remainingSeconds.get() <= 0) {
            complete();
        }
    }

    private void complete() {
        ticker.stop();
        running.set(false);
        if (phase.get() == Phase.FOCUS) {
            completedFocusSessions.set(completedFocusSessions.get() + 1);
        }
        if (onPhaseComplete != null) {
            onPhaseComplete.run();
        }
    }

    /** Move to the phase that should follow the one that just finished. */
    public void advanceToNextPhase(boolean autoStart) {
        Phase next;
        if (phase.get() == Phase.FOCUS) {
            int interval = Math.max(1, settings.getLongBreakInterval());
            next = (completedFocusSessions.get() % interval == 0) ? Phase.LONG_BREAK : Phase.SHORT_BREAK;
        } else {
            next = Phase.FOCUS;
        }
        setPhase(next, autoStart);
    }

    public void setPhase(Phase p, boolean autoStart) {
        ticker.stop();
        running.set(false);
        phase.set(p);
        int secs = durationFor(p) * 60;
        totalSeconds.set(secs);
        remainingSeconds.set(secs);
        if (autoStart) {
            start();
        }
    }

    private int durationFor(Phase p) {
        switch (p) {
            case FOCUS: return Math.max(1, settings.getFocusMinutes());
            case SHORT_BREAK: return Math.max(1, settings.getShortBreakMinutes());
            case LONG_BREAK: return Math.max(1, settings.getLongBreakMinutes());
            default: return 25;
        }
    }

    public void start() {
        if (remainingSeconds.get() <= 0) {
            remainingSeconds.set(totalSeconds.get());
        }
        ticker.playFromStart();
        running.set(true);
    }

    public void pause() {
        ticker.stop();
        running.set(false);
    }

    public void toggle() {
        if (running.get()) {
            pause();
        } else {
            start();
        }
    }

    /** Reload the current phase from settings (also applies edited durations). */
    public void reset() {
        setPhase(phase.get(), false);
    }

    /** Skip the current phase and move on without ringing the alarm. */
    public void skip() {
        ticker.stop();
        running.set(false);
        if (phase.get() == Phase.FOCUS) {
            completedFocusSessions.set(completedFocusSessions.get() + 1);
        }
        advanceToNextPhase(false);
    }

    public ObjectProperty<Phase> phaseProperty() { return phase; }
    public Phase getPhase() { return phase.get(); }

    public IntegerProperty remainingSecondsProperty() { return remainingSeconds; }
    public int getRemainingSeconds() { return remainingSeconds.get(); }

    public IntegerProperty totalSecondsProperty() { return totalSeconds; }
    public int getTotalSeconds() { return totalSeconds.get(); }

    public BooleanProperty runningProperty() { return running; }
    public boolean isRunning() { return running.get(); }

    public IntegerProperty completedFocusSessionsProperty() { return completedFocusSessions; }
    public int getCompletedFocusSessions() { return completedFocusSessions.get(); }
}
