# RoseTear layered albedo v4

Generated with the built-in ImageGen tool, no API/CLI fallback. This revision replaces cream/yellow-white (colorId 4) with blue-enchantress; white (colorId 3) stays. Each of eight colors has three separately authored texture fields, outer → middle → inner becoming gently deeper. These are new production raster masters; the earlier source images remain historical. The [refinement prompts](refinement-v4.md) describe the final selected versions.

Saved sources:
- `art/textures/petal-whorls-a-master-v4.png`: columns red / pink / pale pink / white.
- `art/textures/petal-whorls-b-master-v4.png`: columns blue-enchantress / yellow / orange / lavender.
- Each source has three rows: outer / middle / inner. Crop each field with a 2-source-pixel inset, resize to 512² RGB PNG; no procedural hue or brightness editing. Runtime uses separate white-factor Unlit materials and the corresponding layer texture.
- Existing `petal-neutral-master-v2.png` and `stem-atlas-master-v2.png` stay unchanged.

Both inputs are reference-only: `art/textures/petal-palettes-master-v2.png` and `requirements/model-references/rose-single-petal-reference.png`. They supply material appearance and UV orientation, not an edit to the original references.

## Final atlas A prompt

```text
Use case: photorealistic-natural.
Asset type: one production base-color UV atlas for a 3D rose, exact FOUR columns by THREE rows, wide 4:3 canvas. Twelve equally sized square rectangular full-bleed texture fields, no gaps, borders, labels, text, silhouettes, empty background, shadows or directional lighting. All straight field boundaries at x=25%,50%,75% and y=33.333%,66.667%.
The input images are references only: previous albedo palette and the fresh petal's supple botanical surface. Each field is a continuous flat unwrapped petal membrane, extending to every corner, NOT a cutout petal photograph.
In EVERY column the TOP ROW is the OUTER petal layer (a little lighter), MIDDLE ROW is the MIDDLE petal layer (medium depth), BOTTOM ROW is the INNER petal layer (a little deeper and richer). The three fields in each column MUST be separately authored, with subtly different irregular branching veins and pigmentation, not three identical copied images. Keep the transition soft and natural when assembled into one rose; inner/outer must clearly differ in average color depth, while retaining the same color family.
UV orientation of EACH square field: narrow attachment root at BOTTOM CENTER, outer rim at TOP. Faint branching capillaries fan out from the root. Subtle fresh velvet epidermis and very fine cells; smooth hydrated living rose tissue. A tiny softly lighter root zone only in the bottom 8-12 percent. No paper wrinkles, dried spots, thick vein ridges, crumples, weathering, glitter, glossy highlights or baked shadows. Albedo only.
FOUR COLOR COLUMNS from left to right:
Column 1 RED ROSE: outer soft rose-red around RGB 216,103,118; middle rich red around 197,57,79; inner deeper carmine around 161,40,61. Root has a restrained warm blush gradient, no large yellow patch.
Column 2 PINK ROSE: outer soft rose-pink around 240,163,176; middle around 229,119,141; inner deeper pink around 200,83,117.
Column 3 PALE PINK ROSE: outer luminous pale blush around 247,205,214; middle around 242,181,195; inner richer blush around 219,143,166.
Column 4 WHITE ROSE (KEEP WHITE): outer pearl-white around 249,248,246; middle soft neutral white around 236,237,239; inner softly deeper pearl around 216,219,225. All stay white/pearl, NOT brown, yellow, pink or lavender. Root only faint warm ivory.
Make all 12 squares align precisely. No color mixing between columns. TOP is lighter OUTER, BOTTOM is deeper INNER.
```

## Final atlas B prompt

```text
Use case: photorealistic-natural.
Asset type: one production base-color UV atlas for a 3D rose, exact FOUR columns by THREE rows, wide 4:3 canvas. Twelve equally sized square rectangular full-bleed texture fields, no gaps, borders, labels, text, silhouettes, empty background, shadows or directional lighting. All straight field boundaries at x=25%,50%,75% and y=33.333%,66.667%.
The input images are references only: previous albedo palette and the fresh petal's supple botanical surface. Each field is a continuous flat unwrapped petal membrane, extending to every corner, NOT a cutout petal photograph.
In EVERY column the TOP ROW is the OUTER petal layer (a little lighter), MIDDLE ROW is the MIDDLE petal layer (medium depth), BOTTOM ROW is the INNER petal layer (a little deeper and richer). The three fields in each column MUST be separately authored, with subtly different irregular branching veins and pigmentation, not three identical copied images. Keep the transition soft and natural when assembled into one rose; inner/outer must clearly differ in average color depth, while retaining the same color family.
UV orientation of EACH square field: narrow attachment root at BOTTOM CENTER, outer rim at TOP. Faint branching capillaries fan out from the root. Subtle fresh velvet epidermis and very fine cells; smooth hydrated living rose tissue. A tiny softly lighter root zone only in the bottom 8-12 percent. No paper wrinkles, dried spots, thick vein ridges, crumples, weathering, glitter, glossy highlights or baked shadows. Albedo only.
FOUR COLOR COLUMNS from left to right:
Column 1 BLUE ENCHANTRESS ROSE: rich saturated royal-blue rose petal, no yellow, cream, green, teal or cyan. OUTER row gently lighter cobalt/cornflower blue around RGB 104,145,221. MIDDLE row deep royal blue around RGB 54,96,184. INNER row richer dark sapphire around RGB 30,57,132. Retain supple rose texture and subtle blue pigment mottling; avoid a metallic/neon look. The small root zone is softer pearl-blue, NOT yellow or ivory.
Column 2 YELLOW ROSE: outer light golden yellow around RGB 247,215,134; middle around 237,192,79; inner warm richer gold around 216,165,51. Root only a little paler warm yellow, not pink.
Column 3 PEACH ORANGE ROSE: outer soft peach around RGB 246,183,143; middle apricot-orange around 236,151,108; inner richer warm coral-orange around 213,121,80.
Column 4 LAVENDER ROSE: outer pale lilac around RGB 219,199,234; middle lavender around 197,165,217; inner richer soft purple around 167,130,196.
Make all 12 squares align precisely. No color mixing between columns. TOP is lighter OUTER, BOTTOM is deeper INNER. Each layer field has a different fine branching vein pattern; fresh smooth membrane without creases.
```

