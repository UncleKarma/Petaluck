# Low-poly petal collision v1

User requested retaining the improved detailed petals while using the preceding
low-poly variants for collision, following persistent difficulty selecting middle
and inner petals with GazePinch. Implemented in 1.0.27 / code28, 2026-11-17.

`rose/*.glb` remains the byte-identical rose-llm-v10 display library. Each
`rose/collision/<variant>.glb` is a byte-identical copy from commit `95872b9`
(rose-llm-v9: gentle outer curl, before edge subdivision). There are twelve
one-to-one variant pairs: outer/middle colliders have 412 triangles, inner 476,
versus the displayed 1496/1736. The collision manifest records provenance,
hashes, triangle counts and bounds independently of the existing render manifest.

`RoseScene.ensureAssets` loads collision source hierarchies once per scene. They
never enter SpatialView and are never rendered or given interaction components.
Assembly obtains the matching source's unique ModelComponent mesh and calls
`ShapeResource.createStaticMesh` on it. The resulting shape stays on the existing
high-poly mesh entity with the existing Collision/Interactable/Hover components;
all gesture target IDs, entrance, detach/reparent, capture, release and fall paths
therefore keep their existing owner and transforms. Both pinch modes use this
collision shape. Shapes remain round-owned and globally retained until existing
round cleanup; collision sources are destroyed on scene disposal. Partial source
loads are retained and missing variants remain retryable as with render sources.

`python3 tools/verify_petal_collision.py` passes for all twelve pairs: hashes,
matching GLB nodes/scenes and local frames, closed directed-edge topology, correct
triangle counts, and zero error for every original coarse vertex in the detailed
mesh. The slightly different outlines between original samples are expected;
the collider follows the prior polygonal membrane. See
`docs/petal-collision-v1-verification.json`.

Verification: 139 host tests pass with zero failures/errors;
`testDebugUnitTest assembleDebug assembleDebugAndroidTest` succeeds using Android
Studio JBR. Experimental opt-in check passes; SpatialUI style check passes with
zero errors and the existing requested large-text warning. APK ZIP checks confirm
every production render GLB and all twelve collision GLBs match source bytes.

Installed and launched on PB311XHGKC150135G. Native log reports Ready with nineteen
petals, all twelve variants, `revision=low-poly-collision-v1`, and completion panel
disabled. Optional petal audio, rain and ending music also prepare successfully.
No assembly failure appears in the collected startup log. This proves native
load/assembly, **not** improved physical GazePinch selection or a DirectPinch
regression pass. Those require the user's headset retest, including a second round.
The inspected 1920×1080 spatial screenshot shows the flower visibly assembled;
it cannot establish collision alignment or gesture selection behavior.

APK SHA-256: `2adbbe255ce523ab5a4ac8f5be82233929eeecc9dcb88b0d47a164f1e2f11207`.
APK, startup log, screenshot and hash are preserved under
`backups/petal-collision-v1-2026-11-17/`.
