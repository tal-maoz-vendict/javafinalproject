# AGENTS.md

## Cursor Cloud specific instructions

This is a minimal Java Swing desktop game ("Color Blaster") with no build system or dependency manager.

### Project structure

- `assets/startscreen.java` — Single Java source file (Swing JFrame start screen)
- `assets/*.png` — Sprite images (ships and blocks in blue/green/red/yellow)

### Build & Run

```bash
cd assets
javac startscreen.java
DISPLAY=:1 java startscreen
```

- JDK 21 is pre-installed in the environment.
- The VM has a VNC display on `:1`; set `DISPLAY=:1` when running GUI apps.
- The app window closes when "Single Player" is clicked (calls `dispose()`).

### Testing

There are no automated tests. Manual verification: compile with `javac`, run, and confirm the Swing window appears.

### Lint

No linter is configured. Standard `javac` compiler warnings serve as the primary code-quality check.
