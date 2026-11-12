"""Draw a small, original vector petal sprite for the standalone Editor effect."""
from pathlib import Path
import math
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'art/PetalRain/Sources/Textures'
DEST.mkdir(parents=True, exist_ok=True)
segments = [
    ((128, 224), (109, 196), (40, 155), (42, 99)),
    ((42, 99), (43, 43), (99, 23), (132, 49)),
    ((132, 49), (169, 21), (223, 49), (213, 105)),
    ((213, 105), (206, 161), (162, 207), (128, 224)),
]
path = 'M128 224 ' + ' '.join(f'C{b[0]} {b[1]} {c[0]} {c[1]} {d[0]} {d[1]}' for a,b,c,d in segments) + ' Z'
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256">
<defs><linearGradient id="pink" x2="0.25" y2="1"><stop stop-color="#ffd4df"/><stop offset=".5" stop-color="#f688ad"/><stop offset="1" stop-color="#b53565"/></linearGradient></defs>
<path d="{path}" fill="url(#pink)"/></svg>'''
(DEST / 'petal.svg').write_text(svg)
scale = 4
mask = Image.new('L', (256*scale, 256*scale))
points = []
for a,b,c,d in segments:
    for i in range(100):
        t = i / 100
        points.append(tuple(scale*((1-t)**3*a[k]+3*(1-t)**2*t*b[k]+3*(1-t)*t*t*c[k]+t**3*d[k]) for k in (0,1)))
ImageDraw.Draw(mask).polygon(points, fill=255)
mask = mask.resize((256,256), Image.Resampling.LANCZOS)
image = Image.new('RGBA', (256,256))
for y in range(256):
    for x in range(256):
        t = min(1, max(0, (y-28)/196))
        colors = ((255,212,223),(246,136,173),(181,53,101))
        a,b = colors[:2] if t < .5 else colors[1:]
        u = t*2 if t < .5 else (t-.5)*2
        # Slight center fold, baked into the neutral-multiplier sprite.
        shade = 1-.08*math.exp(-((x-132-(y-100)*.08)/12)**2)
        rgb = tuple(round((a[k]*(1-u)+b[k]*u)*shade) for k in range(3))
        image.putpixel((x,y), (*rgb,mask.getpixel((x,y))))
image.save(DEST / 'petal.png')
print(DEST / 'petal.png')
