# RoseTear icon artwork

Input: `/petaluck.png` (user-provided original).

`rose-tear-foreground.png` is a compact transparent foreground study made with the built-in ImageGen tool. The generated master was copied into this workspace without modifying the source image. It is an optional alternative, not the current PICO launcher resource.

Current application icon configuration, already present when work resumed:

- `app/src/main/res/mipmap-xxxhdpi/ic_spatial_launcher.png`
- `app/src/main/res/drawable/icon_3d_layer_0.png` and `icon_3d_layer_1.png`
- `app/src/main/res/drawable/icon_3d_sdf_0.png` and `icon_3d_sdf_1.png`
- Application icon/roundIcon: `@mipmap/ic_spatial_launcher`; PICO metadata references the layer and SDF arrays in `values/drawables_3d.xml`.

These active resources retain the original roseTear artwork and were preserved. No Host, SDK, or editor installation files were changed.

ImageGen prompt:

> Use case: compositing / precise-object-edit. Asset type: Android adaptive launcher icon foreground for the FlowerTears app. Input image is the edit target and visual identity reference. Adapt its existing pink rose-like flower with short green stem and its floating heart-shaped pink petal into one compact centered icon composition. Preserve the original soft realistic 3D sculptural look, warm delicate pink petals, golden stamens, green sepals and short stem, and recognizable heart-shaped detached petal. Place the flower in the lower-left part of the compact group and bring the floating heart petal closer just above-right of the flower, so they read together at a tiny launcher size. Enlarge and simplify only as needed for strong legibility, maintaining the source artwork identity and gentle lighting. The entire two-object group must fit within the central safe circle of the square canvas: circle centered at (50%,50%) with diameter 60% of the canvas. Keep generous genuinely transparent padding on all sides; do not let objects or shadows extend outside this safe circle. Output one square transparent PNG foreground asset, not an icon grid or a device mockup. No background scene, no baked icon tile, no rounded-square outline, no border, no lettering, no watermark, no new objects. Retain genuine transparency, with clean antialiased edges.

Generated foreground: 1254 × 1254 RGBA. Visually reviewed against a peach background. Android/PICO launcher rendering remains unverified on device.
