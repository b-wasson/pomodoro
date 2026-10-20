package com.pomodoro;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * System-wide hotkeys (work even when the window isn't focused):
 *   Ctrl+Alt+P  toggle start/pause
 *   Ctrl+Alt+M  cycle window mode
 *   Ctrl+Alt+R  reset current phase
 */
public class GlobalHotkeys implements NativeKeyListener {

    private Runnable onToggle;
    private Runnable onCycleMode;
    private Runnable onReset;
    private boolean registered = false;

    public void setOnToggle(Runnable r) { this.onToggle = r; }
    public void setOnCycleMode(Runnable r) { this.onCycleMode = r; }
    public void setOnReset(Runnable r) { this.onReset = r; }

    public void start() {
        // jnativehook is very chatty by default; silence it.
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            registered = true;
        } catch (NativeHookException e) {
            System.err.println("Global hotkeys unavailable: " + e.getMessage());
        }
    }

    public void stop() {
        if (registered) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ignored) {
                // shutting down anyway
            }
            registered = false;
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        boolean ctrl = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        boolean alt = (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0;
        if (!(ctrl && alt)) {
            return;
        }
        switch (e.getKeyCode()) {
            case NativeKeyEvent.VC_P:
                if (onToggle != null) onToggle.run();
                break;
            case NativeKeyEvent.VC_M:
                if (onCycleMode != null) onCycleMode.run();
                break;
            case NativeKeyEvent.VC_R:
                if (onReset != null) onReset.run();
                break;
            default:
                break;
        }
    }
}
