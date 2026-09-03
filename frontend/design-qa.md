# Design QA

- Source visual truth: `reference/main.png`
- Authentication references: `reference/login.png`, `reference/signup.png`
- Implementation URL: `http://127.0.0.1:3000/`
- Implementation screenshot: unavailable because the in-app browser runtime could not initialize
- Intended comparison viewport: 1450 × 1086 CSS pixels at device scale factor 1
- Source dimensions: 1450 × 1086 pixels
- Implementation dimensions: unavailable
- Density normalization: not performed because no browser-rendered screenshot could be captured
- State: logged-out main screen; backend-unavailable error state

## Findings

- [P0] Browser-rendered comparison evidence is unavailable.
  - Location: full main screen and authentication screens.
  - Evidence: the source images opened successfully, but the in-app browser runtime failed while initializing its local assets. HTTP responses and production build succeeded, but those do not substitute for visual evidence.
  - Impact: typography, exact spacing, responsive behavior, and visual fidelity cannot be signed off against the references.
  - Fix: restore the in-app browser runtime, capture `/`, `/login`, and `/signup`, then compare the main screen at 1450 × 1086.

## Required Fidelity Surfaces

- Fonts and typography: implemented with Pretendard-compatible system fallbacks; browser comparison blocked.
- Spacing and layout rhythm: implemented from the reference proportions; browser comparison blocked.
- Colors and visual tokens: blue, white, slate, thin borders, rounded cards, and low elevation follow the references; browser comparison blocked.
- Image quality and asset fidelity: the logged-out flow requires no raster content from the backend. UI icons use Phosphor rather than handcrafted SVG or CSS art.
- Copy and content: Korean logged-out, login, signup, loading, empty, and backend-error copy is present; HTTP-rendered content checks passed.

## Full-view Comparison Evidence

Blocked. The source was inspected, but an implementation screenshot could not be captured.

## Focused Region Comparison Evidence

Blocked for header, hero, request list, login form, and signup form because no implementation screenshot is available.

## Primary Interactions Tested

- Main, login, signup, and registration-success URLs returned HTTP 200.
- Login proxy returned HTTP 503 with a structured Korean message while the Gateway was offline.
- Backend `/posts` connection was attempted and the logged-out main screen rendered the explicit server-unavailable state.
- Browser console inspection was blocked with the browser runtime.

## Comparison History

- No visual fix iteration was possible without the implementation capture.

## Implementation Checklist

- Restore in-app browser capture.
- Compare the logged-out main screen at the source viewport.
- Exercise login and signup with user-service, post-service, Gateway, and databases running.
- Re-run this QA and resolve any P0/P1/P2 mismatch.

final result: blocked
