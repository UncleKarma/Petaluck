# RoseTear layered albedo v5

ImageGen used the two v4 raster masters as edit targets and produced the v5 files for export. Four columns are color families and three rows are outer, middle and inner layers. The root is at the bottom center of each field and the rim is at the top. The exporter crops each field with a two-pixel inset and resizes it to 512 × 512 RGB. The runtime shares the individual layer textures. Each petal selects one of its mesh’s two UV maps and a neutral Unlit brightness factor from .86 to 1, with a .93 mean in every layer.

| Color | Local pigment |
| --- | --- |
| Red | Carmine and burgundy shoulders with a salmon-red root |
| Pink | Raspberry blush on the rim and peach-pink at the root |
| Pale pink | Shell-pink tips and a soft apricot-ivory base |
| White | Blush-pink rim patches and pale buttery yellow near the root |
| Blue enchantress | Indigo and blue-violet shoulders with a cornflower-blue root |
| Yellow | Pale lemon rim with honey-gold depth near the base |
| Orange | Apricot with a coral-pink blush and a golden-peach root |
| Lavender | Pink-mauve near the rim and a cooler pearly lavender root |

The selected colors draw on examples of white roses with pink and apricot tints and coral roses with warm yellow near the center. The blue palette is styled as a dyed blue rose. [David Austin white and cream roses](https://www.davidaustinroses.com/collections/white-cream-roses), [Top Cream](https://starrosesandplants.com/plants/top-cream/), [Rainbow Knock Out](https://starrosesandplants.com/plants/rainbow-knock-out/)

## Atlas A

The edit input was `petal-whorls-a-master-v4.png`. The selected result is `petal-whorls-a-master-v5.png`.

```text
Edit the supplied 3D rose petal base-color UV atlas. Preserve the exact canvas, cell boundaries at 25/50/75% X and 1/3 and 2/3 Y, four columns by three rows, full bleed. Each of the 12 rectangles is the entire unwrapped petal membrane. Root is BOTTOM CENTER inside each cell and the petal rim is TOP edge inside each cell, upper side edges are also visible. Add perceptible but restrained color variation (localized in the outer 12–23% of the petal height near the rim or around the root). Make the border of pigment organically irregular with broad feathered diffusion along faint veins. Asymmetric little pools or blushes of pigment spread within the membrane. Keep the established main hue occupying at least 75% of each cell and the existing natural petal microtexture. The top row is lighter, the middle row intermediate and the bottom row deeper in each column, retain the existing average tonal separation but not harsh. No stripes, rigid lines within tiles, repetitive centered linear gradient, bright uniform edging, paper, aging, glitter, lighting, specular, silhouette, text or padding. All cells remain individually textured and seamless within the field. Column 1 RED: true rich red with tiny burgundy/carmine infusions near one rim shoulder and a soft salmon-red warm root, no big cream area. Column 2 PINK: lively rose pink with localized raspberry blush rim and a soft peach-pink root. Column 3 PALE PINK: delicate pale pink with localized shell-pink tips and buttercream/apricot barely feathering into the lower middle. Column 4 WHITE: maintain mostly neutral pearly white, give each layer visible but very pale warm blush PINK restricted to irregular areas at the upper rim and a small pale butter YELLOW/ivory transition feathered upward from the bottom center. Especially preserve the white identity. The white middle membrane stays white pearl, retain its relative luminance in each row and do not turn gray or purple. Make both localized accent colors noticeable without dominating.
```

## Atlas B

The edit input was `petal-whorls-b-master-v4.png`. The selected result is `petal-whorls-b-master-v5.png`.

```text
Edit the supplied 3D rose petal base-color UV atlas. Preserve the exact canvas, cell boundaries at 25/50/75% X and 1/3 and 2/3 Y, four columns by three rows, full bleed. Each of the 12 rectangles is the entire unwrapped petal membrane. Root is BOTTOM CENTER inside each cell and the petal rim is TOP edge inside each cell, upper side edges are also visible. Add perceptible but restrained color variation (localized in the outer 12–23% of the petal height near the rim or around the root). Make the border of pigment organically irregular with broad feathered diffusion along faint veins. Asymmetric little pools or blushes of pigment spread within the membrane. Keep the established main hue occupying at least 75% of each cell and the existing natural petal microtexture. The top row is lighter, the middle row intermediate and the bottom row deeper in each column, retain the existing average tonal separation but not harsh. No stripes, rigid lines within tiles, repetitive centered linear gradient, bright uniform edging, paper, aging, glitter, lighting, specular, silhouette, text or padding. All cells remain individually textured and seamless within the field. Column 1 BLUE ENCHANTRESS: a dyed cobalt-blue rose, rich royal blue over most of each cell with localized indigo/blue-violet pooling near a rim shoulder and a brighter cornflower-blue area spreading narrowly from the root. Keep cobalt, never green or cyan. Column 2 YELLOW: golden yellow dominant with a soft pale lemon/cream rim where the tissue is thin and a rich honey/amber base with slight off-center variation. Column 3 ORANGE: apricot orange dominant, warm golden-peach root and a visible restrained coral-pink blush in part of the rim, fading irregularly into apricot. Column 4 LAVENDER: distinct lilac dominant with subtle pink-mauve pooling at one rim side and a cool pearly lavender root. Keep the white and yellow families entirely out of this lavender cell.
```
