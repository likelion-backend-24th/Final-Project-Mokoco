# Design QA

- Source visual truth: `reference/main-unauth-no-data.png`, `reference/main-auth-with-data.png`
- Source dimensions: 688 × 919 and 682 × 919 pixels
- Implementation URL: `http://127.0.0.1:3000/`
- Implementation screenshots: unavailable because the in-app browser runtime could not initialize
- Verified states: unauthenticated server render; authenticated server render using a request cookie

## Findings

- [P0] Browser-rendered comparison evidence is unavailable.
  - Evidence: both reference images opened successfully. The in-app browser failed again with `failed to write kernel assets: 지정된 경로를 찾을 수 없습니다. (os error 3)`.
  - Impact: exact rendered typography, wrapping, spacing, and responsive fidelity cannot be signed off.
  - Fix: restore the in-app browser runtime, capture both states at the matching reference widths, compare source and implementation together, and iterate on visible differences.

- [P1] Authenticated reference includes post photographs, helper profiles, and chats that the current backend contract does not provide.
  - Evidence: `GET /posts` returns id, title, content, authorEmail, status, createdAt, and updatedAt only. There is no image, category, helper profile, rating, or chat payload.
  - Resolution: the implementation renders only real post fields and derived counts from those posts. It does not fabricate photos, helper identities, ratings, or chats.

## Required Fidelity Surfaces

- Layout: separate unauthenticated and authenticated structures match the references' main sections.
- Typography and color: compact Korean typography, blue primary actions, pale blue accents, white cards, subtle borders, and low elevation follow the references.
- Assets: UI imagery uses the existing Phosphor icon library. No dummy raster content or copied screenshot crops are used.
- Data: request lists use `GET /posts`; authenticated activity counts are derived from the real response and current login email.
- Location: authenticated screen keeps the region fetch/update flow and exposes the saved region in the reference-style location bar.

## Functional Verification

- `npm.cmd test`: passed, 4 tests, including the 1km automatic-location accuracy boundary.
- `npm.cmd run lint`: passed.
- `npm.cmd run build`: passed.
- Unauthenticated `/`: HTTP 200 and expected main copy present.
- Authenticated `/` with login cookie: HTTP 200; region bar, request section, and activity summary present.
- Visual capture and console inspection: blocked by the in-app browser initialization failure.

## Location Confirmation Flow

- Automatic coordinates are no longer persisted immediately.
- Accuracy at or below 1km opens a confirmation map before saving.
- Accuracy above 1km opens a warning state with the browser estimate and its accuracy circle; saving remains disabled until the user clicks the map.
- A manual map click replaces the estimated position and enables explicit confirmation.
- The flow includes retry guidance and a mobile GPS recommendation.
- Map rendering uses Leaflet with OpenStreetMap tiles; runtime tile loading requires internet access.

## Comparison History

- Source images inspected at original resolution.
- First implementation pass completed and layout breakpoint adjusted so the 688/682-pixel reference widths retain the desktop composition.
- Second visual comparison is blocked until browser capture works.

final result: blocked
