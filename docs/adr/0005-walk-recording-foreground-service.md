# ADR 0005: Walk recording with a location foreground service and platform LocationManager

- Status: Accepted
- Date: 2026-10-19
- Run: `20261019-dog-walk-buddy-mvp`

## Context
Walk recording must meet these requirements:
- It starts within 1 s of a tap (AC28).
- It keeps recording with the screen locked or another app open (AC29).
- It works in airplane mode with GPS on (AC31).
- It measures distance within ±2 % on a clean track (AC32).
- It survives the process being killed (AC34).
- It uses no more than 8 % battery per hour on the reference phone (NFR).
- It shows a persistent notification (AC28).
- It uses precise location only while recording, and approximate location only for the check (spec, Security).

Google Play reviews background location, which is a release-time risk.

## Options considered
1. **Foreground service, type `location`, started from the visible activity, using the platform `LocationManager` `GPS_PROVIDER` through `LocationManagerCompat`.**
   - Pros:
     - No Google Play Services dependency, which is proprietary. It also works on devices without GMS.
     - The GPS provider works fully offline.
     - Location in a foreground service started while the app is visible keeps "while in use" access, so **`ACCESS_BACKGROUND_LOCATION` is not requested**. This keeps the Play policy risk low.
   - Cons:
     - No sensor fusion.
     - It may use slightly more battery than the fused provider in urban canyons.
2. **Fused Location Provider (`play-services-location`).**
   - Pros: battery-optimised fusion.
   - Cons:
     - A proprietary Google SDK. Its Terms of Service and data-collection disclosure for Play would have to be assessed against "no tracking".
     - It does not work without GMS.
3. **WorkManager periodic location.** Rejected: a minimum interval of about 15 minutes, which cannot give a route.

## Decision
Option 1.

- **`WalkRecordingService`** extends `LifecycleService`.
  - Manifest: `android:exported="false"` and `android:foregroundServiceType="location"`.
  - Permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` and `ACCESS_FINE_LOCATION`. `POST_NOTIFICATIONS` is requested in context on API 33+.
  - If notification permission is denied, recording still runs. The system task manager still lists the service, and the recording screen shows a hint.
- **Start sequence (AC28):**
  1. On the Start tap, check for precise location. If it is missing, show the explanation and the grant control, and start nothing (AC33).
  2. Insert the `walk` row (`RECORDING`, `start_utc_ms = clock.now`).
  3. `ContextCompat.startForegroundService`.
  4. The service calls `ServiceCompat.startForeground` with an ongoing notification in channel `walk_recording` ("Recording your walk"). The notification's content intent opens the app. There is no stop action, because stopping from the notification is out of scope.
- **Location requests:**
  - `GPS_PROVIDER` with `LocationRequestCompat`: interval 3 s, minimum update distance 0 m, quality `QUALITY_HIGH_ACCURACY`.
  - The interval is tuned for the 8 %/h battery budget. A UI refresh of 5 s or faster (AC28) is met by a 1 s elapsed-time ticker plus the distance Flow.
- **Fix filter and distance (pure Kotlin `RouteDistanceCalculator`):**
  - Reject fixes with accuracy over 25 m, or with a timestamp that is not after the previous fix.
  - Add a segment's haversine length (R = 6,371,008.8 m) only when it is at least `max(3 m, 0.5 × combined accuracy)`. This suppresses stationary jitter.
  - Each accepted fix is persisted transactionally (ADR 0004).
- **Elapsed time:** `clock.now − start`, never a sum of fixes, so locked-screen periods count (AC29). `Clock` is injected.
- **Stop (AC30):**
  1. Set `end_utc_ms = clock.now` and `status = COMPLETE`.
  2. Remove location updates.
  3. Call `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf`.
- **`onStartCommand` returns `START_NOT_STICKY`.** A system restart of the service without the app visible could not legally restart foreground location anyway. Recovery happens on next launch instead (ADR 0004, AC34).
- **Process-local `RecordingStateHolder`:** a `StateFlow` that the service publishes and the UI observes. Its absence in a fresh process is what marks a `RECORDING` row as interrupted.
- **Location abstraction:** domain-facing interfaces `WalkLocationSource : Flow<LocationFix>` and `ApproximateLocationSource.current(): LocationFix?`. Tests inject fakes. The adapters are tested with Robolectric's `ShadowLocationManager`.
- **Approximate location for the check:**
  - `ACCESS_COARSE_LOCATION` only, requested when the check is first opened (AC25).
  - It uses `LocationManagerCompat.getCurrentLocation` with `FUSED_PROVIDER` where available (API 31+), otherwise `NETWORK_PROVIDER`, with a 10 s timeout.
  - A last-known fix under 30 minutes old is used first, for speed.
  - The service never uses the coarse path, and the check never uses `GPS_PROVIDER`.

## Consequences
- No background-location permission and no GMS dependency, so the Play declaration is simpler. The foreground-service declaration for the `location` type is still required in Play Console. That is the owner's release-time task.
- Battery needs a manual measurement on the reference phone. If it is over 8 %/h, the first lever is the interval (3 s → 5 s), which needs no redesign.
- Reversal: swapping to the fused provider is contained in the `WalkLocationSource` adapter, plus a new ADR for the GMS dependency.

## For new dependencies
None. This uses the platform plus `androidx.core` and `androidx.lifecycle:lifecycle-service`, which ADR 0001 covers.
