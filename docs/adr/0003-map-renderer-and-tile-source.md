# ADR 0003: Route map with MapLibre Native Android and OpenFreeMap vector tiles

- Status: Accepted
- Date: 2026-10-19
- Run: `20261019-dog-walk-buddy-mvp`

## Context
Walk detail must draw the saved route on a street map, zoomed to fit (AC38). The tile source must be free, keyless and allowed for commercial use. It must also be easy to swap (spec Assumption 13). OpenStreetMap Foundation tile servers are excluded.

Offline, or when tiles fail to load, the screen shows "Map not available offline" (AC39). The required attribution is "© OpenStreetMap contributors" and "© OpenMapTiles" (AC38). The only hosts allowed are the OpenFreeMap tile and style hosts (AC49).

## Commercial-use re-confirmation (done at design time, 2026-10-19)
- **OpenFreeMap FAQ** (https://openfreemap.org/, FAQ section): the question "Is commercial usage allowed?" is answered **"Yes."**
  - The same FAQ asks for a unique User-Agent and limits bulk or offline downloads (at most 3,000 tiles or 50 MB, at most 4 parallel requests). This app does no bulk download.
- **OpenFreeMap Terms of Service** (https://openfreemap.org/tos/, last updated 26 Feb 2025, operator Hyperknot Software Kft., Hungary): no clause restricts commercial use. The terms say:
  - The service is provided as-is, with no warranty or SLA.
  - It may be discontinued at any time without notice.
  - It may use **Cloudflare** as a CDN, which processes requests.
  - Hungarian law and arbitration apply.
- **Data licences:** OpenStreetMap data is ODbL (commercial use with attribution). The OpenMapTiles schema is BSD-3 and CC BY 4.0. OpenFreeMap software is MIT.
- **Result: commercial use is allowed with attribution. No BLOCK.**
- Sources:
  - https://openfreemap.org/
  - https://openfreemap.org/tos/
  - https://github.com/hyperknot/openfreemap

## Options considered
1. **MapLibre Native Android 13.6.1 with the OpenFreeMap "liberty" style** (`https://tiles.openfreemap.org/styles/liberty`).
   - Pros:
     - The standard keyless renderer for OpenFreeMap vector styles.
     - The style, sprites, glyphs and tiles all come from `tiles.openfreemap.org`, so there is one host.
     - BSD-2 licence, no telemetry.
     - `LatLngBounds` fit-to-route.
     - A line layer from a GeoJSON source built on the device, so route coordinates never leave the phone.
   - Cons:
     - Native libraries (APK size).
     - Cannot run under Robolectric.
     - Vulkan is the default renderer in 13.x.
2. **osmdroid with raster tiles.**
   - Pros: pure Java.
   - Cons: OpenFreeMap serves vector tiles only, and the free raster alternatives are OSMF tiles (excluded) or keyed providers. Rejected.
3. **Google Maps SDK or Mapbox.** Rejected: an API key, a proprietary licence and telemetry.
4. **Static route drawing without a map.** Rejected: AC38 requires a street map.

## Decision
Option 1, wrapped so it is swappable and testable.

- **`MapConfig`** holds the single constant `STYLE_URL` and the attribution strings. Changing provider means changing that constant and the attribution resources.
- **Interface `RouteMap`.**
  - A composable slot: `@Composable (route: List<LatLon>, onLoadFailed: () -> Unit) -> Unit`.
  - The production implementation `MapLibreRouteMap` is in package `...map.maplibre`. It is excluded from coverage (ADR 0001) and verified manually.
  - Tests inject a fake `RouteMap`, so walk-detail logic (offline message, attribution, fit bounds computation) is covered on the JVM.
- **Fit to route:**
  - The bounds are computed in pure Kotlin (`RouteBounds.of(points)`). For a single-point route, a fixed padding is added.
  - The adapter calls `easeCamera`/`moveCamera` with `CameraUpdateFactory.newLatLngBounds(bounds, padding)`.
- **Attribution:**
  - The app renders its own visible `Text`, "© OpenStreetMap contributors · © OpenMapTiles", over the map.
  - MapLibre's own attribution button is kept enabled, but it is not relied on.
- **Offline and failure:**
  - If `ConnectivityManager` reports no validated network when the detail opens, the map is not created at all, and "Map not available offline" is shown instead.
  - If the map's `OnDidFailLoadingMapListener` fires, the same message replaces the map (AC39).
  - The duration, distance and date are always shown.
- **HTTP:** MapLibre is given the app's OkHttp client (`HttpRequestUtil.setOkHttpClient`). That client has the HTTPS-only interceptor and the app User-Agent `DogWalkBuddy/<versionName>`, which follows OpenFreeMap's unique-UA guidance.
- **Ambient cache:** MapLibre's tile cache lives in app-private storage and is excluded from backup with everything else. "Delete all my data" calls `OfflineManager.clearAmbientCache` because cached tiles imply the areas of the routes the user viewed.
- **Renderer:**
  - The default `android-sdk` artifact (Vulkan) is used.
  - If the AC38 manual check fails on the reference phone because of rendering, the fallback is MapLibre's OpenGL ES artifact for the same version. That swap needs an amendment to this ADR.

## Consequences
- No SLA. AC39 makes failure graceful, and `MapConfig` makes a swap a one-file change, for example to a self-hosted OpenFreeMap or another keyless commercial-use host.
- **Privacy note (the owner approved this Cloudflare wording at the stack checkpoint on 2026-10-19):** the OpenFreeMap ToS says requests may be processed by Cloudflare as a CDN. The Privacy screen text (AC46) names OpenFreeMap as the map provider. The design adds "(served through its CDN provider, Cloudflare)" after it. This is an accurate, additive disclosure and does not change the AC.
- APK size grows. Use an App Bundle.

## For new dependencies
- Licence: MapLibre Native BSD-2-Clause, with bundled third-party notices shown in the app's licences list. Tile data ODbL and OpenMapTiles (BSD-3 / CC BY 4.0).
- Maintenance: MapLibre organisation with monthly releases in 2026 (13.x line).
- Known vulnerabilities: none known for 13.6.1 at design time.
- Size / performance impact: about 10-15 MB of native libraries per ABI. The map is only created on the walk-detail screen.
