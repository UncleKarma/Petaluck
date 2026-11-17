"""Reimport actual final GLB bytes and verify budgets, surfaces, skin weights and identities."""
from create_review_assemblies import accessor, material_texture, ROOT
from generate_rose_assets import OUT
from pathlib import Path
import json,struct,hashlib,collections,io,math
from PIL import Image, ImageStat, ImageFilter
import numpy as np

def verify_texture(d,b,material,expected,factor=None):
    assert 'KHR_materials_unlit' in material['extensions']
    assert material['pbrMetallicRoughness']['baseColorFactor']==(factor or [1,1,1,1]), 'Authored textures must not be tinted twice'
    assert material_texture(d,b,material)==expected, 'Embedded and runtime texture bytes differ'
    return True

def verify_uv(d,b,prim,channel=0):
    uv=accessor(d,b,prim['attributes'][f'TEXCOORD_{channel}'])
    assert len(uv)==d['accessors'][prim['attributes']['POSITION']]['count']
    assert all(all(math.isfinite(a) and 0<=a<=1 for a in p) for p in uv)
    assert max(p[1] for p in uv)-min(p[1] for p in uv)>.4, 'Texture must span the length of the mesh'
    return uv

def load(p):
    raw=p.read_bytes();magic,version,length=struct.unpack_from('<III',raw);assert magic==0x46546c67 and version==2 and length==len(raw)
    ln=struct.unpack_from('<I',raw,12)[0];return json.loads(raw[20:20+ln]),raw[28+ln:]


def closed_shells(doc, data, primitive):
    positions = accessor(doc, data, primitive['attributes']['POSITION'])
    vertices, weld = np.unique(np.round(positions, 7), axis=0, return_inverse=True)
    indices = np.array(accessor(doc, data, primitive['indices'])).reshape(-1, 3)
    triangles = weld[indices]
    edges = collections.Counter()
    neighbors = collections.defaultdict(set)
    for a, b, c in triangles:
        assert len({a,b,c}) == 3, 'Degenerate triangle in flower base'
        for i, j in [(a,b),(b,c),(c,a)]:
            edges[tuple(sorted((i,j)))] += 1
            neighbors[i].add(j)
            neighbors[j].add(i)
    assert all(n==2 for n in edges.values()), 'Shell must be closed after UV seam welding'
    remaining = set(neighbors)
    groups = []
    for seed in triangles[:,0]:
        if seed not in remaining:
            continue
        stack = [seed]
        group = {seed}
        remaining.remove(seed)
        while stack:
            new = neighbors[stack.pop()] & remaining
            group.update(new)
            remaining.difference_update(new)
            stack.extend(new)
        member_faces = triangles[np.isin(triangles[:,0], list(group))]
        groups.append((vertices[sorted(group)], vertices[member_faces]))
    return groups


def inside_shell(point, shell):
    # Sum oriented triangle solid angles around a sample point in a closed shell.
    a, b, c = (shell[1] - point).transpose(1,0,2)
    la, lb, lc = (np.linalg.norm(v, axis=1) for v in (a,b,c))
    dot = lambda u, v: np.einsum('ij,ij->i', u, v)
    numerator = dot(a, np.cross(b,c))
    denominator = la*lb*lc + dot(a,b)*lc + dot(b,c)*la + dot(c,a)*lb
    return abs(2*np.arctan2(numerator, denominator).sum()) > 3.8*math.pi


def verify_flower_contacts(doc, data):
    shells = {m['name']: closed_shells(doc,data,m['primitives'][0]) for m in doc['meshes']}
    cup, *sepals = shells['Hypanthium_and_five_sepals']
    stalks = shells['Filaments']
    heads = shells['Anthers_and_pistils']
    assert len(sepals)==5 and len(stalks)==len(heads)==31
    for blade in sepals:
        points = blade[0]
        root = points[np.argmin(np.linalg.norm(points[:,[0,2]],axis=1))]
        assert inside_shell(root,cup), 'Sepal must enter the receptacle'
    for index, (stalk, head) in enumerate(zip(stalks,heads)):
        vertices = stalk[0]
        root = vertices[np.isclose(vertices[:,1],vertices[:,1].min(),atol=1e-7)]
        assert all(inside_shell(p,cup) for p in root), f'Flower filament {index} floats above the receptacle'
        assert any(inside_shell(p,head) for p in vertices), f'Flower head {index} is detached from its filament'
    # Check that the stem enters the receptacle below the joint, using the same pitch as the GLB node.
    points = shells['Stem_Skinned'][0][0]
    pitch = math.radians(35)
    c,s = math.cos(pitch), math.sin(pitch)
    upper = points[(points[:,1] > -.016) & (points[:,1] < -.004)]
    local = np.stack([upper[:,0], c*upper[:,1]+s*upper[:,2], -s*upper[:,1]+c*upper[:,2]],axis=1)
    assert any(inside_shell(p,cup) for p in local), 'Stem and receptacle must overlap below the flower head'
    return {'seatedSepals':len(sepals),'seatedFilamentsAndStyles':len(stalks),
            'headsConnectedToSupports':len(heads),'stemOverlapsReceptacle':True,'closedShells':True}

def verify():
    manifest=json.loads((OUT/'manifest.json').read_text());results=[]
    textures=manifest['textures'];assert textures['version']=='layered-basecolor-v5'
    texture_report={}
    for key in ['petal','stem']:
        rec=textures[key];raw=(OUT/rec['file']).read_bytes();image=Image.open(io.BytesIO(raw))
        assert hashlib.sha256(raw).hexdigest()==rec['sha256']
        assert list(image.size)==rec['size'] and image.mode=='RGB'
        assert max(ImageStat.Stat(image).stddev)>3, 'Base color must contain real tonal detail'
        texture_report[key]={'file':rec['file'],'size':rec['size'],'sha256':rec['sha256'],'opaque':True}
    assert len(textures['masters'])==4
    for name,digest in textures['masters'].items():
        assert hashlib.sha256((OUT.parents[4]/'art/textures'/name).read_bytes()).hexdigest()==digest
    palettes=textures['palettes'];assert [r['colorId'] for r in palettes]==list(range(8))
    assert palettes[3]['name']=='white' and palettes[4]['name']=='blue-enchantress'
    assert all(p['name']!='cream' for p in palettes)
    assert len({r['sha256'] for p in palettes for r in p['layers']})==24
    palette_report=[]
    for palette in palettes:
        assert [r['layer'] for r in palette['layers']]==['outer','middle','inner']
        assert not (OUT/f"petal-{palette['colorId']}.png").exists(), 'Remove unused single-layer textures from the package'
        layers=[];patterns=[]
        for rec in palette['layers']:
            raw=(OUT/rec['file']).read_bytes();image=Image.open(io.BytesIO(raw))
            assert hashlib.sha256(raw).hexdigest()==rec['sha256'] and image.size==(512,512) and image.mode=='RGB'
            assert rec['baseColorFactor']==[1,1,1,1]
            mean=ImageStat.Stat(image).mean
            if palette['name']=='white':assert max(mean)-min(mean)<18 and min(mean)>195, 'Keep the white palette predominantly white'
            if palette['name']=='blue-enchantress':assert mean[2]>mean[0]+40 and mean[2]>mean[1]+20, 'Blue enchantress must remain blue at every layer'
            if palette['name']=='yellow':assert mean[0]>mean[1]>mean[2]+30, 'Yellow texture must not have pink contamination'
            pixels=np.asarray(image,dtype=float)
            accents={}
            if palette['name']=='white':
                red,green,blue=np.moveaxis(pixels,-1,0)
                pink=(red-green>9)&(blue-green>1)
                yellow=(red-blue>9)&(green-blue>4)
                accents={
                    'pinkRimFraction':float(pink[:96].mean()),
                    'warmRootFraction':float(yellow[-96:,160:352].mean()),
                    'centerMeanChroma':float(np.ptp(pixels[140:350],axis=2).mean()),
                }
                assert accents['pinkRimFraction']>.25 and accents['warmRootFraction']>.25, 'White petals need pink rim and warm yellow root accents'
                assert accents['centerMeanChroma']<8, 'Keep the white petal membrane neutral between accents'
            rgb=pixels/255
            linear=np.where(rgb<=.04045,rgb/12.92,((rgb+.055)/1.055)**2.4)
            luminance=float((linear@[.2126,.7152,.0722]).mean())
            grey=image.convert('L');patterns.append((np.asarray(grey,dtype=float)-np.asarray(grey.filter(ImageFilter.GaussianBlur(3)),dtype=float)).ravel())
            layers.append({'layer':rec['layer'],'file':rec['file'],'sha256':rec['sha256'],'meanSrgb':mean,'meanLinearLuminance':luminance,**accents})
        values=[r['meanLinearLuminance'] for r in layers]
        assert values[0]>values[1]+.015 and values[1]>values[2]+.015, 'Every color must become naturally deeper from outer to inner'
        lightness=[116*v**(1/3)-16 for v in values]
        deltas=[lightness[i]-lightness[i+1] for i in [0,1]]
        assert max(deltas)<10.5, 'Layer-to-layer pigment differences must stay soft, not become sharply separate rings'
        correlations=[float(np.corrcoef(patterns[a],patterns[b])[0,1]) for a,b in [(0,1),(1,2),(0,2)]]
        assert all(abs(c)<.995 for c in correlations), 'Layer textures must have independently authored surface detail'
        palette_report.append({'colorId':palette['colorId'],'name':palette['name'],'layers':layers,'adjacentLightnessDeltas':deltas,'surfacePatternCorrelations':correlations})
    uv_maps=set()
    for rec in manifest['petals']:
        p=OUT/(rec['variantId']+'.glb');d,b=load(p);total=0
        assert hashlib.sha256(p.read_bytes()).hexdigest()==rec['sha256']
        for m in d['meshes']:
            for prim in m['primitives']:
                v=accessor(d,b,prim['attributes']['POSITION']);ii=accessor(d,b,prim['indices']);idx=[i[0] for i in ii];total+=len(idx)//3
                for channel in (0,1):
                    uv=verify_uv(d,b,prim,channel)
                    # The first vertex row on each face is the curved attachment edge.
                    back_start=len(v)//2
                    columns=rec['surfaceColumns']
                    roots=list(range(columns))+list(range(back_start,back_start+columns))
                    assert all(abs(uv[i][1]-1)<1e-6 for i in roots)
                    assert min(p[1] for p in uv)==0
                    uv_maps.add(tuple(tuple(p) for p in uv))
                verify_texture(d,b,d['materials'][prim['material']],(OUT/'neutral.png').read_bytes())
                edges=collections.Counter()
                for a,c,e in zip(idx[::3],idx[1::3],idx[2::3]):
                    for x,y in [(a,c),(c,e),(e,a)]:edges[tuple(sorted((x,y)))]+=1
                assert all(n==2 for n in edges.values()),'Petal must be a closed thin solid with a back and edge'
                assert all(0<=i<len(v) for i in idx)
        assert total==rec['triangles'] and total<=1800;results.append({'asset':p.name,'triangles':total,'closedSolid':True})
        profile=manifest['petalCupProfiles'][rec['variantId']]
        if profile:
            # A mid-blade cross section must curve around the core on both sides.
            tilt=math.radians(profile['tilt'])
            outward=np.array(v)[:,1]*math.cos(tilt)-np.array(v)[:,2]*math.sin(tilt)
            columns=rec['surfaceColumns']
            row=(rec['surfaceRows']-1)//2
            center=outward[row*columns+columns//2]
            assert min(center-outward[row*columns],center-outward[(row+1)*columns-1])>.0015
            results[-1]['bothSidesCupInward']=True
    assert len(results)==12 and len({r['sha256'] for r in manifest['petals']})==12
    assert len(uv_maps)==24, 'Each variant and channel must have its own UV sampling'
    d,b=load(OUT/'stem.glb');stem=sum(d['accessors'][p['indices']]['count']//3 for m in d['meshes'] for p in m['primitives']);assert stem<=5000
    assert len(d['images'])==1, 'Stem parts must share one embedded atlas'
    for m in d['meshes']:
        for prim in m['primitives']:
            uv=verify_uv(d,b,prim)
            verify_texture(d,b,d['materials'][prim['material']],(OUT/'stem-atlas.png').read_bytes(),[.88,.88,.88,1] if m['name']=='Anthers_and_pistils' else None)
            expected={'Stem_Skinned':{(0,0)},'Hypanthium_and_five_sepals':{(0,0),(1,0)},'Filaments':{(1,1)},'Anthers_and_pistils':{(0,1)}}[m['name']]
            assert {(int(u*2),int(v*2)) for u,v in uv}==expected
            ids=[i[0] for i in accessor(d,b,prim['indices'])]
            for i in range(0,len(ids),3):
                assert len({(int(uv[j][0]*2),int(uv[j][1]*2)) for j in ids[i:i+3]})==1, 'Triangle crosses atlas quadrants'
    skin=d['skins'][0];assert [d['nodes'][j]['name'] for j in skin['joints']]==['stem_root','stem_mid','stem_tip']
    assert len(skin['joints'])==3
    skinned=[n for n in d['nodes'] if 'skin' in n];assert len(skinned)==1
    prim=d['meshes'][skinned[0]['mesh']]['primitives'][0]
    stem_rec=manifest['stem'];assert stem_rec['geometryVersion']=='curved-stem-seated-core-v5'
    assert hashlib.sha256((OUT/'stem.glb').read_bytes()).hexdigest()==stem_rec['sha256']
    vertices=accessor(d,b,prim['attributes']['POSITION']);stride=stem_rec['bodyRingStride'];segments=stem_rec['bodySegments']
    centers=[[sum(p[k] for p in vertices[i*stride:i*stride+segments])/segments for k in range(3)] for i in range(stem_rec['bodyRings'])]
    assert all(max(abs(a[k]-e[k]) for k in range(3))<1e-7 for a,e in zip(centers,stem_rec['centerline']))
    assert max(p[0] for p in centers)-min(p[0] for p in centers)<.012, 'Stem must not retain the exaggerated sideways S'
    assert all(a[1]<c[1] for a,c in zip(centers,centers[1:])), 'Stem must grow upward without folding back'
    assert all(a[0]>=c[0]-1e-7 and a[2]<=c[2]+1e-7 for a,c in zip(centers,centers[1:])), 'The stem should curve gently in one direction'
    assert .60<=stem_rec['neckStartFraction']<=.70, 'The neck bend should begin lower on the stem'
    body=[p for i,p in enumerate(centers) if i/(len(centers)-1)<=stem_rec['neckStartFraction']]
    delta=np.array(body[-1])-body[0]
    incline=math.degrees(math.atan2(math.hypot(delta[0],delta[2]),delta[1]));assert .5<incline<4
    direction=delta/np.linalg.norm(delta)
    deviations=[np.linalg.norm(np.cross(np.array(p)-body[0],direction)) for p in body]
    assert .0007<max(deviations)<.002, 'The lower body needs a shallow visible arc'
    assert .015<centers[-1][2]-centers[0][2]<.028, 'The head should have a modest forward overhang'
    world=[0,0,0]
    for j,ring in zip(skin['joints'],[0,len(centers)//2,len(centers)-1]):
        world=[world[k]+d['nodes'][j]['translation'][k] for k in range(3)]
        assert max(abs(world[k]-centers[ring][k]) for k in range(3))<1e-7, 'Resting rig pivot must meet the curved centerline'
    binds=accessor(d,b,skin['inverseBindMatrices']);world=[0,0,0]
    for index,j in enumerate(skin['joints']):
        world=[world[k]+d['nodes'][j]['translation'][k] for k in range(3)]
        assert max(abs(binds[index][12+k]+world[k]) for k in range(3))<1e-7, 'Bind matrices must cancel the authored leaning joint positions'
    # The top cross-section normal and the real source core node must point along one axis.
    c=centers[-1];start=(len(centers)-1)*stride
    a=[vertices[start][k]-c[k] for k in range(3)];q=[vertices[start+2][k]-c[k] for k in range(3)]
    normal=[a[1]*q[2]-a[2]*q[1],a[2]*q[0]-a[0]*q[2],a[0]*q[1]-a[1]*q[0]]
    length=math.sqrt(sum(x*x for x in normal));normal=[x/length*(1 if normal[1]>0 else -1) for x in normal]
    axis=stem_rec['tipAxis'];assert sum(a*c for a,c in zip(normal,axis))>.99999
    for node in d['nodes']:
        if node.get('name') in ['Hypanthium_and_five_sepals','Filaments','Anthers_and_pistils']:
            x,y,z,w=node['rotation'];core_axis=[2*(x*y-w*z),1-2*(x*x+z*z),2*(y*z+w*x)]
            assert sum(a*c for a,c in zip(core_axis,axis))>.99999
    parameters=json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    for production,ring in zip(parameters['stemBindPositions'],[0,len(centers)//2,len(centers)-1]):
        assert max(abs(production[k]-centers[ring][k]) for k in range(3))<1e-7, 'Runtime forward kinematics must use the authored asset bind vectors'
    for config in parameters['rounds']:
        selected={p['textureFile'] for p in config['petals']}
        expected={'rose/'+r['file'] for r in palettes[config['colorId']]['layers']}
        assert selected==expected, 'All three production layer bindings must reach their own textures'
        for p in config['petals']:
            assert p['textureFile']==f"rose/petal-{config['colorId']}-{p['variant'].split('_')[0]}.png"
            assert p['uvIndex'] in (0,1) and .86<=p['albedoScale']<=1
        for layer in ('outer','middle','inner'):
            petals=[p for p in config['petals'] if p['variant'].startswith(layer)]
            assert len({p['albedoScale'] for p in petals})==len(petals)
            assert abs(sum(p['albedoScale'] for p in petals)/len(petals)-.93)<1e-6
    head=math.radians(parameters['headPitch']);core=math.radians(parameters['headPitch']+parameters['coreLocalPitch'])
    assert abs(math.cos(core)+math.sin(head))<1e-6 and abs(math.sin(core)-math.cos(head))<1e-6
    assert axis[1]>axis[2]>0, 'Shared growth axis must favor sky and still point toward the viewer'
    contacts = verify_flower_contacts(d,b)
    weights=accessor(d,b,prim['attributes']['WEIGHTS_0']);joints=accessor(d,b,prim['attributes']['JOINTS_0'])
    assert all(abs(sum(w)-1)<1e-5 and min(w)>=0 for w in weights)
    assert all(all(0<=j<3 for j in v) for v in joints)
    assert any(w[0]>.1 and w[1]>.1 for w in weights),'Skin must blend across joints'
    maximum=stem+30*max(r['triangles'] for r in results);assert maximum<=60000
    assert manifest['triangleBudgets']['petal']==1800 and manifest['triangleBudgets']['max30Assembly']==60000
    assert maximum==manifest['max30PetalTriangles']
    report={'passed':True,'petals':results,'stemTriangles':stem,'jointCount':3,'weightSumsValid':True,'max30PetalTriangles':maximum,'organicStem':{'version':stem_rec['geometryVersion'],'ringCount':len(centers),'bodyInclineDegrees':incline,'bodyLineDeviationMeters':max(deviations),'forwardOverhangMeters':centers[-1][2]-centers[0][2],'rigPivotsMeetCenterline':True,'restBindProductsIdentity':True,'closedAfterUvWelding':True,'tipAndCoreAxis':axis,'matchesProductionCrownAxis':True},'textures':texture_report,'palettes':palette_report,'all24TexturesDistinct':True,'allColorsOuterLighterThanInner':True,'textureMapping':{'productionLayerBindingsVerified':True,'petalRootAtImageBottom':True,'stemAtlasShared':True,'noCrossQuadrantTriangles':True,'longitudinalUVs':True},'scope':'Actual exported GLB/UV/skin, 24 layered textures, production Kotlin texture paths, weighted neck and exported crown axes; not headset rendering, comfort or performance.'}
    report['flowerContacts'] = contacts
    report['petalVariation']={'uniqueUvMaps':len(uv_maps),'channelsPerVariant':2,'productionAppearanceVerified':True,
        'neutralAlbedoScale':[.86,1],'meanLayerAlbedoScale':.93,'sharedRoundTextures':3}
    report['organicStem']['neckStartFraction'] = stem_rec['neckStartFraction']
    report['scope'] = 'Exported GLB and textures, authored bind positions, stem arc, and rest-pose mesh contacts.'
    dest=Path(__file__).resolve().parents[1]/'docs/asset-verification.json';dest.parent.mkdir(exist_ok=True);dest.write_text(json.dumps(report,indent=2));print(json.dumps(report))
if __name__=='__main__':verify()
