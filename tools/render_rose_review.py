"""Offline mesh review, with actual UV/base-color sampling. Not a headset capture.

Default rendering preserves the earlier diagnostic lighting; --materials reviews Unlit.
Requires numpy and Pillow. glTF UV image origin: top-left, base color texture: sRGB.
"""
import argparse
import io
from functools import lru_cache
import json
import math
from pathlib import Path
from tempfile import TemporaryDirectory
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from create_review_assemblies import ROOT, DEST, read_glb, mesh_data, material_texture


def srgb_to_linear(rgb):
    return np.where(rgb<=.04045,rgb/12.92,((rgb+.055)/1.055)**2.4)


def linear_to_srgb(rgb):
    return np.where(rgb<=.0031308,12.92*rgb,1.055*np.maximum(rgb,0)**(1/2.4)-.055)


@lru_cache(maxsize=4)
def decode_texture(raw):
    return srgb_to_linear(np.array(Image.open(io.BytesIO(raw)).convert('RGB'),dtype=float)/255)


def meshes(path):
    doc, data = read_glb(path)
    for m in doc['meshes']:
        p = m['primitives'][0]
        v, f, uv = mesh_data(doc, data, p)
        mat=doc['materials'][p['material']]
        color = mat['pbrMetallicRoughness']['baseColorFactor'][:3]
        raw=material_texture(doc,data,mat)
        yield np.array(v), np.array(f), np.array(color,dtype=float), np.array(uv), decode_texture(raw) if raw else None


def render(path, direction, size=(580,650), unlit=False, scale=1550, center=(0.,-.025,0.)):
    width, height = size
    toward = np.array(direction,dtype=float)
    toward /= np.linalg.norm(toward)
    up = np.array([0.,1.,0.]) if abs(toward[1])<.95 else np.array([0.,0.,-1.])
    right = np.cross(up,toward);right /= np.linalg.norm(right)
    up = np.cross(toward,right)
    center = np.array(center)
    pixels = np.full((height,width,3),[235.,233.,228.])
    depth = np.full((height,width),-np.inf)
    light = np.array([-.3,.65,1.]);light /= np.linalg.norm(light)
    for vertices, faces, color, uvs, texture in meshes(path):
        points = vertices-center
        screen = np.stack([points@right*scale+width/2,height/2-points@up*scale,points@toward],axis=1)
        for ids in faces:
            a,b,c = screen[ids]
            xmin=max(0,int(math.floor(min(a[0],b[0],c[0]))));xmax=min(width-1,int(math.ceil(max(a[0],b[0],c[0]))))
            ymin=max(0,int(math.floor(min(a[1],b[1],c[1]))));ymax=min(height-1,int(math.ceil(max(a[1],b[1],c[1]))))
            if xmin>xmax or ymin>ymax:continue
            denom=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(denom)<1e-8:continue
            y,x=np.mgrid[ymin:ymax+1,xmin:xmax+1];x=x+.5;y=y+.5
            wa=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/denom
            wb=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/denom
            wc=1-wa-wb
            z=wa*a[2]+wb*b[2]+wc*c[2]
            mask=(wa>=0)&(wb>=0)&(wc>=0)&(z>depth[ymin:ymax+1,xmin:xmax+1])
            if not np.any(mask):continue
            brightness=1.
            if not unlit:
                va,vb,vc=vertices[ids]
                normal=np.cross(vb-va,vc-va);normal/=max(np.linalg.norm(normal),1e-12)
                if normal@toward<0:normal=-normal
                brightness=.53+.47*max(0,normal@light)
            base=np.broadcast_to(color,(np.count_nonzero(mask),3)).copy()
            if texture is not None:
                ta,tb,tc=uvs[ids]
                uv=wa[mask,None]*ta+wb[mask,None]*tb+wc[mask,None]*tc
                # Bilinear sampling of sRGB-decoded texels, clamp-to-edge like the assets.
                h,w=texture.shape[:2];xy=np.clip(uv,0,1)*[w-1,h-1]
                lo=np.floor(xy).astype(int);hi=np.minimum(lo+1,[w-1,h-1]);frac=xy-lo
                fx=frac[:,0,None];fy=frac[:,1,None]
                tex=(texture[lo[:,1],lo[:,0]]*(1-fx)+texture[lo[:,1],hi[:,0]]*fx)*(1-fy)+(texture[hi[:,1],lo[:,0]]*(1-fx)+texture[hi[:,1],hi[:,0]]*fx)*fy
                base*=tex
            pixels[ymin:ymax+1,xmin:xmax+1][mask]=np.clip(linear_to_srgb(base*brightness)*255,0,255)
            depth[ymin:ymax+1,xmin:xmax+1][mask]=z[mask]
    return Image.fromarray(pixels.astype('uint8'))


def main():
    canvas=Image.new('RGB',(1800,1460),(245,244,240))
    draw=ImageDraw.Draw(canvas)
    font=ImageFont.load_default(size=21)
    draw.text((30,18),'FlowerTears | layered-bowl-v2 | actual GLB geometry + production Kotlin layouts',fill='#292929',font=font)
    draw.text((30,50),'Offline diagnostic lighting. No runtime, material, input or headset validation.',fill='#666666',font=font)
    views=[('FRONT',(0,.12,1)),('SIDE',(1,.12,.15)),('TOP',(0,1,.001))]
    for row,count in enumerate([17,30]):
        for col,(label,direction) in enumerate(views):
            x=col*600+10;y=100+row*675
            canvas.paste(render(DEST/f'rose_{count}.glb',direction),(x,y))
            draw.text((x+15,y+10),f'{count} petals / {label}',fill='#333333',font=font)
    output=ROOT/'art/rose-layout-v2-threeviews.png'
    canvas.save(output)
    parameters=json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    reports=[]
    for count in range(17,31):
        vertices=np.concatenate([mesh[0] for mesh in meshes(DEST/f'rose_{count}.glb')])
        lo=vertices.min(axis=0);hi=vertices.max(axis=0)
        assert np.all(lo>=np.array([-.30,-.35,-.20])) and np.all(hi<=np.array([.30,.35,.20]))
        reports.append({'count':count,'sourceAssembly':f'art/review-models/rose_{count}.glb','min':lo.tolist(),'max':hi.tolist()})
    report={'layoutVersion':parameters['layoutVersion'],'seed':20261110,'source':'Actual GLB vertices transformed with exported production Kotlin layouts','preview':str(output.relative_to(ROOT)),
            'growthAxis':[0,-math.sin(math.radians(parameters['headPitch'])),math.cos(math.radians(parameters['headPitch']))],
            'headPitch':parameters['headPitch'],'corePitch':parameters['corePitch'],'sampledAssemblies':reports,
            'nominalVolumeBounds':{'min':[-.30,-.35,-.20],'max':[.30,.35,.20]},'sampledRestMeshEnvelopeFitsNominalVolume':True,
            'runtimeVisualBoundsVerified':False,'allSeedIntersectionsVerified':False,'runtimeVisualStatus':'UNOBSERVED'}
    (ROOT/'docs/layout-v2-review.json').write_text(json.dumps(report,indent=2)+'\n')
    print(output)


def material_review():
    canvas=Image.new('RGB',(2000,1320),(245,244,240));draw=ImageDraw.Draw(canvas)
    font=ImageFont.load_default(size=22)
    draw.text((24,14),'RoseTear | botanical-basecolor-v2 | 8 palettes | actual GLB UVs + textures',fill='#292929',font=font)
    draw.text((24,47),'Offline Unlit preview / sRGB base color. No diagnostic lights. Not a headset capture.',fill='#666666',font=font)
    for i in range(8):
        x=(i%4)*500;y=90+(i//4)*610
        canvas.paste(render(DEST/f'color_{i}.glb',(0,.12,1),(490,600),True,scale=1450),(x,y))
        draw.text((x+18,y+15),f'Palette {i+1}',fill='#333333',font=font)
    path=ROOT/'art/rose-materials-v2-eight-colors.png';canvas.save(path);print(path)
    # Close views show texture detail, rather than diagnosing geometry with added lights.
    canvas=Image.new('RGB',(1800,720),(245,244,240));draw=ImageDraw.Draw(canvas)
    path=DEST/'color_1.glb'
    for col,(label,direction,scale,center) in enumerate([
        ('Bloom / front',(0,.12,1),2700,(0,.066,.015)),
        ('Bloom / side',(1,.12,.15),2700,(0,.066,.015)),
        ('Center + upper stem',(0,.12,1),4300,(0,-.025,0))]):
        source=path if col<2 else DEST/'stem_only.glb'
        canvas.paste(render(source,direction,(590,650),True,scale,center),(col*600,65))
        draw.text((col*600+18,18),label+' | offline Unlit',fill='#333333',font=font)
    path=ROOT/'art/rose-materials-v2-detail.png';canvas.save(path);print(path)


def stem_review():
    import tarfile
    # Read the previous immutable snapshot; never replace production assets for a preview.
    previous=ROOT/'backups/materials-v2-2026-11-10/RoseTear-materials-v2-2026-11-10-source.tar.gz'
    before=Path('/tmp/rose-stem-v3-before-review.glb')
    with tarfile.open(previous,'r:gz') as archive:
        before.write_bytes(archive.extractfile('VibeCodingApp/art/review-models/stem_only.glb').read())
    canvas=Image.new('RGB',(1800,1480),(245,244,240));draw=ImageDraw.Draw(canvas);font=ImageFont.load_default(size=21)
    draw.text((24,14),'RoseTear | organic-stem-v3 | curved resting mesh + one shared blossom transform',fill='#292929',font=font)
    draw.text((24,48),'Actual GLB geometry / UVs / base color. Offline Unlit. Not a headset capture.',fill='#666666',font=font)
    views=[
        ('Before / stem front',before,(0,.12,1),2000,(0,-.075,0)),
        ('After / stem front',DEST/'stem_only.glb',(0,.12,1),2000,(0,-.075,0)),
        ('After / stem side',DEST/'stem_only.glb',(1,.06,.08),2000,(0,-.075,0)),
        ('17 petals / front',DEST/'rose_17.glb',(0,.12,1),1550,(0,-.025,0)),
        ('30 petals / side',DEST/'rose_30.glb',(1,.12,.15),1550,(0,-.025,0)),
        ('Core / common growth axis',DEST/'stem_only.glb',(1,.12,.2),9000,(0,.035,0)),
    ]
    for i,(label,path,direction,scale,center) in enumerate(views):
        x=(i%3)*600+5;y=100+(i//3)*680
        canvas.paste(render(path,direction,(590,650),True,scale,center),(x,y))
        draw.text((x+14,y+8),label,fill='#333333',font=font)
        if i==5:
            toward=np.array(direction,dtype=float);toward/=np.linalg.norm(toward)
            right=np.cross([0,1,0],toward);right/=np.linalg.norm(right);up=np.cross(toward,right)
            origin=np.array([0,.04,0]);axis=np.array([0,math.cos(math.radians(35)),math.sin(math.radians(35))])
            def screen(p):
                p=p-np.array(center)
                return np.array([x+295+p@right*scale,y+325-p@up*scale])
            a=screen(origin);b=screen(origin+axis*.025);line=b-a;line/=np.linalg.norm(line);perp=np.array([-line[1],line[0]])
            draw.line([tuple(a),tuple(b)],fill='#315e91',width=4)
            draw.polygon([tuple(b),tuple(b-line*17+perp*7),tuple(b-line*17-perp*7)],fill='#315e91')
            draw.text((x+18,y+600),'55 deg above horizontal / toward +Z camera',fill='#315e91',font=ImageFont.load_default(size=19))
    path=ROOT/'art/rose-stem-v3-review.png';canvas.save(path);print(path)
    records=[]
    for count in range(17,31):
        v=np.concatenate([m[0] for m in meshes(DEST/f'rose_{count}.glb')]);lo=v.min(0);hi=v.max(0)
        assert np.all(lo>=np.array([-.30,-.35,-.20])) and np.all(hi<=np.array([.30,.35,.20]))
        records.append({'count':count,'min':lo.tolist(),'max':hi.tolist()})
    manifest=json.loads((ROOT/'app/src/main/assets/rose/manifest.json').read_text())
    report={'geometryVersion':manifest['stem']['geometryVersion'],'stemSha256':manifest['stem']['sha256'],
        'preview':str(path.relative_to(ROOT)),'baselineSnapshot':str(previous.relative_to(ROOT)),
        'coreAndPetalsShare':'CrownMotion','crownPitchDegrees':-55,'coreLocalPitchDegrees':90,
        'sharedGrowthAxis':manifest['stem']['tipAxis'],'tipTangentMatchesGrowthAxis':True,
        'restMeshFitsNominalVolumeForSampledSeed':True,'sampledRestBounds':records,
        'nativeJointPoseSpaceAndAnimatedSkinVerified':False,'allSeedIntersectionsVerified':False,'runtimeVisualBoundsVerified':False,
        'runtimeVisualStatus':'UNOBSERVED','scope':'Actual exported rest mesh and production Kotlin layout samples, offline Unlit; no headset or animated native bone validation.'}
    (ROOT/'docs/stem-v3-review.json').write_text(json.dumps(report,indent=2)+'\n')


def layered_v4_review():
    import tarfile
    font=ImageFont.load_default(size=22)
    canvas=Image.new('RGB',(2000,1320),(245,244,240));draw=ImageDraw.Draw(canvas)
    draw.text((24,14),'RoseTear | 24 separate albedos | outer lighter / middle / inner deeper',fill='#292929',font=font)
    draw.text((24,47),'Actual production layer bindings + GLB UVs. Offline Unlit, not a headset capture.',fill='#666666',font=font)
    names=['Red','Pink','Pale pink','White','Blue enchantress','Yellow','Orange','Lavender']
    for i,name in enumerate(names):
        x=(i%4)*500;y=90+(i//4)*610
        canvas.paste(render(DEST/f'color_{i}.glb',(0,.12,1),(490,600),True,1450),(x,y))
        draw.text((x+18,y+15),name,fill='#333333',font=font)
    path=ROOT/'art/rose-layered-v4-eight-colors.png';canvas.save(path);print(path)
    canvas=Image.new('RGB',(1200,760),(245,244,240));draw=ImageDraw.Draw(canvas)
    draw.text((24,12),'Blue enchantress | outer / middle / inner use different textures',fill='#292929',font=font)
    draw.text((24,46),'Offline Unlit / actual GLB mesh and material data',fill='#666666',font=font)
    for col,direction in enumerate([(0,.12,1),(1,.12,.15)]):
        canvas.paste(render(DEST/'color_4.glb',direction,(590,650),True,2700,(0,.066,.015)),(col*600,95))
    path=ROOT/'art/rose-layered-v4-blue-detail.png';canvas.save(path);print(path)
    previous=ROOT/'backups/stem-v3-2026-11-10/RoseTear-stem-v3-2026-11-10-source.tar.gz'
    before=Path('/tmp/rose-v4-previous-stem.glb')
    with tarfile.open(previous,'r:gz') as archive:
        before.write_bytes(archive.extractfile('VibeCodingApp/art/review-models/stem_only.glb').read())
    canvas=Image.new('RGB',(1800,820),(245,244,240));draw=ImageDraw.Draw(canvas)
    draw.text((24,14),'RoseTear | almost straight leaning body + small weighted bend at the top',fill='#292929',font=font)
    draw.text((24,48),'Same Unlit view / actual GLB bytes. No S-shaped sway in the lower body.',fill='#666666',font=font)
    for col,(label,path,direction) in enumerate([
        ('Before / S curve',before,(0,.12,1)),('After / front',DEST/'stem_only.glb',(0,.12,1)),('After / side',DEST/'stem_only.glb',(1,.06,.08))]):
        canvas.paste(render(path,direction,(590,700),True,2200,(0,-.075,0)),(col*600,100))
        draw.text((col*600+18,108),label,fill='#333333',font=font)
    path=ROOT/'art/rose-neck-v4-review.png';canvas.save(path);print(path)
    bounds=[]
    for count in range(17,31):
        v=np.concatenate([m[0] for m in meshes(DEST/f'rose_{count}.glb')]);lo=v.min(0);hi=v.max(0)
        assert np.all(lo>=[-.30,-.35,-.20]) and np.all(hi<=[.30,.35,.20])
        bounds.append({'count':count,'min':lo.tolist(),'max':hi.tolist()})
    report={'revision':'weighted-neck-and-layered-colors-v4','previews':['art/rose-neck-v4-review.png','art/rose-layered-v4-eight-colors.png','art/rose-layered-v4-blue-detail.png'],
        'previousSnapshot':str(previous.relative_to(ROOT)),'sampledRestBounds':bounds,'sampledRestMeshesFitNominalVolume':True,
        'method':'Actual GLB triangles/UVs and individually exported layer textures, using production Kotlin textureFile paths. CPU sRGB Unlit, no native mipmap/tone-mapper simulation.',
        'runtimeVisualStatus':'UNOBSERVED','nativeSkinAndJointPoseSpaceVerified':False,'runtimeVisualBoundsVerified':False,'allSeedIntersectionsVerified':False}
    (ROOT/'docs/layered-v4-review.json').write_text(json.dumps(report,indent=2)+'\n')


def layered_v5_review():
    import create_review_assemblies as assembly

    parameters=json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    manifest=json.loads((ROOT/'app/src/main/assets/rose/manifest.json').read_text())
    palettes=manifest['textures']['palettes']
    canvas=Image.new('RGB',(1920,1030),(245,244,240))
    draw=ImageDraw.Draw(canvas)
    font=ImageFont.load_default(size=22)
    draw.text((20,16),'RoseTear / Eight palettes with secondary pigment',fill='#292929',font=font)
    draw.text((20,48),'Offline Unlit preview of packaged GLB and albedo textures',fill='#666666',font=font)
    with TemporaryDirectory(prefix='rose-palette-v5-') as temporary:
        assembly.DEST=Path(temporary)
        for palette in palettes:
            i=palette['colorId']
            name=palette['name'].replace('-',' ').capitalize()
            config=next(c for c in parameters['rounds'] if c['colorId']==i and c['count']==24)
            path=assembly.assemble(config,parameters,f'color_{i}')
            x=i%4*480
            y=82+i//4*470
            canvas.paste(render(path,(.12,.38,1),(470,460),True,2100,(0,.084,.033)),(x,y))
            draw.text((x+14,y+8),name,fill='#333333',font=font)
    output=ROOT/'art/rose-layered-v5-eight-colors.png'
    canvas.save(output)
    print(output)


def variation_v6_review():
    import tarfile
    import create_review_assemblies as assembly

    parameters=json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    previous=ROOT/'backups/replay-palettes-v5-2026-11-11/RoseTear-replay-palettes-v5-2026-11-11-source.tar.gz'
    canvas=Image.new('RGB',(1800,1160),(245,244,240))
    draw=ImageDraw.Draw(canvas)
    font=ImageFont.load_default(size=22)
    draw.text((20,16),'RoseTear / Individual petal UV and pigment-depth variation',fill='#292929',font=font)
    draw.text((20,48),'Same exported layout, GLB geometry and albedos / offline Unlit',fill='#666666',font=font)
    with TemporaryDirectory(prefix='rose-variation-v6-') as temporary:
        before=Path(temporary)/'before'
        before.mkdir()
        assembly.DEST=Path(temporary)/'review'
        with tarfile.open(previous,'r:gz') as archive:
            for member in archive:
                if member.isfile() and member.name.startswith('app/src/main/assets/rose/'):
                    (before/Path(member.name).name).write_bytes(archive.extractfile(member).read())
        for column,(color,name) in enumerate([(1,'Pink'),(3,'White'),(6,'Orange')]):
            config=next(c for c in parameters['rounds'] if c['count']==24 and c['colorId']==color)
            old_config={**config,'petals':[{**p,'uvIndex':0,'albedoScale':1} for p in config['petals']]}
            old_path=assembly.assemble(old_config,parameters,f'before_{color}',before)
            new_path=assembly.assemble(config,parameters,f'after_{color}')
            for row,(path,label) in enumerate([(old_path,'Before'),(new_path,'After')]):
                x=column*600
                y=84+row*530
                canvas.paste(render(path,(.12,.38,1),(590,520),True,2560,(0,.084,.033)),(x,y))
                draw.text((x+14,y+8),f'{name} / {label}',fill='#333333',font=font)
    output=ROOT/'art/rose-petal-variation-v6-review.png'
    canvas.save(output)
    print(output)


def layout_review(cupped=False):
    import create_review_assemblies as assembly

    parameters=json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    canvas=Image.new('RGB',(1800,1400),(245,244,240))
    draw=ImageDraw.Draw(canvas)
    font=ImageFont.load_default(size=22)
    heading='Cupped middle and inner petals' if cupped else 'Seated roots and spiral petal overlap'
    draw.text((20,16),f'RoseTear / {heading}',fill='#292929',font=font)
    draw.text((20,48),'Production Kotlin layouts and exported GLB triangles / offline Unlit',fill='#666666',font=font)
    with TemporaryDirectory(prefix='rose-layout-v3-') as temporary:
        assembly.DEST=Path(temporary)
        for row,count in enumerate((17,30)):
            config=next(c for c in parameters['rounds'] if c['count']==count and c['seed']==20261110)
            whole=assembly.assemble(config,parameters,f'rose_{count}')
            inner={**config,'petals':[p for p in config['petals'] if p['variant'].startswith('inner')]}
            cutaway=assembly.assemble(inner,parameters,f'inner_{count}')
            views=[
                ('Full flower / front',whole,(0,.25,1),2300,(0,.057,.022)),
                ('Full flower / side',whole,(1,.08,.15),2300,(0,.057,.022)),
                ('Inner petals + core',cutaway,(.65,.15,1),4900,(0,.061,.016)),
            ]
            for column,(label,path,direction,scale,center) in enumerate(views):
                x=column*600
                y=86+row*650
                canvas.paste(render(path,direction,(590,630),True,scale,center),(x,y))
                draw.text((x+14,y+8),f'{count} petals / {label}',fill='#333333',font=font)
    filename='rose-cupped-layout-v4-review.png' if cupped else 'rose-seated-layout-v3-review.png'
    output=ROOT/'art'/filename
    canvas.save(output)
    print(output)


def natural_layout_review(interleaved=False):
    import create_review_assemblies as assembly

    parameters=json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    canvas=Image.new('RGB',(1800,1400),(245,244,240))
    draw=ImageDraw.Draw(canvas)
    font=ImageFont.load_default(size=22)
    heading='Interleaved petal openings' if interleaved else 'Cupped outer petals and varied arrangements'
    draw.text((20,16),f'RoseTear / {heading}',fill='#292929',font=font)
    draw.text((20,48),'Three 24-petal pink roses / production layouts and GLB textures / offline Unlit',fill='#666666',font=font)
    with TemporaryDirectory(prefix='rose-layout-v5-') as temporary:
        assembly.DEST=Path(temporary)
        for column,seed in enumerate((0,7,19)):
            config=next(c for c in parameters['rounds'] if c['count']==24 and c['seed']==seed and c['colorId']==1)
            path=assembly.assemble(config,parameters,f'rose_{seed}')
            for row,(label,direction) in enumerate([
                ('Front',(0,.25,1)),('Side',(1,.08,.15)),
            ]):
                x=column*600
                y=86+row*650
                canvas.paste(render(path,direction,(590,630),True,2300,(0,.057,.022)),(x,y))
                draw.text((x+14,y+8),f'Seed {seed} / {label}',fill='#333333',font=font)
    filename='rose-interleaved-layout-v6-review.png' if interleaved else 'rose-natural-layout-v5-review.png'
    output=ROOT/'art'/filename
    canvas.save(output)
    print(output)


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--materials',action='store_true')
    parser.add_argument('--stem',action='store_true')
    parser.add_argument('--v4',action='store_true')
    parser.add_argument('--v5',action='store_true')
    parser.add_argument('--v6',action='store_true')
    parser.add_argument('--layout-v3',action='store_true')
    parser.add_argument('--layout-v4',action='store_true')
    parser.add_argument('--layout-v5',action='store_true')
    parser.add_argument('--layout-v6',action='store_true')
    args=parser.parse_args()
    if args.layout_v6:
        natural_layout_review(interleaved=True)
    elif args.layout_v5:
        natural_layout_review()
    elif args.layout_v4:
        layout_review(cupped=True)
    elif args.layout_v3:
        layout_review()
    elif args.v6:
        variation_v6_review()
    elif args.v5:
        layered_v5_review()
    elif args.v4:
        layered_v4_review()
    elif args.stem:
        stem_review()
    elif args.materials:
        material_review()
    else:
        main()
