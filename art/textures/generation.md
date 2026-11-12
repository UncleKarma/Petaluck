# RoseTear natural base color textures v2

2026-11-10. Generated with the built-in ImageGen tool (no API/CLI fallback). New project-bound raster sources; not cloud 3D generation. Existing LLM-authored mesh geometry and provenance remain separate.

Sources:
- `art/textures/petal-neutral-master-v2.png`: neutral petal reference embedded in the reusable mesh prototypes.
- `art/textures/petal-palettes-master-v2.png`: final app petal albedo, eight full-color fields; 4 columns × 2 rows, red/pink/pale-pink/white/cream/yellow/orange/lavender. Each field maps from narrow root (image bottom) to broad rim (image top).
- `art/textures/stem-atlas-master-v2.png`: top-left stem, top-right sepal, bottom-left pollen, bottom-right filament.

Prompt history (the refinement below produces the final petal master; the first version was rejected for dry, paper-like edges):

## Petal neutral master

```text
Use case: photorealistic-natural.
Asset type: production UV base-color/albedo texture for a 3D rose petal, neutral tintable master.
Create a square 1024x1024 opaque image, entirely filled edge to edge by a single continuous fresh ivory rose petal membrane. This is an UNWRAPPED rectangular texture map, not a photograph of a cutout petal. No silhouette, border, background, labels, grid, text, shadow, specular highlight, gloss, or lighting baked into the image. All corners must contain petal tissue.
UV composition: top of image is the broad outer rim, bottom is the narrow attachment root. Extremely fine, softly visible irregular branching veins radiate from the bottom center toward the entire upper edge and sides, spaced like real rose petal venation, never straight stripes. Fresh living velvety epidermis, delicate tiny translucent cells and very subtle pigmentation mottling. Ivory throughout, cool pale gray-cream in the upper part, softly warmer cream at the bottom center, gently deeper values along the upper rim and lateral margins. Broad organic soft tonal variation must be visible even at a small scale; preserve a luminous midbody. No large folds, crumples, ridges, scratches, paper fibers, cracks, brown spots or disease. Texture should be non-directionally lit, photographic botanical scan quality, restrained organic detail suitable for multiplying with red, pink, white, yellow, orange, lavender palette colors. Avoid perfectly uniform gradients or synthetic wave patterns.
```

## Final petal refinement

Built-in ImageGen edit. Target: initial petal master; material reference: `requirements/model-references/rose-single-petal-reference.png`. Only the refined source is exported to the app.

```text
Use case: precise-object-edit.
Asset type: neutral tintable base color UV texture for a fresh living rose petal in an Unlit 3D application.
Image 1 is the edit target (the rectangular ivory membrane texture). Image 2 is a MATERIAL reference only: match the fresh, supple, softly translucent rose petal's delicate branching veins, not its pink hue, outline or background.
Revise image 1 into a smoother, FRESH silky living petal surface. Remove all paper-like wrinkles, creases, folds, rough fibers and gray-yellow/brown weathered edges, especially the crumpled strip along the upper image edge. This must look like a hydrated living flower membrane, not parchment, paper, fabric or dried leaves.
Preserve the fully opaque square image filled entirely edge-to-edge with petal tissue and no outside background, no silhouette. Preserve vein growth radiating subtly from bottom center toward top and sides, but use softly submerged, thin branching capillaries like the reference, with extremely fine epidermal cells, not raised rope veins.
Palette is nearly achromatic pearl-white and pale cool gray (NOT beige): bright luminous body near RGB 235,240,242. Subtle broader neutral gray pigmentation gradients toward upper rim/sides near RGB 195,200,203, with smooth transitions and irregular organic softly washed patches. The bottom center 15 percent can fade to very pale warm cream near RGB 247,240,217, remaining luminous. Zero brown and zero dry yellow mottling elsewhere.
No directional light, hard shadows, specular highlights, baked folds, edge outlines, text, border or objects. Production albedo scan. Only restrained soft pigment gradients and subtle capillary/cellular texture, fresh silk-soft botanical material.
```

## Resource export and review

`tools/generate_rose_assets.py` only crops atlas fields, converts and resizes generated sources to RGB PNG: petals 512 × 512, stem atlas 1024 × 1024. Two source pixels are excluded at palette field boundaries to prevent neighbor colors. The original masters remain here. No procedural noise/vein replacement or offline hue manipulation is applied to the generated bitmaps. The app uses eight individually baked petal textures with white material factors, preserving ivory roots and saturated edges. Neutral material remains embedded in the reusable mesh prototypes and is replaced at runtime. Stem and sepal/filament atlas regions use white factors; pollen uses a neutral 0.88 factor to leave headroom for the existing completion brightness pulse.

Mapping follows [glTF UV and base-color conventions](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#images) and [KHR_materials_unlit](https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_unlit/README.md). Offline review samples actual exported UVs and embedded textures, decodes sRGB before multiplying the color factor, and adds no lights in Unlit mode. It is not a PICO runtime capture.

## Final eight-color petal atlas

Built-in ImageGen generation using the existing petal reference and refined neutral material reference; one packed UV atlas, exported to eight runtime textures.

```text
Use case: photorealistic-natural.
Asset type: one production rose-petal base-color UV atlas, eight color fields in a strict 4-column by 2-row grid. WIDE 2:1 canvas. Each field is the same SQUARE unwrapped petal membrane in a different natural rose color. No gutters or borders.
Image 1 is a material reference for supple fresh rose membrane and realistic root-to-rim coloration, NOT an outline/background to copy. Image 2 is a neutral UV reference: its veins fan out from bottom-center root to upper rim. Preserve that orientation in EVERY field.
Entire image must be filled edge-to-edge with the eight rectangular albedo texture fields, each occupying exactly one quarter width and one half height. All fields have precisely straight aligned boundaries at x=25%,50%,75% and y=50%. No petal silhouettes, no blank background, no labels, text, grid lines or shadows.
The four TOP ROW fields, left to right:
1) fresh deep rose-red petal, #B94150 midbody and slightly richer carmine upper edge, a small softly lighter warm apricot-ivory attachment root at the bottom center.
2) fresh rose-pink petal, #DC829A midbody, softly deeper pink upper rim and side edges, softly luminous ivory-blush attachment root at bottom center, like the first reference.
3) fresh delicate pale-pink petal, #EAB4BE body, blush pink edge, luminous near-ivory root.
4) fresh white rose petal, #F1EDE5 body, neutral pearl-white outer region, warm pale cream root. WHITE, no pink.
The four BOTTOM ROW fields, left to right:
5) cream rose, #EAD8AE body, soft buttery cream gradients, lighter ivory root. NO pink.
6) yellow rose, #E5BE55 body, softly honey-gold outer edge, pale ivory-yellow root. NO pink.
7) peach-orange rose, #DE9061 body with a gentle coral-orange upper rim, pale creamy-apricot root.
8) pale lavender rose, #B59BCB body, subtly richer lilac outer rim and pearly ivory-lavender root.
Every field: broad smooth naturally irregular pigment gradients, from a very small warm root zone in the bottom 12 percent smoothly into the colored body and richer upper edge. Faint branching translucent capillaries radiate from bottom center, very fine epidermal cells with fresh, velvety smooth hydrated skin. Keep tiny detail restrained, softly below the surface. Match color to living flower petals; texture is ALBEDO ONLY, no directional lighting, baked highlights, specular or drop shadows.
Crucially no crumpled paper, wrinkles, creases, coarse veins, dried patches, brown borders or weathering. No photographic petal outlines. Four by two full-bleed rectangular fields of continuous membrane.
```

## Stem atlas master

```text
Use case: photorealistic-natural.
Asset type: production square 1024x1024 base-color texture atlas for a fresh rose stem, sepals and flower center.
Create an opaque rectangular UV ALBEDO ATLAS, not a picture of separate objects. Four equally sized square texture fields in an exact 2 by 2 grid, no padding or borders. Every field must fill its quadrant edge to edge with biological surface texture. No labels, text, background, silhouettes, decorative dividers, highlights, reflections, shadows, or directional lighting. Flat botanical scan, natural muted colors.
TOP LEFT quadrant: continuous unwrapped fresh rose stem skin, longitudinal subtle irregular green fibers going vertically. Olive emerald and yellow-green chlorophyll variation, tiny pale lenticel speckles, smooth tender skin rather than tree bark. Upper edge a little fresher yellow-green, lower edge deeper olive green. Left and right edges should visually tile in the horizontal direction. No thorns in the image.
TOP RIGHT quadrant: fresh green rose sepal membrane, a delicate central vertical midvein and fine branching secondary veins, sage yellow-green center fading to deeper moss-green sides, tiny natural cell variation. No leaf outline.
BOTTOM LEFT quadrant: fine golden ochre rose pollen surface, warm yellow-gold fine irregular grains, slightly deeper honey patches, no green, no objects or outline.
BOTTOM RIGHT quadrant: tender rose filament tissue, pale butter ivory transitioning to light yellow-green, subtle fine longitudinal organic threads and tiny cellular surface variation.
Surface detail needs to look fresh, real and restrained. No glossy plastic, crumpled paper, thick ropes, bark cracks, water droplets, or heavy noisy contrast. Keep exactly four texture fields, aligned at image midpoint.
```

