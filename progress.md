# PROGRESS.md

Tracks phase-by-phase progress. Update status and date as work happens. See `IMPLEMENTATION.md` for phase details.

| Phase | Status | Date | Notes |
|---|---|---|---|
| 0 — Environment & Project Setup | In Progress | 2026-08-25 | Project skeleton created, git initialized |
| 1 — Permissions & Camera Preview | Done | 2026-08-25 | CameraX preview with runtime permissions implemented |
| 2 — Single-Shot Capture | Done | 2026-08-25 | Implemented and verified on device |
| 3 — Bird Detection (Manual) | Done | 2026-08-25 | TFLite wrapper implemented and verified with broad labels |
| 4 — ROI Editor | Done | 2026-08-25 | Draggable/resizable ROI overlay implemented with DataStore persistence |
| 5 — ROI-Gated Detection | Done | 2026-08-25 | Combined detection with ROI intersection logic; verified with unit tests. Overlap rule: Any Overlap (AABB). |
| 6 — Audio Deterrent Actuation | In Progress | 2026-08-25 | SAF import and MediaPlayer playback with playlist cycling implemented. Awaiting manual hardware verification. |
| 7 — Background Scheduling | Not Started | | |
| 8 — Telemetry & Logging | Not Started | | |
| 9 — Field Hardening & Soak Test | Not Started | | |
| 10 — Polish & Documentation | Not Started | | |

**Status values:** Not Started · In Progress · Blocked · Done

## Current Focus
Phase 5 — ROI-Gated Detection

## Blockers
_(anything stuck, waiting on hardware, decisions pending)_