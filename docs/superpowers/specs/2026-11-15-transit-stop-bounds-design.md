# Transit Stop Bounds Mobile Integration Design

## Intent

Replace the inactive mobile GeoJSON endpoint with the live, authenticated bounding-box APIs so Mapbox displays bus and metro stops within the current map viewport.

## Goals

- Load each supported default transit mode concurrently for the viewport.
- Convert backend stop-marker lists into valid GeoJSON FeatureCollections consumed by the existing Mapbox source/layer manager.
- Use camera bounds that match the actual padded map viewport and avoid invalid requests.
- Keep `TRAIN` and `BIKE` configurations intact for future APIs.

## Non-goals

- Add or change backend endpoints, database data, or Mapbox layer styling.
- Fabricate stops for unsupported modes.
- Fetch GBFS bike-rental stations as a substitute for a transit-stop bounds API.

## Approaches

1. Keep all four visual configurations and introduce an explicit client-side endpoint map. `BUS` and `METRO` load concurrently; unsupported `TRAIN` and `BIKE` produce empty FeatureCollections. This preserves the UI contract and prepares the data flow for later APIs. **Chosen.**
2. Remove unsupported modes from `defaultConfigs`. This avoids empty layers but changes the current feature surface.
3. Extend the backend for train and bike first. This is outside this mobile-only change and requires product/API work not requested here.

## Design

`MapApi` exposes POST methods for `/transit-stops/bus/search-in-bounds` and `/transit-stops/metro/search-in-bounds`, using a shared serializable request model with `minLat`, `minLon`, `maxLat`, and `maxLon`. Their shared response model has `id`, `name`, `latitude`, and `longitude`.

`MapRepository.getTransitStopsGeoJson` takes validated bounds and constructs one `TransitSourceData` item for every entry in `defaultConfigs`. It uses `supervisorScope` and concurrent child calls for modes with a registered endpoint. A failed mode becomes an empty FeatureCollection; if every supported request fails, the method fails so the existing UI error state remains meaningful. `TRAIN` and `BIKE` remain empty until endpoint mappings are added.

The repository converts every marker to a GeoJSON Point using `[longitude, latitude]`, with stable feature `id` and properties `stop_id`, `name`, and `type`. It returns raw FeatureCollection JSON because the existing `TransitMapStyleManager` owns actual `GeoJsonSource` creation and updates.

The viewport helper derives bounds from the actual `CameraState`, including camera padding, bearing, and pitch. Bounds are latitude-clamped and normalized before buffering. Antimeridian-wrapping bounds are not sent as an invalid inverted longitude request; the viewport is split into two request boxes before aggregation. ViewModel cache state is committed only after a successful load.

## Constraints

- Both target stop APIs require the existing JWT interceptor.
- Backend coordinates use WGS84 and accept longitude/latitude in their named JSON fields.
- Backend rejects inverted bounds; Mapbox documents that wrapped antimeridian bounds are not a minimum bounding box.
- No mock GeoJSON fallback remains for network/API failures.
