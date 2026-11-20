# Transit Stop Bounds Mobile Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Load bus and metro stop markers for the Mapbox viewport and render them through the existing clustered transit sources.

**Architecture:** Retrofit requests the two authenticated bounding-box APIs with a typed JSON body. `MapRepository` concurrently loads the endpoint-backed default modes, converts marker DTOs into FeatureCollection JSON, and returns one `TransitSourceData` per configured type. The Mapbox module continues to create/update `GeoJsonSource` instances from that JSON; the ViewModel owns buffering, cache validity, and UI state.

**Tech Stack:** Kotlin, Coroutines, Retrofit, kotlinx.serialization, Mapbox Maps SDK, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-15-transit-stop-bounds-design.md`

## Global Constraints

- Preserve `BUS`, `METRO`, `TRAIN`, and `BIKE` in `MapRepository.defaultConfigs`.
- Call only the available bounds APIs: `POST /transit-stops/bus/search-in-bounds` and `POST /transit-stops/metro/search-in-bounds`.
- Send `{minLat, minLon, maxLat, maxLon}` in WGS84; marker geometry must use `[longitude, latitude]`.
- Do not create mock stops or alter backend/database code.
- `TRAIN` and `BIKE` must receive empty FeatureCollections until an API mapping exists.
- Retain the existing JWT interceptor path.

## Review Focus

- A marker at longitude `105.8151131`, latitude `20.9998526` becomes `[105.8151131,20.9998526]`; covered in Task 2.
- One endpoint failure must not discard successfully loaded data from the other endpoint; covered in Task 2.
- If all endpoint-backed modes fail, the existing error UI must be reached; covered in Task 2.
- Camera padding, bearing, and pitch must be reflected in the requested viewport; covered in Task 3.
- A wrapped antimeridian viewport must not send `minLon > maxLon` to the backend; covered in Task 3.

---

### Task 1: Typed transit-stop API contract

**Files:**
- Modify: `core/data/src/main/java/com/vie/mit/data/network/api/map/MapApi.kt`
- Create: `core/data/src/main/java/com/vie/mit/data/network/model/map/TransitStopBoundsRequest.kt`
- Create: `core/data/src/main/java/com/vie/mit/data/network/model/map/TransitStopMarkerResponse.kt`
- Test: `core/data/src/test/java/com/vie/mit/data/network/model/map/TransitStopApiModelTest.kt`

**Interfaces:**
- Produces: `@Serializable data class TransitStopBoundsRequest(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)`.
- Produces: `@Serializable data class TransitStopMarkerResponse(val id: String, val name: String, val latitude: Double, val longitude: Double)`.
- Produces: `suspend fun searchBusStopsInBounds(@Body bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse>` and the matching metro method.

- [ ] **Step 1: Write failing serialization tests** asserting the bounds JSON keys are exactly `minLat`, `minLon`, `maxLat`, `maxLon`, and the sample marker decodes without field loss.
- [ ] **Step 2: Run the focused test**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TransitStopApiModelTest"`

Expected: FAIL because the DTOs and API methods do not exist.

- [ ] **Step 3: Add the two serializable DTOs and replace `getTransitStopsGeoJson` in `MapApi` with the two typed POST methods**

Use Retrofit `@POST`, `@Body`, and the exact paths from the global constraints.

- [ ] **Step 4: Re-run the focused test**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TransitStopApiModelTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/java/com/vie/mit/data/network/api/map/MapApi.kt core/data/src/main/java/com/vie/mit/data/network/model/map/TransitStopBoundsRequest.kt core/data/src/main/java/com/vie/mit/data/network/model/map/TransitStopMarkerResponse.kt core/data/src/test/java/com/vie/mit/data/network/model/map/TransitStopApiModelTest.kt
git commit -m "feat: add typed transit stop bounds API"
```

### Task 2: Concurrent marker-to-GeoJSON repository mapping

**Files:**
- Modify: `core/data/src/main/java/com/vie/mit/data/repository/map/MapRepository.kt`
- Test: `core/data/src/test/java/com/vie/mit/data/repository/map/MapRepositoryTest.kt`

**Interfaces:**
- Consumes: the Task 1 DTOs and `MapApi` methods.
- Produces: `suspend fun getTransitStopsGeoJson(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double): Result<Map<String, String>>`.
- Produces: a FeatureCollection for each `defaultConfigs` type, with each feature containing `id`, `stop_id`, `name`, and `type`.

- [ ] **Step 1: Write failing repository tests** using a fake `MapApi` for: concurrent BUS/METRO calls, feature coordinate order/properties, empty TRAIN/BIKE collections, partial success, and all-supported-mode failure.
- [ ] **Step 2: Run the focused test**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MapRepositoryTest"`

Expected: FAIL because the API contract and GeoJSON mapping do not match the test.

- [ ] **Step 3: Implement endpoint mapping and GeoJSON conversion in `MapRepository`**

Use `supervisorScope` plus `async` for BUS and METRO; build JSON with kotlinx.serialization JSON elements rather than string interpolation. Preserve all default configs, return empty FeatureCollections for unsupported types or a single failed supported mode, and return failure when all supported calls fail. Remove `createMockTransitGeoJson`.

- [ ] **Step 4: Re-run the focused test**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*MapRepositoryTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/java/com/vie/mit/data/repository/map/MapRepository.kt core/data/src/test/java/com/vie/mit/data/repository/map/MapRepositoryTest.kt
git commit -m "feat: map transit stop markers to geojson"
```

### Task 3: Viewport-bound normalization and cache correctness

**Files:**
- Modify: `core/mapbox/src/main/java/com/vie/mit/mapbox/composable/TransitStopsMapScreen.kt`
- Modify: `app/src/main/java/com/vie/mit/green/ui/home/HomeContainerViewModel.kt`
- Test: `core/mapbox/src/test/java/com/vie/mit/mapbox/composable/TransitViewportBoundsTest.kt`
- Test: `app/src/test/java/com/vie/mit/green/ui/home/HomeContainerViewModelTest.kt`

**Interfaces:**
- Consumes: `CameraState` and `TransitMapViewportInfo`.
- Produces: normalized non-wrapping request boxes from a viewport, and updates `cachedBounds` only after a successful repository response.

- [ ] **Step 1: Write failing viewport tests** for current camera padding/bearing/pitch retention, latitude clamping, and an antimeridian-wrapping viewport split into two non-inverted longitude boxes.
- [ ] **Step 2: Write failing ViewModel tests** asserting a failed request does not update cache and a subsequent viewport event retries it; assert successful load produces a source for every default config type.
- [ ] **Step 3: Run the focused tests**

Run: `./gradlew :core:mapbox:testDebugUnitTest :app:testDebugUnitTest --tests "*TransitViewportBoundsTest" --tests "*HomeContainerViewModelTest"`

Expected: FAIL because bounds use a flattened camera and cache is written before the result.

- [ ] **Step 4: Preserve the actual camera state when deriving viewport bounds and make the ViewModel normalize/split buffered bounds before repository calls**

Keep the existing 35% per-side prefetch buffer, clamp latitude to `[-90, 90]`, split wrapping longitude requests, merge the returned type maps, and commit cache/loaded zoom only after a non-failed load completes.

- [ ] **Step 5: Re-run the focused tests**

Run: `./gradlew :core:mapbox:testDebugUnitTest :app:testDebugUnitTest --tests "*TransitViewportBoundsTest" --tests "*HomeContainerViewModelTest"`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/mapbox/src/main/java/com/vie/mit/mapbox/composable/TransitStopsMapScreen.kt app/src/main/java/com/vie/mit/green/ui/home/HomeContainerViewModel.kt core/mapbox/src/test/java/com/vie/mit/mapbox/composable/TransitViewportBoundsTest.kt app/src/test/java/com/vie/mit/green/ui/home/HomeContainerViewModelTest.kt
git commit -m "fix: load transit stops for actual map viewport"
```

### Task 4: Full mobile verification

**Files:**
- Verify: files from Tasks 1-3

**Interfaces:**
- Consumes: completed typed API, repository mapper, and viewport handling.
- Produces: a buildable Android app whose clustered sources receive server-backed data.

- [ ] **Step 1: Run all affected unit tests**

Run: `./gradlew :core:data:testDebugUnitTest :core:mapbox:testDebugUnitTest :app:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 2: Compile the debug application**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Inspect workspace changes**

Run: `git diff --check && git status --short`

Expected: no whitespace errors; do not modify the existing user-owned `.idea` changes.
