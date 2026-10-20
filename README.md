# Pomodoro

A clean, modern desktop Pomodoro timer in light earth tones (greige + sage).
Built with **Java + JavaFX**, packaged into a standalone Windows app.

## Features

- Classic **25 / 5 / 15** timer with a long break every 4 focus sessions — all
  editable in **Settings** (⚙).
- **Borderless window** — no native Windows title bar. Move it by dragging the
  top bar (or the timer itself in compact mode), resize it from any edge or
  corner, and use the custom **—** / **×** buttons (top-right) to minimize/close.
- **Light & dark mode** (`Dark` / `Light` toggle in the top bar) — both in warm
  earth tones; your choice is remembered.
- Three window modes you can toggle any time:
  - **Compact** — a tiny, freely resizable always-on-top window showing *only*
    the timer (the digits scale with the window) plus a small ⤢ button to
    return to the normal window.
  - **Window** — normal resizable window.
  - **Full** — fullscreen on the current monitor.
- **Alarm takeover** when a session ends: the window restores itself (even from
  minimized), jumps to the front, goes fullscreen, and pulses — you dismiss it
  with a click.
- **Global hotkeys** (work even when the app isn't focused):
  - `Ctrl+Alt+P` — start / pause
  - `Ctrl+Alt+M` — cycle window mode
  - `Ctrl+Alt+R` — reset current phase
- **Auto-start next session** (toggleable in Settings).

Settings persist to `~/.pomodoro/settings.properties`.

## Requirements (to build)

- A **JDK 21** on Windows (you have Adoptium JDK 21 installed).
- Maven does **not** need to be installed — the project ships a Maven Wrapper
  (`mvnw.cmd`) that downloads the right Maven version automatically on first run.
- The Windows `.exe` must be built **on Windows** — `jpackage` produces a
  package for the OS it runs on, so building from WSL would give a Linux app.

## Run it during development

From a Windows terminal (cmd or PowerShell) in this folder:

```
run-dev.bat
```

This builds a runnable fat jar and launches it with `java -jar`.

## Build the standalone Windows app

```
build-exe.bat
```

This produces `dist\Pomodoro\Pomodoro.exe` — a self-contained app with its own
bundled Java runtime, so it runs on machines without Java installed. Double-click
`Pomodoro.exe` to launch, or make a shortcut / pin it to your taskbar.

> Want a single double-click **installer** (`.msi`) instead of a folder? Install
> the [WiX Toolset](https://wixtoolset.org/) and change `--type app-image` to
> `--type msi` in `build-exe.bat`.

## Project layout

```
pom.xml                              Maven build (JavaFX + shade + jpackage-ready)
build-exe.bat                        Build the standalone Windows app
run-dev.bat                          Quick dev run
src/main/java/com/pomodoro/
  Launcher.java                      Entry point (enables fat-jar JavaFX launch)
  App.java                           UI, window modes, alarm takeover
  PomodoroTimer.java                 Timer state machine
  Settings.java                      Persisted settings
  GlobalHotkeys.java                 System-wide hotkeys (jnativehook)
src/main/resources/styles.css        Theme (greige + sage)
src/main/resources/icons/            App icon PNGs (taskbar/window)
packaging/icon.svg                   Icon source (edit, then regenerate)
icon.ico                             Multi-res icon for the packaged .exe
```

### Regenerating the app icon

Edit `packaging/icon.svg`, then (needs `rsvg-convert` + ImageMagick):

```
for s in 16 24 32 48 64 128 256; do rsvg-convert -w $s -h $s packaging/icon.svg -o src/main/resources/icons/icon-$s.png; done
magick src/main/resources/icons/icon-256.png -define icon:auto-resize=256,128,64,48,32,16 icon.ico
```

## Notes

- Windows restricts apps from stealing keyboard focus from a fullscreen app in
  some cases; the takeover uses always-on-top + fullscreen + restore so it still
  visually takes over the screen even when those restrictions apply.
- The palette: bg `#ECEBE6`, surface `#D5CFC0`, borders `#B2ADA0`, text
  `#3E3A33`, sage accent `#7E9079`.
