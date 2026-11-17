# Gaze selection investigation — 2026-11-17

Follow-up: user requested the previous low-poly meshes as collision resources.
That experiment is delivered in 1.0.27; see `petal-collision-v1.md`. Physical gaze
improvement and the original root cause remain unverified.

User reports that middle/inner petals are difficult to select with GazePinch on
1.0.26/code27, while DirectPinch still works. **Root cause and a fix remain unverified.**

The first observed round (seed -1429878035845074295, N=26, color=3) logged
GazePinch plucks of petals 1,3,2,0,18,11,17,4,10,5,16,9,19,8, then a
RayBasedPinch pluck of 13 after a long gap. IDs 0–11 are outer, 12–20 middle,
21–25 inner for this count. No inner Gaze selection was recorded in that excerpt.
`RoundInputReady` reported `completionEnabled=false`; the replay completion surface
is therefore not established as this round's blocker.

The recent refinement increases render and exact static collider triangles from
412/476 to 1496/1736 per petal. This correlation does **not** establish causation.

## Native collision probe

A temporary diagnostic build retained production meshes, colliders, transforms,
and gesture routing. For each petal, 48 triangles distributed through the GLB
index buffer were sampled across front/back/edge surfaces. Each native
`Scene.rayCast` began 5 mm along the triangle's outward normal, pointed toward
its centroid, and extended 10 mm. `ALL` hits and all collision groups were used,
with the mesh entity as the reference frame; a pass required that same entity
in the results. Each petal was checked on a separate real scene frame after Ready.

Device PB311XHGKC150135G, 19:25:20: seed 362410535322853215, N=22, color=2.
All twelve variants were present. All 22 petals returned **48/48**, totaling
**1056/1056** successful native ray probes. Ready and optional audio/rain
preparation also succeeded.

This is evidence against missing collision surfaces at the sampled positions.
It does not test real eye tracking, native gaze target ranking, first-visible-hit
occlusion, gesture recognition, or every triangle. An ALL-hit short ray is not
equivalent to GazePinch selection. No performance conclusion is drawn.

Diagnostic source, generated probe data, build APK, log and hashes are preserved
in `backups/gaze-hit-v1-2026-11-17/`. Temporary source/assets were removed; the
reviewed production 1.0.26 APK was restored (SHA-256
`3cdf2c94e73d7e2d977b71eb9be1c5d129b5b02871da410598775292f92c8e7b`).
No production behavior or collision approximation was changed.

## Remaining evidence

Need a physical retest indicating whether gaze hover appears on the intended
middle/inner petal, highlights another target, or never appears, and whether
the difficulty persists after surrounding outer petals are removed. That
distinguishes target acquisition/occlusion from a recognized target whose pinch
does not begin. Existing DirectPinch movement and selection should be preserved.
