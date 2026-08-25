# IMPLEMENTATION.md — Autonomous Edge AI Solar Bird Repeller

Phase-by-phase build plan. Each phase has a goal, concrete steps, and an exit criterion — don't move to the next phase until the exit criterion is met. This keeps the project debuggable: camera, ML, scheduling, and audio are each fragile in their own way, and mixing them together before each is individually proven is the #1 way this kind of project stalls.

Companion file: `AGENT.md` (architecture rules, conventions, constraints). Source spec: `Project_Concept_Document__Autonomous_Edge_AI_Solar_Bird_Repeller`.

---

## Phase 0 — Environment & Project Setup

**Goal:** A blank app installs and runs on the target phone.

1. Install Android Studio (latest stable), JDK 17, Android SDK Platform for API level matching your target device + min SDK 26.
2. Create new project: Empty Compose Activity, Kotlin, `build.gradle.kts` (Kotlin DSL).
3. Decide and lock: `applicationId`, package root (e.g. `com.yourname.birdrepeller`), min SDK 26 / target SDK latest stable.
4. Set up version control (git) with a `.gitignore` for Android/Gradle (`local.properties`, `.gradle/`, `build/`).
5. Connect the actual repurposed phone via USB debugging (not just an emulator) — camera, thermal, and background-execution behavior only matter for real hardware.
6. Run the default "Hello World" Compose screen on-device.

**Exit criterion:** App installs and launches on the physical repurposed phone.

---

## Phase 1 — Permissions & Camera Preview

**Goal:** Live camera preview on screen, with permissions handled correctly.

1. Add manifest permissions: `CAMERA`, `RECORD_AUDIO` (only if needed later), storage access as required for SAF, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA` (API 34+), `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` (so scheduling can restart after reboot), `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` (API 31+).
2. Implement runtime permission request flow in Compose (`rememberLauncherForActivityResult`), gracefully handling denial.
3. Add CameraX dependencies. Bind a `Preview` use case to a `PreviewView` composable to confirm the camera pipeline works at all.
4. Confirm preview renders correctly in portrait and whatever mount orientation the enclosure will use.

**Exit criterion:** You can see a live camera feed on-screen from the mounted phone, permissions granted cleanly on first launch.

---

## Phase 2 — Single-Shot Capture (No ML Yet)

**Goal:** Prove discrete `ImageCapture` works reliably — this is the capture mode the whole power budget depends on, so validate it in isolation before adding inference.

1. Add CameraX `ImageCapture` use case alongside `Preview`.
2. Wire a temporary "Capture" button that takes one photo and saves it to app-internal storage.
3. Verify captured image quality/resolution is sufficient to detect a bird-sized object at the panel's actual camera-to-array distance (test with a real photo of the array, at least at dusk/dawn lighting extremes).
4. Measure and log: time from capture-trigger to image-ready. This number matters later for wake-cycle duration budgeting.

**Exit criterion:** Button-triggered single photo capture works reliably, image quality is sufficient at the real mounting distance/angle, and you know your capture latency.

---

## Phase 3 — On-Device Bird Detection (Manual Trigger)

**Goal:** TFLite inference correctly identifies birds in a captured frame — still triggered manually, no scheduling or audio yet.

1. Source or train a quantized bird-detection model (SSD MobileNet or YOLOv8n). If using a pretrained general object detector (e.g., COCO-based), confirm it includes a "bird" class; note its false-positive tendencies (kites, drones, leaves) for later tuning.
2. Add the `.tflite` file to `app/src/main/assets/`, document its source/version in `MODEL_README.md`.
3. Add TFLite Task Vision API dependency; implement an inference wrapper that takes a `Bitmap`/`ImageProxy` and returns bounding boxes + confidence scores.
4. Wire the Phase 2 capture button to also run inference and display bounding boxes overlaid on the captured image (debug UI only — not the final UX).
5. Test against a batch of real photos: birds present, birds absent, clouds/shadows present, birds outside camera range. Record accuracy informally.

**Exit criterion:** Inference correctly flags bird-present vs. bird-absent on a test set of real photos from the mounting location, with an acceptable false-positive rate on shadows/clouds.

---

## Phase 4 — ROI (Region of Interest) Editor

**Goal:** User can draw a box over the live preview marking "this is the solar panel area," and it persists.

1. Build a Compose overlay on top of `PreviewView` that lets the user draw/drag/resize a rectangle (drag handles on corners at minimum).
2. Store ROI coordinates normalized to image dimensions (not raw pixels) so it's resolution-independent.
3. Persist ROI to DataStore; reload it on app start so the box survives app restarts.
4. Add a "reset ROI" control.

**Exit criterion:** Drawing a box, restarting the app, and seeing the same box reappear works correctly.

---

## Phase 5 — ROI-Gated Detection

**Goal:** Combine Phase 3 (detection) + Phase 4 (ROI) — only detections intersecting the ROI count as "bird present."

1. Implement bounding-box-intersects-ROI logic (simple rectangle overlap test; decide the overlap threshold — e.g. any overlap vs. center-point-inside-ROI vs. >50% area inside).
2. Update the debug overlay to visually distinguish in-ROI detections (trigger) from out-of-ROI detections (ignored).
3. Re-test against the Phase 3 test photo set, now confirming birds outside the ROI (background trees, sky) are correctly ignored.

**Exit criterion:** Only birds landing within the drawn ROI are flagged as actionable detections; birds elsewhere in frame are correctly ignored.

---

## Phase 6 — Audio Deterrent Actuation

**Goal:** A detected in-ROI bird triggers audio playback through the 3.5mm jack, cycling through user-provided sound files.

1. Implement SAF file picker for importing `.mp3`/`.wav` files into app-managed storage (persist chosen URIs/permissions).
2. Build a playlist-cycling strategy (e.g., round-robin or random-without-immediate-repeat) so the same sound doesn't play every time.
3. Implement `MediaPlayer` playback: force system/media volume to max before playing, play through wired output, handle playback errors (missing file, corrupt audio) without crashing.
4. Wire the Phase 5 "in-ROI detection" trigger to call this playback logic.
5. Bench-test with the actual amplifier + piezo horn hardware connected via the 3.5mm jack — confirm signal chain works end-to-end, not just "sound plays from phone speaker."

**Exit criterion:** A manually-triggered in-ROI detection reliably plays a deterrent sound through the real external amp/speaker hardware, cycling correctly across multiple triggers.

---

## Phase 7 — Background Scheduling & Duty Cycle

**Goal:** The whole pipeline (capture → inference → ROI check → actuation) runs autonomously on a timer, screen off, without user interaction.

1. Design the duty-cycle state machine (sealed class: `Sleeping`, `Capturing`, `Inferring`, `Alerting`, `Cooldown`) per `AGENT.md` conventions.
2. Implement a `ForegroundService` that performs one full capture-inference-actuate cycle when woken, then stops itself.
3. Implement `AlarmManager.setExactAndAllowWhileIdle` scheduling: default 5-minute interval, re-arming itself after each cycle (don't use a repeating alarm — re-schedule each time so the interval can change dynamically).
4. Implement adaptive interval: on positive detection, next alarm fires in 1 minute ("High Alert"); on a clear frame, step back toward 5 minutes (decide and implement your step-down rule from `AGENT.md` §7).
5. Scope a partial `WakeLock`: acquire right before capture starts, release right after the cycle completes — never held across the sleep interval.
6. Handle device reboot: re-arm the alarm via a `BOOT_COMPLETED` broadcast receiver.
7. Test with screen off and app backgrounded — confirm the cycle actually fires without any foreground UI interaction, on the real device (watch for OEM battery-optimization killing the service — you may need in-app guidance to disable it for this app).

**Exit criterion:** With the screen off and the app not in foreground, the device autonomously captures, detects, and (when appropriate) actuates on the correct interval for at least a few consecutive hours, surviving a device reboot.

---

## Phase 8 — Telemetry & Event Logging

**Goal:** Every cycle is recorded so you can review system behavior over time.

1. Define Room entity: timestamp, outcome (clear / detected-in-ROI / detected-out-of-ROI), confidence score, audio file played (if any), cycle duration.
2. Log every cycle from the Phase 7 service (or a rolling summary if full per-cycle logging is too noisy — your call, document the choice).
3. Build a simple Compose dashboard screen listing recent events (most-recent-first), with basic stats (detections today, last trigger time).
4. Add a settings screen: adjust default interval, High Alert interval, manage imported audio files, view/edit ROI.

**Exit criterion:** You can open the app after a day of autonomous operation and see an accurate history of what happened, when.

---

## Phase 9 — Field Hardening & Soak Test

**Goal:** Meet the concept doc's success criteria under real outdoor conditions.

1. Mount the phone in its final enclosure with real power delivery (solar → buck converter → battery terminals) and real audio chain (amp + piezo horn).
2. Run a continuous 48-hour soak test. Monitor CPU temperature/throttling — confirm no thermal shutdown or crash (Success Criterion 1).
3. Log false positives against real-world shadows, cloud movement, and birds outside the ROI over multiple days; tune detection confidence threshold and/or ROI overlap threshold as needed (Success Criterion 2).
4. Confirm the camera snapshot reliably fires on schedule with the screen off, unattended, across day/night cycles and varying light (Success Criterion 3).
5. Verify power balance: battery doesn't drain faster than the solar panel can recharge it across a full 24-hour cycle, including cloudy days if possible.
6. Fix issues found; re-run the soak test after any change to scheduling, wake-lock handling, or inference frequency.

**Exit criterion:** All three success criteria from the concept doc are met simultaneously across a real 48+ hour unattended outdoor run.

---

## Phase 10 — Polish & Documentation (Optional / Ongoing)

1. Improve/replace the detection model if false-positive rate isn't acceptable (consider fine-tuning on your own labeled photos from the actual install site).
2. Add onboarding UI (first-run permission + battery-optimization exemption walkthrough).
3. Add export for the event log (CSV) if you want to analyze effectiveness over a season.
4. Write up build notes / lessons learned — useful both as documentation and as a record of what you learned about Android dev.

---

## Quick Reference: Phase → Milestone Mapping

| Phase | Corresponds to AGENT.md Milestone |
|---|---|
| 0–1 | Milestone 1 (Skeleton app) |
| 2–3 | Milestone 2 (Manual capture + inference) |
| 4 | Milestone 3 (ROI editor UI) |
| 5 | Milestone 4 (ROI-gated detection) |
| 6 | Milestone 5 (Audio actuation) |
| 7 | Milestone 6 (Scheduling) |
| 8 | Milestone 7 (Telemetry) |
| 9 | Milestone 8 (Hardening) |