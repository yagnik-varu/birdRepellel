# AGENT.md — Autonomous Edge AI Solar Bird Repeller

This file orients any AI coding agent (or human) working on this repository. Read it before making changes. It captures project intent, architecture, conventions, and current status so work stays consistent across sessions.

---

## 1. What This Project Is

An Android app that turns an old, upcycled smartphone into a solar-powered, edge-AI security agent for a rooftop solar array. It wakes on a timer, snaps a photo, runs on-device object detection to spot roosting birds inside a user-defined zone, and — if a bird is found — blasts an audio deterrent out the headphone jack to an external amp/speaker.

**Primary goals for this build:**
- Solve a real, physical problem (bird droppings degrading solar panel output).
- Serve as a hands-on Android development learning project (Kotlin, Jetpack Compose, CameraX, TFLite, background execution).

Full product spec lives in: `Project_Concept_Document__Autonomous_Edge_AI_Solar_Bird_Repeller` (treat as source of truth for feature intent; this file governs *how we build it*).

---

## 2. Current Status

**Stage: Pre-code / greenfield.** No source tree exists yet. When the first module is scaffolded, update this section with:
- Date scaffolding started
- Which milestone (see §8) is in progress
- Any deviations from the concept doc and why

Until code exists, agents should treat requests to "fix" or "refactor" something as requests to *design and create it first*, and confirm scope before generating a large amount of code in one pass.

---

## 3. Tech Stack (authoritative)

| Layer | Choice |
|---|---|
| Language | Kotlin (no Java for new code) |
| UI | Jetpack Compose, Material 3 |
| Camera | CameraX (`ImageCapture` use case only — not `ImageAnalysis` streaming) |
| ML inference | TensorFlow Lite Task Vision API, quantized model (SSD MobileNet or YOLOv8n) |
| Background execution | `AlarmManager.setExactAndAllowWhileIdle`, a `ForegroundService` for the capture/inference window, partial `WakeLock` held only for that window |
| Local storage | Room (event history), DataStore (ROI + settings) |
| Audio | Android SAF for file import, `MediaPlayer` for playback out the 3.5mm jack |
| Min SDK | Android 8.0 (API 26) |
| Build | Gradle (Kotlin DSL preferred: `build.gradle.kts`) |

Do not introduce new architectural components (e.g., a different DI framework, a different DB, streaming video analysis) without flagging the tradeoff — this is a battery/thermal-constrained device, and "obvious" upgrades (e.g., continuous `ImageAnalysis`) directly violate the power budget this design is built around.

---

## 4. Non-Negotiable Design Constraints

These come directly from the project's physical constraints. Violating them isn't a style issue — it can overheat the phone, drain the battery faster than the solar panel can recharge it, or brick the deterrent logic.

1. **No continuous camera streaming.** Single discrete `ImageCapture` snapshots only, on a duty cycle. Never wire up `ImageAnalysis` in streaming mode for the main detection loop.
2. **Screen stays off during normal operation.** Any code path that calls into UI rendering during the background wake cycle is a bug.
3. **WakeLocks are scoped tightly.** Acquire immediately before capture+inference, release immediately after. No wake locks held across the sleep interval.
4. **Duty cycle is adaptive, not fixed.** Default 5-minute interval; drop to 1-minute "High Alert" polling after a positive detection, and only step back up to 5 minutes after a clear frame (or N consecutive clear frames — decide and document the threshold when implemented).
5. **ROI gating happens after detection, before actuation.** The model may detect a bird anywhere in frame; only a detection whose bounding box intersects the user-drawn ROI should trigger audio. Never trigger on detections outside the ROI.
6. **Audio playback maximizes volume and must be interruptible/loggable** — always log what played and when, even if playback fails.
7. **Thermal/power telemetry matters.** Any new feature that adds CPU/GPU work during the wake cycle should be evaluated against the 48-hour thermal stability success criterion (§6 of concept doc).

---

## 5. Project Structure (target layout)

Use this as the scaffold when creating the app. Keep detection, scheduling, and actuation loosely coupled so each can be tested/tuned independently.

```
app/
 └─ src/main/java/.../birdrepeller/
     ├─ scheduling/       # AlarmManager receivers, ForegroundService, duty-cycle state machine
     ├─ capture/          # CameraX setup, single-frame capture logic
     ├─ detection/        # TFLite model loading, inference, ROI intersection logic
     ├─ actuation/        # Audio file cycling, MediaPlayer playback, volume control
     ├─ data/
     │   ├─ db/           # Room entities/DAOs for event history
     │   └─ settings/     # DataStore for ROI coordinates, thresholds, audio playlist
     ├─ ui/
     │   ├─ roi/          # Compose ROI draw/drag editor over live preview
     │   ├─ dashboard/     # Event history / telemetry log view
     │   └─ settings/      # Audio import, interval config
     └─ di/               # Manual DI or Hilt — decide once and stay consistent
```

Model assets (`.tflite`) go in `app/src/main/assets/`. Keep the model file small and quantized; document its source/training data provenance in `app/src/main/assets/MODEL_README.md` once added.

---

## 6. Coding Conventions

- **Kotlin idioms:** prefer immutable `val`, data classes for state, sealed classes for the duty-cycle state machine (`Sleeping`, `Capturing`, `Inferring`, `Alerting`, `Cooldown`).
- **Coroutines** over callbacks for camera/inference/IO; use `Dispatchers.Default` for inference, `Dispatchers.IO` for file/db work.
- **Compose:** stateless, hoisted-state composables; ROI editor state lives in a `ViewModel`, not in the composable.
- **No hardcoded strings for thresholds/intervals** — pull from DataStore-backed settings with sane defaults defined once in a `Config` or `Defaults` object.
- **Every background entry point logs a timestamped event** to Room, even "nothing detected" cycles (or a rolling summary, if full logging is too noisy — decide and document).
- **Tests:** unit-test the ROI-intersection math and duty-cycle state machine in isolation (no camera/hardware dependency needed for these). Prefer JVM unit tests over instrumented tests where logic doesn't touch Android framework classes.

---

## 7. Things an Agent Should Ask About Before Assuming

- Which detection model file/source to use (none is bundled yet — quantized SSD MobileNet vs. YOLOv8n is still open).
- Hilt vs. manual DI — not yet decided.
- Exact "High Alert → normal" step-down policy (single clear frame vs. N consecutive).
- Package name / applicationId for the app.
- Target device(s) for testing (screen-off background behavior varies by OEM battery-optimization settings — may need manufacturer-specific "disable battery optimization" guidance in-app).

If asked to generate code touching any of the above, state the assumption made and proceed — don't block on it unless it's a large amount of code that would need rework.

---

## 8. Suggested Build Milestones

Useful for scoping a session's work — pick one milestone at a time rather than generating the whole app at once.

1. **Skeleton app** — empty Compose app, permissions (camera, storage), min SDK setup, builds and installs.
2. **Manual capture + inference** — button-triggered (not yet scheduled) single photo → TFLite inference → log result to console. Validates the ML pipeline works before automating it.
3. **ROI editor UI** — draw/drag box over live preview, persist to DataStore.
4. **ROI-gated detection** — wire inference output through ROI intersection check.
5. **Audio actuation** — SAF import, playlist cycling, MediaPlayer trigger, volume max.
6. **Scheduling** — AlarmManager duty cycle, ForegroundService, WakeLock scoping, adaptive interval.
7. **Telemetry** — Room event log + dashboard UI.
8. **Hardening** — thermal/battery soak test against the 48-hour criterion, false-positive tuning against shadows/clouds.

---

## 9. Reference

- Full spec: `Project_Concept_Document__Autonomous_Edge_AI_Solar_Bird_Repeller`
- Hardware side (solar panel, buck converter, amp, enclosure) is out of scope for this codebase but relevant context for power/thermal decisions in software.