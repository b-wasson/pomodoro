package com.pomodoro;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Restores edge / corner resizing on an undecorated (borderless) Stage, which
 * normally loses the native resize grips. Install once on the scene; it watches
 * the mouse near the window edges and resizes the stage, respecting its
 * min width/height.
 */
public class WindowResizer {

    private static final int BORDER = 6;

    private final Stage stage;
    private final Scene scene;

    private boolean rLeft, rRight, rTop, rBottom;
    private double startScreenX, startScreenY;
    private double startW, startH, startX, startY;

    private WindowResizer(Stage stage, Scene scene) {
        this.stage = stage;
        this.scene = scene;
    }

    public static void install(Stage stage, Scene scene) {
        WindowResizer r = new WindowResizer(stage, scene);
        scene.addEventHandler(MouseEvent.MOUSE_MOVED, r::onMoved);
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, r::onPressed);
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, r::onDragged);
    }

    /** True while the pointer sits over a resize edge (so dragging-to-move can defer). */
    public boolean isOnEdge() {
        return scene.getCursor() != null && scene.getCursor() != Cursor.DEFAULT;
    }

    private void onMoved(MouseEvent e) {
        if (stage.isFullScreen()) {
            scene.setCursor(Cursor.DEFAULT);
            return;
        }
        double x = e.getSceneX();
        double y = e.getSceneY();
        double w = scene.getWidth();
        double h = scene.getHeight();

        boolean left = x < BORDER;
        boolean right = x > w - BORDER;
        boolean top = y < BORDER;
        boolean bottom = y > h - BORDER;

        if (left && top) scene.setCursor(Cursor.NW_RESIZE);
        else if (right && top) scene.setCursor(Cursor.NE_RESIZE);
        else if (left && bottom) scene.setCursor(Cursor.SW_RESIZE);
        else if (right && bottom) scene.setCursor(Cursor.SE_RESIZE);
        else if (left) scene.setCursor(Cursor.W_RESIZE);
        else if (right) scene.setCursor(Cursor.E_RESIZE);
        else if (top) scene.setCursor(Cursor.N_RESIZE);
        else if (bottom) scene.setCursor(Cursor.S_RESIZE);
        else scene.setCursor(Cursor.DEFAULT);
    }

    private void onPressed(MouseEvent e) {
        if (stage.isFullScreen()) {
            return;
        }
        Cursor c = scene.getCursor();
        rLeft = c == Cursor.W_RESIZE || c == Cursor.NW_RESIZE || c == Cursor.SW_RESIZE;
        rRight = c == Cursor.E_RESIZE || c == Cursor.NE_RESIZE || c == Cursor.SE_RESIZE;
        rTop = c == Cursor.N_RESIZE || c == Cursor.NW_RESIZE || c == Cursor.NE_RESIZE;
        rBottom = c == Cursor.S_RESIZE || c == Cursor.SW_RESIZE || c == Cursor.SE_RESIZE;

        startScreenX = e.getScreenX();
        startScreenY = e.getScreenY();
        startW = stage.getWidth();
        startH = stage.getHeight();
        startX = stage.getX();
        startY = stage.getY();
    }

    private void onDragged(MouseEvent e) {
        if (stage.isFullScreen() || !(rLeft || rRight || rTop || rBottom)) {
            return;
        }
        double dx = e.getScreenX() - startScreenX;
        double dy = e.getScreenY() - startScreenY;
        double minW = Math.max(1, stage.getMinWidth());
        double minH = Math.max(1, stage.getMinHeight());

        if (rRight) {
            stage.setWidth(Math.max(minW, startW + dx));
        }
        if (rBottom) {
            stage.setHeight(Math.max(minH, startH + dy));
        }
        if (rLeft) {
            double newW = startW - dx;
            if (newW >= minW) {
                stage.setX(startX + dx);
                stage.setWidth(newW);
            }
        }
        if (rTop) {
            double newH = startH - dy;
            if (newH >= minH) {
                stage.setY(startY + dy);
                stage.setHeight(newH);
            }
        }
    }
}
