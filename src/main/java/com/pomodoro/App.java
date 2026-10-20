package com.pomodoro;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.EventTarget;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import com.pomodoro.PomodoroTimer.Phase;

public class App extends Application {

    public enum WindowMode { NORMAL, COMPACT, FULLSCREEN }

    private static final double DIAL_SIZE = 260;
    private static final double DIAL_RADIUS = 110;

    // Vector icon paths (~16px box). SVG renders reliably; emoji do not, on Windows.
    private static final String IC_COMPACT  = "M2,5 H9 V13 H2 Z M10.5,2.5 H13.5 V5.5 M13.5,2.5 L8,8";
    private static final String IC_WINDOW   = "M2,3 H14 V13 H2 Z M2,6 H14";
    private static final String IC_FULL     = "M2,5 V2 H5 M11,2 H14 V5 M14,11 V14 H11 M5,14 H2 V11";
    private static final String IC_MOON     = "M13,10.8 A5.5,5.5 0 1 1 7.2,2.6 A4.2,4.2 0 1 0 13,10.8 Z";
    private static final String IC_SUN      = "M8,5.5 A2.5,2.5 0 1 1 7.99,5.5 Z M8,1 V2.6 M8,13.4 V15 "
            + "M1,8 H2.6 M13.4,8 H15 M3.4,3.4 L4.5,4.5 M12.6,3.4 L11.5,4.5 M3.4,12.6 L4.5,11.5 M12.6,12.6 L11.5,11.5";
    private static final String IC_SETTINGS = "M2,4 H14 M2,8 H14 M2,12 H14 "
            + "M10,2.4 A1.6,1.6 0 1 1 9.99,2.4 Z M5,6.4 A1.6,1.6 0 1 1 4.99,6.4 Z M11,10.4 A1.6,1.6 0 1 1 10.99,10.4 Z";
    private static final String IC_MIN      = "M1,7 H13";
    private static final String IC_CLOSE    = "M1,1 L13,13 M1,13 L13,1";
    private static final String IC_RESTORE  = "M5,2 V5 H2 M11,2 V5 H14 M2,11 H5 V14 M14,11 H11 V14";

    private Settings settings;
    private PomodoroTimer timer;
    private GlobalHotkeys hotkeys;

    private Stage stage;
    private Scene scene;
    private final double[] dragOffset = new double[2];
    private StackPane rootStack;
    private BorderPane mainPane;

    private Arc progressArc;
    private Label timeLabel;
    private Label phaseLabel;
    private Label sessionLabel;
    private Label wordmark;
    private Button startBtn;

    private StackPane compactView;
    private Label compactTime;

    private Button compactBtn;
    private Button normalBtn;
    private Button fullBtn;
    private Button themeBtn;
    private SVGPath themeGlyph;

    private StackPane settingsOverlay;
    private Spinner<Integer> focusSpin;
    private Spinner<Integer> shortSpin;
    private Spinner<Integer> longSpin;
    private Spinner<Integer> intervalSpin;
    private CheckBox autoStartChk;

    private StackPane takeoverOverlay;
    private Label takeoverTitle;
    private Label takeoverSub;
    private VBox takeoverBox;
    private Region takeoverFlash;
    private ParallelTransition pulse;

    private WindowMode currentMode = WindowMode.NORMAL;
    private WindowMode modeBeforeTakeover = WindowMode.NORMAL;
    private boolean inTakeover = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        // Borderless: no native Windows title bar / chrome.
        stage.initStyle(StageStyle.UNDECORATED);

        settings = Settings.load();
        timer = new PomodoroTimer(settings);

        buildUI();
        timer.setOnPhaseComplete(this::onTimerComplete);

        scene = new Scene(rootStack, 420, 580);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Pomodoro");
        loadAppIcons();
        stage.setMinWidth(300);
        stage.setMinHeight(360);
        stage.setOnCloseRequest(e -> cleanup());

        // Restore edge-resizing that an undecorated window otherwise loses.
        WindowResizer.install(stage, scene);

        // Collapse the wordmark on narrow widths so the icons keep their room.
        scene.widthProperty().addListener((o, a, b) -> updateWordmark(b.doubleValue()));
        updateWordmark(scene.getWidth());

        bindTimer();
        applyTheme(settings.isDarkMode());
        applyMode(WindowMode.NORMAL);

        hotkeys = new GlobalHotkeys();
        hotkeys.setOnToggle(() -> Platform.runLater(this::handleStartPause));
        hotkeys.setOnCycleMode(() -> Platform.runLater(this::cycleMode));
        hotkeys.setOnReset(() -> Platform.runLater(() -> timer.reset()));
        hotkeys.start();

        stage.show();
    }

    @Override
    public void stop() {
        cleanup();
    }

    private void cleanup() {
        if (hotkeys != null) hotkeys.stop();
    }

    /** Custom taskbar / window icon (several sizes so Windows picks the sharpest). */
    private void loadAppIcons() {
        for (String size : new String[] {"16", "24", "32", "48", "64", "128", "256"}) {
            var url = getClass().getResource("/icons/icon-" + size + ".png");
            if (url != null) {
                stage.getIcons().add(new Image(url.toExternalForm()));
            }
        }
    }

    // ----------------------------------------------------------------- UI build

    private void buildUI() {
        mainPane = new BorderPane();
        mainPane.getStyleClass().add("main-pane");
        mainPane.setTop(buildTopBar());
        mainPane.setCenter(buildDial());
        mainPane.setBottom(buildBottomBar());

        settingsOverlay = buildSettingsOverlay();
        settingsOverlay.setVisible(false);

        takeoverOverlay = buildTakeoverOverlay();
        takeoverOverlay.setVisible(false);

        compactView = buildCompactView();
        compactView.setVisible(false);

        rootStack = new StackPane(mainPane, compactView, settingsOverlay, takeoverOverlay);
        rootStack.getStyleClass().add("root-stack");
    }

    private HBox buildTopBar() {
        Label title = new Label("pomodoro");
        title.getStyleClass().add("title");
        title.setMinWidth(0);
        // Hidden entirely when the window is narrow (see updateWordmark) so the
        // window can shrink further without ever clipping the icon buttons.
        wordmark = title;

        // All controls are crisp SVG icon buttons (emoji can't render on Windows).
        compactBtn = iconButton(IC_COMPACT, "Compact pop-out (always on top)");
        compactBtn.setOnAction(e -> applyMode(WindowMode.COMPACT));

        normalBtn = iconButton(IC_WINDOW, "Normal window");
        normalBtn.setOnAction(e -> applyMode(WindowMode.NORMAL));

        fullBtn = iconButton(IC_FULL, "Fullscreen on this monitor");
        fullBtn.setOnAction(e -> applyMode(WindowMode.FULLSCREEN));

        HBox modeBar = new HBox(2, compactBtn, normalBtn, fullBtn);
        modeBar.setAlignment(Pos.CENTER_RIGHT);

        themeBtn = iconButton(IC_MOON, "Toggle light / dark mode");
        themeGlyph = (SVGPath) themeBtn.getGraphic();
        themeBtn.setOnAction(e -> toggleTheme());

        Button gear = iconButton(IC_SETTINGS, "Settings");
        gear.setOnAction(e -> toggleSettings());

        Button minBtn = iconButton(IC_MIN, "Minimize");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = iconButton(IC_CLOSE, "Close", "win-close");
        closeBtn.setOnAction(e -> {
            cleanup();
            Platform.exit();
        });

        HBox winControls = new HBox(2, minBtn, closeBtn);
        winControls.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(6, title, spacer, modeBar, themeBtn, gear, winControls);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");
        enableDrag(top);
        return top;
    }

    private StackPane buildDial() {
        Arc trackArc = new Arc(DIAL_SIZE / 2, DIAL_SIZE / 2, DIAL_RADIUS, DIAL_RADIUS, 0, 360);
        trackArc.setType(ArcType.OPEN);
        trackArc.setFill(Color.TRANSPARENT);
        trackArc.setStrokeWidth(14);
        trackArc.setStrokeLineCap(StrokeLineCap.ROUND);
        trackArc.getStyleClass().add("track-arc");

        progressArc = new Arc(DIAL_SIZE / 2, DIAL_SIZE / 2, DIAL_RADIUS, DIAL_RADIUS, 90, -360);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStrokeWidth(14);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);
        progressArc.getStyleClass().add("progress-arc");

        javafx.scene.layout.Pane arcPane = new javafx.scene.layout.Pane(trackArc, progressArc);
        arcPane.setPrefSize(DIAL_SIZE, DIAL_SIZE);
        arcPane.setMaxSize(DIAL_SIZE, DIAL_SIZE);
        arcPane.setMinSize(DIAL_SIZE, DIAL_SIZE);

        phaseLabel = new Label("Focus");
        phaseLabel.getStyleClass().add("phase-label");

        timeLabel = new Label("25:00");
        timeLabel.getStyleClass().add("time-label");

        VBox center = new VBox(2, phaseLabel, timeLabel);
        center.setAlignment(Pos.CENTER);

        StackPane dial = new StackPane(arcPane, center);
        dial.setPrefSize(DIAL_SIZE, DIAL_SIZE);
        dial.getStyleClass().add("dial");

        StackPane wrapper = new StackPane(dial);
        wrapper.getStyleClass().add("dial-wrapper");
        return wrapper;
    }

    /**
     * Compact mode: nothing but the timer (which scales with the window) and a
     * small button in the corner to return to the normal window.
     */
    private StackPane buildCompactView() {
        compactTime = new Label("25:00");
        compactTime.getStyleClass().add("compact-time");

        Button exit = iconButton(IC_RESTORE, "Exit compact mode", "compact-exit");
        exit.setOnAction(e -> applyMode(WindowMode.NORMAL));
        StackPane.setAlignment(exit, Pos.TOP_RIGHT);
        StackPane.setMargin(exit, new Insets(3, 5, 0, 0));

        StackPane view = new StackPane(compactTime, exit);
        view.getStyleClass().add("compact-view");
        enableDrag(view);

        // Scale the timer text to the window height so it stays large and
        // readable at any size the user drags the window to.
        compactTime.styleProperty().bind(Bindings.concat(
                "-fx-font-size: ",
                view.heightProperty().multiply(0.55).asString(java.util.Locale.US, "%.0f"),
                "px;"));
        return view;
    }

    private VBox buildBottomBar() {
        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("button");
        resetBtn.setOnAction(e -> timer.reset());

        startBtn = new Button("Start");
        startBtn.getStyleClass().addAll("button", "primary");
        startBtn.setOnAction(e -> handleStartPause());

        Button skipBtn = new Button("Skip");
        skipBtn.getStyleClass().add("button");
        skipBtn.setOnAction(e -> timer.skip());

        HBox controls = new HBox(10, resetBtn, startBtn, skipBtn);
        controls.setAlignment(Pos.CENTER);

        sessionLabel = new Label();
        sessionLabel.getStyleClass().add("session-label");

        VBox bottom = new VBox(12, controls, sessionLabel);
        bottom.setAlignment(Pos.CENTER);
        bottom.getStyleClass().add("bottom-bar");
        return bottom;
    }

    private StackPane buildSettingsOverlay() {
        focusSpin = intSpinner(1, 180, settings.getFocusMinutes());
        shortSpin = intSpinner(1, 60, settings.getShortBreakMinutes());
        longSpin = intSpinner(1, 120, settings.getLongBreakMinutes());
        intervalSpin = intSpinner(1, 12, settings.getLongBreakInterval());

        autoStartChk = new CheckBox("Auto-start next session");
        autoStartChk.setSelected(settings.isAutoStartNext());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, settingsLabel("Focus (min)"), focusSpin);
        grid.addRow(1, settingsLabel("Short break (min)"), shortSpin);
        grid.addRow(2, settingsLabel("Long break (min)"), longSpin);
        grid.addRow(3, settingsLabel("Long break every"), intervalSpin);

        Button save = new Button("Save");
        save.getStyleClass().addAll("button", "primary");
        save.setOnAction(e -> saveSettings());
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("button");
        cancel.setOnAction(e -> settingsOverlay.setVisible(false));
        HBox actions = new HBox(10, save, cancel);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label heading = new Label("Settings");
        heading.getStyleClass().add("card-heading");

        VBox card = new VBox(16, heading, grid, autoStartChk, actions);
        card.getStyleClass().add("card");
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("backdrop");
        overlay.setPadding(new Insets(24));
        return overlay;
    }

    private StackPane buildTakeoverOverlay() {
        takeoverFlash = new Region();
        takeoverFlash.getStyleClass().add("takeover-flash");
        takeoverFlash.setOpacity(0);
        takeoverFlash.setMouseTransparent(true);

        takeoverTitle = new Label("Time's up");
        takeoverTitle.getStyleClass().add("takeover-title");
        takeoverSub = new Label("");
        takeoverSub.getStyleClass().add("takeover-sub");

        Button dismiss = new Button("Continue");
        dismiss.getStyleClass().addAll("button", "primary", "takeover-button");
        dismiss.setOnAction(e -> dismissTakeover());

        takeoverBox = new VBox(20, takeoverTitle, takeoverSub, dismiss);
        takeoverBox.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(takeoverFlash, takeoverBox);
        overlay.getStyleClass().add("takeover");
        return overlay;
    }

    /** A borderless window-control button rendered from a crisp SVG path. */
    private Button iconButton(String svg, String tip, String... extraClasses) {
        SVGPath glyph = new SVGPath();
        glyph.setContent(svg);
        glyph.getStyleClass().add("icon-glyph");

        Button b = new Button();
        b.setGraphic(glyph);
        b.getStyleClass().add("win-button");
        for (String c : extraClasses) {
            b.getStyleClass().add(c);
        }
        b.setFocusTraversable(false);
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    /** Let the user drag the window by this node (skips buttons and resize edges). */
    private void enableDrag(Node handle) {
        handle.setOnMousePressed(e -> {
            if (stage.isFullScreen() || isOnButton(e.getTarget()) || isOnResizeEdge()) {
                return;
            }
            dragOffset[0] = e.getScreenX() - stage.getX();
            dragOffset[1] = e.getScreenY() - stage.getY();
        });
        handle.setOnMouseDragged(e -> {
            if (stage.isFullScreen() || isOnButton(e.getTarget()) || isOnResizeEdge()) {
                return;
            }
            stage.setX(e.getScreenX() - dragOffset[0]);
            stage.setY(e.getScreenY() - dragOffset[1]);
        });
    }

    /** Show the wordmark only when there's comfortable room for it. */
    private void updateWordmark(double sceneWidth) {
        boolean show = sceneWidth >= 360;
        if (wordmark.isVisible() != show) {
            wordmark.setVisible(show);
            wordmark.setManaged(show);
        }
    }

    private boolean isOnResizeEdge() {
        return scene != null && scene.getCursor() != null && scene.getCursor() != Cursor.DEFAULT;
    }

    private boolean isOnButton(EventTarget target) {
        if (!(target instanceof Node)) {
            return false;
        }
        Node n = (Node) target;
        while (n != null) {
            if (n instanceof ButtonBase) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    private Label settingsLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private Spinner<Integer> intSpinner(int min, int max, int value) {
        Spinner<Integer> sp = new Spinner<>(min, max, value);
        sp.setEditable(true);
        sp.setPrefWidth(90);
        sp.getStyleClass().add("spinner");
        return sp;
    }

    // -------------------------------------------------------------- behaviour

    private void bindTimer() {
        timer.remainingSecondsProperty().addListener((o, a, b) -> updateDisplay());
        timer.totalSecondsProperty().addListener((o, a, b) -> updateDisplay());
        timer.phaseProperty().addListener((o, a, b) -> updateDisplay());
        timer.runningProperty().addListener((o, a, b) ->
                startBtn.setText(timer.isRunning() ? "Pause" : "Start"));
        timer.completedFocusSessionsProperty().addListener((o, a, b) -> updateSessionLabel());
        updateDisplay();
        updateSessionLabel();
    }

    private void handleStartPause() {
        if (inTakeover) {
            return;
        }
        timer.toggle();
    }

    private void updateDisplay() {
        int remaining = timer.getRemainingSeconds();
        int total = timer.getTotalSeconds();
        timeLabel.setText(format(remaining));
        compactTime.setText(format(remaining));
        phaseLabel.setText(timer.getPhase().label);

        double fraction = total > 0 ? (double) remaining / total : 0;
        progressArc.setLength(-360.0 * fraction);

        boolean isFocus = timer.getPhase() == Phase.FOCUS;
        setPseudoState(isFocus);
    }

    private void setPseudoState(boolean focus) {
        if (focus) {
            if (!rootStack.getStyleClass().contains("focus-phase")) {
                rootStack.getStyleClass().add("focus-phase");
            }
            rootStack.getStyleClass().remove("break-phase");
        } else {
            if (!rootStack.getStyleClass().contains("break-phase")) {
                rootStack.getStyleClass().add("break-phase");
            }
            rootStack.getStyleClass().remove("focus-phase");
        }
    }

    private void updateSessionLabel() {
        int done = timer.getCompletedFocusSessions();
        int interval = Math.max(1, settings.getLongBreakInterval());
        int inCycle = done % interval;
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < interval; i++) {
            dots.append(i < inCycle ? "● " : "○ ");
        }
        sessionLabel.setText(dots.toString().trim() + "    " + done + " completed");
    }

    private static String format(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void toggleSettings() {
        boolean show = !settingsOverlay.isVisible();
        if (show) {
            focusSpin.getValueFactory().setValue(settings.getFocusMinutes());
            shortSpin.getValueFactory().setValue(settings.getShortBreakMinutes());
            longSpin.getValueFactory().setValue(settings.getLongBreakMinutes());
            intervalSpin.getValueFactory().setValue(settings.getLongBreakInterval());
            autoStartChk.setSelected(settings.isAutoStartNext());
        }
        settingsOverlay.setVisible(show);
    }

    private void saveSettings() {
        settings.setFocusMinutes(focusSpin.getValue());
        settings.setShortBreakMinutes(shortSpin.getValue());
        settings.setLongBreakMinutes(longSpin.getValue());
        settings.setLongBreakInterval(intervalSpin.getValue());
        settings.setAutoStartNext(autoStartChk.isSelected());
        settings.save();
        settingsOverlay.setVisible(false);
        if (!timer.isRunning()) {
            timer.reset();
        }
        updateSessionLabel();
    }

    // ---------------------------------------------------------- window modes

    private void cycleMode() {
        if (inTakeover) {
            return;
        }
        switch (currentMode) {
            case NORMAL: applyMode(WindowMode.COMPACT); break;
            case COMPACT: applyMode(WindowMode.FULLSCREEN); break;
            case FULLSCREEN:
            default: applyMode(WindowMode.NORMAL); break;
        }
    }

    private void applyMode(WindowMode mode) {
        currentMode = mode;
        boolean compact = mode == WindowMode.COMPACT;

        // In compact mode show ONLY the bare timer view; otherwise the full UI.
        mainPane.setVisible(!compact);
        compactView.setVisible(compact);
        if (compact) {
            settingsOverlay.setVisible(false);
        }

        switch (mode) {
            case NORMAL:
                stage.setFullScreen(false);
                stage.setAlwaysOnTop(false);
                // Wordmark collapses under 360px, so the floor only needs to fit
                // the icon buttons themselves.
                stage.setMinWidth(300);
                stage.setMinHeight(360);
                stage.setWidth(420);
                stage.setHeight(580);
                break;
            case COMPACT:
                stage.setFullScreen(false);
                stage.setAlwaysOnTop(true);
                // allow the window to be dragged very small
                stage.setMinWidth(140);
                stage.setMinHeight(90);
                stage.setWidth(220);
                stage.setHeight(130);
                break;
            case FULLSCREEN:
                stage.setAlwaysOnTop(false);
                stage.setMinWidth(300);
                stage.setMinHeight(360);
                stage.setFullScreenExitHint("Press ESC to exit fullscreen");
                stage.setFullScreenExitKeyCombination(KeyCombination.keyCombination("ESC"));
                stage.setFullScreen(true);
                break;
            default:
                break;
        }
        updateModeButtons();
    }

    private void applyTheme(boolean dark) {
        if (dark) {
            if (!rootStack.getStyleClass().contains("dark")) {
                rootStack.getStyleClass().add("dark");
            }
            themeGlyph.setContent(IC_SUN);   // click to go back to light
        } else {
            rootStack.getStyleClass().remove("dark");
            themeGlyph.setContent(IC_MOON);  // click to go to dark
        }
    }

    private void toggleTheme() {
        boolean dark = !settings.isDarkMode();
        settings.setDarkMode(dark);
        settings.save();
        applyTheme(dark);
    }

    private void updateModeButtons() {
        setActive(compactBtn, currentMode == WindowMode.COMPACT);
        setActive(normalBtn, currentMode == WindowMode.NORMAL);
        setActive(fullBtn, currentMode == WindowMode.FULLSCREEN);
    }

    private void setActive(Button b, boolean active) {
        if (active) {
            if (!b.getStyleClass().contains("active")) {
                b.getStyleClass().add("active");
            }
        } else {
            b.getStyleClass().remove("active");
        }
    }

    // ------------------------------------------------------- alarm takeover

    private void onTimerComplete() {
        showTakeover();
    }

    private void showTakeover() {
        inTakeover = true;
        modeBeforeTakeover = currentMode;

        Phase ended = timer.getPhase();
        if (ended == Phase.FOCUS) {
            takeoverTitle.setText("Time for a break");
            takeoverSub.setText("Nice work — step away for a moment.");
        } else {
            takeoverTitle.setText("Back to focus");
            takeoverSub.setText("Break's over — let's get back to it.");
        }

        takeoverOverlay.setVisible(true);
        takeoverOverlay.toFront();
        startPulse();
        forceToFront();
    }

    private void forceToFront() {
        Platform.runLater(() -> {
            stage.setIconified(false);
            stage.setAlwaysOnTop(true);
            // Don't let ESC dismiss the takeover; a click is required.
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreen(true);
            stage.toFront();
            stage.requestFocus();
        });
    }

    private void dismissTakeover() {
        inTakeover = false;
        stopPulse();
        takeoverOverlay.setVisible(false);

        boolean auto = settings.isAutoStartNext();
        timer.advanceToNextPhase(auto);
        applyMode(modeBeforeTakeover);
    }

    private void startPulse() {
        stopPulse();
        ScaleTransition scale = new ScaleTransition(Duration.millis(750), takeoverBox);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.06);
        scale.setToY(1.06);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);

        FadeTransition flash = new FadeTransition(Duration.millis(750), takeoverFlash);
        flash.setFromValue(0.0);
        flash.setToValue(0.45);
        flash.setAutoReverse(true);
        flash.setCycleCount(Animation.INDEFINITE);

        pulse = new ParallelTransition(scale, flash);
        pulse.play();
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.stop();
            pulse = null;
        }
        takeoverBox.setScaleX(1.0);
        takeoverBox.setScaleY(1.0);
        takeoverFlash.setOpacity(0);
    }
}
