"""LLM-authored rose meshes. Local, reproducible production; no cloud service at runtime.

Each variant has its own shape specification and mesh, not a rescaled copy.
Meters, Y up, +Z front. Petal attachment pivot is the local origin.
Hard limits: petal <=500 triangles (including back/edge); entire stem <=5000.
"""
from pathlib import Path
import json, math, struct, hashlib, io
from functools import lru_cache
from random import Random
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/src/main/assets/rose'
OUT.mkdir(parents=True, exist_ok=True)
TEXTURE_SOURCES = ROOT / 'art/textures'
TEXTURE_VERSION = 'layered-basecolor-v5'
PETAL_MASTERS = ['petal-whorls-a-master-v5.png', 'petal-whorls-b-master-v5.png']
STEM_VERSION = 'curved-stem-seated-core-v5'
STEM_RINGS = 41
STEM_SEGMENTS = 12
LAYERS = ['outer','middle','inner']
PALETTE_NAMES = ['red','pink','pale-pink','white','blue-enchantress','yellow','orange','lavender']
NECK_START = .64
BODY_BOW = .0035
PETAL_CUP_PROFILES = {
    'outer_01': {'radius': .0074, 'height': -.0090, 'tilt': 18},
    'outer_02': {'radius': .0080, 'height': -.0078, 'tilt': 24},
    'outer_03': {'radius': .0086, 'height': -.0065, 'tilt': 30},
    'outer_04': {'radius': .0092, 'height': -.0052, 'tilt': 42},
    'middle_01': {'radius': .0089, 'height': -.0058, 'tilt': 36},
    'middle_02': {'radius': .0096, 'height': -.0044, 'tilt': 48},
    'middle_03': {'radius': .0100, 'height': -.0034, 'tilt': 54},
    'middle_04': {'radius': .0104, 'height': -.0026, 'tilt': 66},
    'inner_01': {'radius': .0102, 'height': -.0030, 'tilt': 60},
    'inner_02': {'radius': .0106, 'height': -.0022, 'tilt': 72},
    'inner_03': {'radius': .0108, 'height': -.0016, 'tilt': 78},
    'inner_04': {'radius': .0109, 'height': -.0014, 'tilt': 82},
}
PETAL_OVERLAP_SLOPE = .05
PETAL_OVERLAP_SLOPES = {"outer_04": .06, "inner_04": .08}
PETAL_COLLAR_LENGTH = .006
PETAL_CUP_HALF_THICKNESS = .00013
PETAL_COLLAR_LIFT_STEP = .0007
# User refinement: a modest backward roll and shallow side cup on the outer layer only.
# The root, authored opening angles and middle/inner surfaces stay unchanged.
OUTER_EXTRA_ROLL = .0025
OUTER_SIDE_CUP = .0012
SPECS = [
    ('outer_01', .082, .096, .010, -.010, .002, .000, .000, 'Broad rounded fan, shallow cup, rolled back upper rim.'),
    ('outer_02', .079, .092, .012, -.005, .004, -.006, .000, 'Wide oval fan, wavy lip, asymmetric left shoulder.'),
    ('outer_03', .084, .094, .008, -.008, .002, .002, .004, 'Broad fan, shallow central notch, gentle saddle bend.'),
    ('outer_04', .080, .090, .011, -.004, .003, .005, .000, 'Rounded silhouette, right lip curls outward.'),
    ('middle_01', .067, .080, .018, .002, .001, .000, .000, 'Rounded fan, deeper bowl, smooth lip.'),
    ('middle_02', .064, .078, .016, .003, .002, -.007, .000, 'Left rim folded inward, asymmetric bowl.'),
    ('middle_03', .063, .084, .015, .001, .004, .001, .000, 'Tall rounded fan, softly undulating lip.'),
    ('middle_04', .066, .079, .017, .003, .001, .007, .000, 'Rounded spoon, gentle diagonal twist.'),
    ('inner_01', .045, .059, .020, .008, .001, .000, .000, 'Narrow rounded petal, inward curl, open front.'),
    ('inner_02', .048, .056, .022, .006, .002, -.006, .000, 'Small cupped fan, softly folded left rim.'),
    ('inner_03', .044, .060, .018, .007, .002, .006, .000, 'Small oval, softly folded right rim.'),
    ('inner_04', .047, .058, .021, .009, .001, .002, .000, 'Small open cup with mild S profile and rounded lip.'),
]

def normals(v, f):
    n = [[0., 0., 0.] for _ in v]
    for a,b,c in f:
        u=[v[b][k]-v[a][k] for k in range(3)];w=[v[c][k]-v[a][k] for k in range(3)]
        q=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]]
        for i in (a,b,c):
            for k in range(3): n[i][k]+=q[k]
    return [[x/max(1e-15,math.sqrt(sum(y*y for y in q))) for x in q] for q in n]

def petal(spec):
    name,width,length,cup,curl,wave,twist,notch,_=spec
    v=[];uv=[];f=[];rows=8;cols=15 if name.startswith('inner') else 13
    for back in range(2):
        for i in range(rows):
            t=i/(rows-1)
            for j in range(cols):
                s=2*j/(cols-1)-1
                spread=(.06+.94*math.sin(math.pi*t*.55)**.85)
                x=s*width/2*spread*(1+.06*math.sin(4*t+twist*40)*s)
                y=length*t-.018*t**5*s*s-notch*t**7*math.exp(-s*s*24)
                y+=wave*.45*math.sin(s*math.pi*3)*t**6
                z=cup*(s*s-.38)*math.sin(t*math.pi*.85)+curl*t**5+twist*s*t*t
                z+=wave*math.sin(s*math.pi*2.5+.7)*t**5
                z+=(-.00045 if back else .00045)
                # glTF UV origin is image top-left: root at bottom, broad rim at top.
                v.append([x,y,z]);uv.append([(s+1)/2,1-t])
        off=back*rows*cols
        for i in range(rows-1):
            for j in range(cols-1):
                a=off+i*cols+j;b=a+1;c=a+cols;d=c+1
                f.extend([(a,b,c),(b,d,c)] if not back else [(a,c,b),(b,c,d)])
    border=list(range(cols))+[i*cols+cols-1 for i in range(1,rows)]+list(range(rows*cols-2,(rows-1)*cols-1,-1))+[i*cols for i in range(rows-2,0,-1)]
    for a,b in zip(border,border[1:]+border[:1]):
        f.extend([(a,a+rows*cols,b),(b,a+rows*cols,b+rows*cols)])
    assert len(f)<=500
    return v,f,uv

@lru_cache(maxsize=26)
def texture_png(kind='petal'):
    # Resource export only: resize generated bitmap masters, preserving their authored colors.
    # 512 px per petal / atlas quadrant; no new runtime shader, lights, or texture generation.
    if isinstance(kind,tuple):
        color,layer=kind;assert 0<=color<8 and layer in LAYERS
        source=PETAL_MASTERS[color//4]
        image=Image.open(TEXTURE_SOURCES/source).convert('RGB')
        # Export one generated atlas field with a two-source-pixel guard against neighbors.
        w,h=image.size;col,row=color%4,LAYERS.index(layer)
        image=image.crop((round(col*w/4)+2,round(row*h/3)+2,round((col+1)*w/4)-2,round((row+1)*h/3)-2))
        size=512
    else:
        source, size = {'petal':('petal-neutral-master-v2.png',512),
                        'stem':('stem-atlas-master-v2.png',1024)}[kind]
        image=Image.open(TEXTURE_SOURCES/source).convert('RGB')
    image=image.resize((size,size),Image.Resampling.LANCZOS)
    png=io.BytesIO();image.save(png,format='PNG',optimize=True);return png.getvalue()

def atlas_uv(tile,u,v):
    # Four-pixel inset prevents cross-quadrant filtering at mip levels used near the flower.
    col,row={'stem':(0,0),'sepal':(1,0),'pollen':(0,1),'filament':(1,1)}[tile]
    inset=4/1024
    return [col*.5+inset+u*(.5-2*inset),row*.5+inset+v*(.5-2*inset)]

class Glb:
    def __init__(self):
        self.data=bytearray();self._textures={};self.doc={'asset':{'version':'2.0','generator':'Petaluck LLM mesh authoring v1 / botanical base color v2'},'buffers':[{}],'bufferViews':[],'accessors':[],'meshes':[],'nodes':[],'materials':[],'scenes':[{'nodes':[]}],'scene':0,'extensionsUsed':['KHR_materials_unlit']}
    def buf(self,data):
        while len(self.data)%4:self.data.append(0)
        idx=len(self.doc['bufferViews']);self.doc['bufferViews'].append({'buffer':0,'byteOffset':len(self.data),'byteLength':len(data)});self.data.extend(data);return idx
    def acc(self,values,typ,ctype=5126):
        n={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16}[typ]
        flat=[q for row in values for q in row] if n>1 else values
        fmt={5126:'f',5123:'H'}[ctype]
        a={'bufferView':self.buf(struct.pack('<'+fmt*len(flat),*flat)),'componentType':ctype,'count':len(values),'type':typ}
        if typ=='VEC3':a.update(min=[min(row[k] for row in values) for k in range(3)],max=[max(row[k] for row in values) for k in range(3)])
        idx=len(self.doc['accessors']);self.doc['accessors'].append(a);return idx
    def material(self,name,color,texture=False):
        m={'name':name,'pbrMetallicRoughness':{'baseColorFactor':color,'metallicFactor':0,'roughnessFactor':.9},'doubleSided':True,'extensions':{'KHR_materials_unlit':{}}}
        if texture:
            image=texture_png() if texture is True else texture
            digest=hashlib.sha256(image).hexdigest()
            if digest not in self._textures:
                images=self.doc.setdefault('images',[]);textures=self.doc.setdefault('textures',[])
                self.doc.setdefault('samplers',[{'magFilter':9729,'minFilter':9987,'wrapS':33071,'wrapT':33071}])
                images.append({'bufferView':self.buf(image),'mimeType':'image/png'})
                self._textures[digest]=len(textures);textures.append({'source':len(images)-1,'sampler':0})
            m['pbrMetallicRoughness']['baseColorTexture']={'index':self._textures[digest]}
        idx=len(self.doc['materials']);self.doc['materials'].append(m);return idx
    def mesh(self,name,v,f,uv,mat,skin=False,uv1=None):
        attrs={'POSITION':self.acc(v,'VEC3'),'NORMAL':self.acc(normals(v,f),'VEC3'),'TEXCOORD_0':self.acc(uv,'VEC2')}
        if uv1 is not None:
            attrs['TEXCOORD_1']=self.acc(uv1,'VEC2')
        if skin:
            joints=[];weights=[]
            for x,y,z in v:
                p=max(0,min(2,(y+.24)/.12));a=min(1,int(p));q=p-a
                joints.append([a,a+1,0,0]);weights.append([1-q,q,0,0])
            attrs['JOINTS_0']=self.acc(joints,'VEC4',5123);attrs['WEIGHTS_0']=self.acc(weights,'VEC4')
        idx=len(self.doc['meshes']);self.doc['meshes'].append({'name':name,'primitives':[{'attributes':attrs,'indices':self.acc([i for t in f for i in t],'SCALAR',5123),'material':mat}]})
        node={'name':name,'mesh':idx}
        if skin:node['skin']=0
        ni=len(self.doc['nodes']);self.doc['nodes'].append(node);self.doc['scenes'][0]['nodes'].append(ni);return ni
    def save(self,path):
        self.doc['buffers'][0]['byteLength']=len(self.data)
        j=json.dumps(self.doc,separators=(',',':')).encode();j+=b' '*((-len(j))%4);d=bytes(self.data);d+=b'\0'*((-len(d))%4)
        if path.exists():
            previous = path.read_bytes()
            json_size = struct.unpack_from('<I', previous, 12)[0]
            old_doc = json.loads(previous[20:20+json_size])
            # A product rename alone must not rewrite unchanged model provenance.
            old_doc['asset']['generator'] = self.doc['asset']['generator']
            if old_doc == self.doc and previous[28+json_size:] == d:
                return
        path.write_bytes(struct.pack('<III',0x46546c67,2,12+8+len(j)+8+len(d))+struct.pack('<I4s',len(j),b'JSON')+j+struct.pack('<I4s',len(d),b'BIN\0')+d)

def tube(v,f,uv,start,end,r1,r2,segments=8,tile='stem'):
    dx,dy,dz=[end[k]-start[k] for k in range(3)];ln=math.sqrt(dx*dx+dy*dy+dz*dz)
    axis=[dx/ln,dy/ln,dz/ln];a=[axis[1],-axis[0],0]
    if sum(x*x for x in a)<.01:a=[1,0,0]
    l=math.sqrt(sum(x*x for x in a));a=[x/l for x in a];b=[axis[1]*a[2]-axis[2]*a[1],axis[2]*a[0]-axis[0]*a[2],axis[0]*a[1]-axis[1]*a[0]]
    base=len(v)
    stride=segments+1
    for i,(c,r) in enumerate([(start,r1),(end,r2)]):
        for j in range(stride):
            th=(j%segments)*2*math.pi/segments;v.append([c[k]+r*(math.cos(th)*a[k]+math.sin(th)*b[k]) for k in range(3)]);uv.append(atlas_uv(tile,j/segments,1-i))
    for j in range(segments):
        p=base+j;q=p+1;f.extend([(p,q,p+stride),(q,q+stride,p+stride)])
    for j in range(1,segments-1):f.extend([(base,base+j+1,base+j),(base+stride,base+stride+j,base+stride+j+1)])

def stem_center(t):
    # A shallow continuous arc runs through the body; the neck adds a smooth forward bend.
    u = max(0, (t - NECK_START) / (1 - NECK_START))
    x = .008 * (1 - t)**2
    neck_bow = (.24 * math.tan(math.radians(35)) - 2 * BODY_BOW) * (1 - NECK_START) / 3
    z = BODY_BOW * (t*t - 1) + neck_bow * (u**3 - 1)
    return [x,-.24+.24*t,z]

def stem_frame(t):
    u=max(0,(t-NECK_START)/(1-NECK_START))
    dx=-.016*(1-t)
    dz=2*BODY_BOW*t+(.24*math.tan(math.radians(35))-2*BODY_BOW)*u*u
    tangent=[dx,.24,dz];length=math.sqrt(sum(x*x for x in tangent));tangent=[x/length for x in tangent]
    across=[tangent[1],-tangent[0],0];length=math.sqrt(sum(x*x for x in across));across=[x/length for x in across]
    around=[across[1]*tangent[2]-across[2]*tangent[1],across[2]*tangent[0]-across[0]*tangent[2],across[0]*tangent[1]-across[1]*tangent[0]]
    return across,around

def warp_stem(point):
    x,y,z=point;t=max(0,min(1,(y+.24)/.24));center=stem_center(t);across,around=stem_frame(t)
    return [center[k]+x*across[k]+z*around[k] for k in range(3)]

def stem_radius(t):
    # Gentle taper, slightly oval sections, and soft growth nodes around the thorn attachments.
    nodes=sum(.11*math.exp(-((t-n)/.035)**2) for n in [1/6,.086/.24,.132/.24])
    return (.0036-.0011*t)*(1+.035*math.sin(t*math.pi*3)+nodes)


def append_rings(vertices, faces, uv, rings, tile, across_width=False):
    """Append a capped mesh with an explicit longitudinal UV seam."""
    base = len(vertices)
    segments = len(rings[0])
    stride = segments + 1
    for i, ring in enumerate(rings):
        for j, point in enumerate(ring + ring[:1]):
            vertices.append(point)
            u = (.5 + .5 * math.cos(j * math.tau / segments)) if across_width else j / segments
            uv.append(atlas_uv(tile, u, 1 - i / (len(rings) - 1)))
    for i in range(len(rings) - 1):
        for j in range(segments):
            a = base + i * stride + j
            faces.extend([(a, a + stride, a + 1), (a + 1, a + stride, a + stride + 1)])
    end = base + (len(rings) - 1) * stride
    for j in range(1, segments - 1):
        faces.extend([(base, base + j, base + j + 1), (end, end + j + 1, end + j)])


def section(center, across, along, width, depth, segments):
    around = [across[1]*along[2]-across[2]*along[1],
              across[2]*along[0]-across[0]*along[2],
              across[0]*along[1]-across[1]*along[0]]
    return [
        [center[k] + width*across[k]*math.cos(angle) + depth*around[k]*math.sin(angle)
         for k in range(3)]
        for angle in (j * math.tau / segments for j in range(segments))
    ]


def unit(vector):
    length = math.sqrt(sum(x*x for x in vector))
    return [x / length for x in vector]


def add_sepals(vertices, faces, uv):
    for i in range(5):
        angle = i * math.tau / 5 + .14
        radial = [math.sin(angle), 0, math.cos(angle)]
        tangent = [math.cos(angle), 0, -math.sin(angle)]
        rings = []
        for j in range(8):
            t = j / 7
            # Broaden near the seated root, then curve downward to a narrow tip.
            radius = .0074 + (.020 + .0014*math.sin(i*2.1)) * t
            y = -.004 + .0025*math.sin(math.pi*t) - .018*t*t
            curl = .0023 * math.sin(i*2.4 + .7) * t*t
            center = [radial[k]*radius + tangent[k]*curl for k in range(3)]
            center[1] = y
            along = unit([.020*radial[0], .0025*math.pi*math.cos(math.pi*t)-.036*t, .020*radial[2]])
            width = .00018 + (.00145*(1-t) + .0036*math.sin(math.pi*t)**.85) * (1-.5*t)
            depth = .00010 + .00058*math.sin(math.pi*t)
            rings.append(section(center, tangent, along, width, depth, 6))
        append_rings(vertices, faces, uv, rings, 'sepal', across_width=True)


def flower_parts():
    """Shared filament end points keep every anther and stigma seated on its support."""
    for i in range(31):
        is_style = i >= 24
        angle = i * 2.399963 + .13
        radial = [math.sin(angle), 0, math.cos(angle)]
        tangent = [math.cos(angle), 0, -math.sin(angle)]
        root_radius = (.0011 + .00055*(i%3)) if is_style else (.0064 + .00365*((i*7)%24)/23)
        height = (.0072 + .0012*math.sin(i*1.8)) if is_style else (.0057 + .0028*((i*11)%24)/23)
        bend = (.00065 if is_style else .00155) * math.sin(i*1.29 + .5)
        rings = []
        for j in range(5):
            t = j / 4
            r = root_radius + bend*t*t
            side = .00065*math.sin(i*2.1)*math.sin(math.pi*t)
            center = [radial[k]*r+tangent[k]*side for k in range(3)]
            center[1] = -.0012 + height*t
            along = unit([2*bend*t*radial[0], height, 2*bend*t*radial[2]])
            width = (.00031 if is_style else .00037) * (1-.48*t)
            rings.append(section(center, tangent, along, width, width, 5))
        yield is_style, rings, center, radial, tangent


def add_flower_parts(g, pale, gold):
    filaments = ([], [], [])
    heads = ([], [], [])
    for is_style, rings, center, radial, tangent in flower_parts():
        append_rings(*filaments, rings, 'filament')
        # A rounded, grooved anther with a long axis across the curved filament tip.
        along = unit([tangent[0], .42, tangent[2]])
        length = .00060 if is_style else .00102
        width = .00052 if is_style else .00071
        head_rings = []
        profile = [(-1,.18),(0,1),(1,.18)] if is_style else [(-1,.18),(-.62,.85),(0,1),(.62,.85),(1,.18)]
        for axial, scale in profile:
            point = [center[k]+axial*length*along[k] for k in range(3)]
            ring = section(point, radial, along, width*scale, width*scale*.80, 6)
            if not is_style:
                # Shallow indentation down each broad face suggests the two pollen sacs.
                for j in (0,3):
                    ring[j] = [point[k] + .82*(ring[j][k]-point[k]) for k in range(3)]
            head_rings.append(ring)
        append_rings(*heads, head_rings, 'pollen')
    return [(g.mesh('Filaments', *filaments, pale), len(filaments[1])),
            (g.mesh('Anthers_and_pistils', *heads, gold), len(heads[1]))]

def build_stem():
    g=Glb();atlas=texture_png('stem')
    # Neutral pollen multiplier leaves headroom for the existing completion brightness pulse.
    green=g.material('Botanical_stem_and_sepals',[1,1,1,1],atlas);gold=g.material('Golden_anthers',[.88,.88,.88,1],atlas);pale=g.material('Pale_filaments',[1,1,1,1],atlas);total=0
    v=[];f=[];uv=[]
    rings = []
    for i in range(STEM_RINGS):
        t = i / (STEM_RINGS - 1)
        y = -.24 + .24*t
        radius = stem_radius(t)
        rings.append([
            warp_stem([radius*math.cos(angle), y, radius*.96*math.sin(angle)])
            for angle in (j*math.tau/STEM_SEGMENTS for j in range(STEM_SEGMENTS))
        ])
    append_rings(v, f, uv, rings, 'stem')
    # Skin all stem loops to the actual 3-joint chain.
    joints=[stem_center(t) for t in [0,.5,1]]
    g.doc['nodes']=[{'name':'stem_root','translation':joints[0],'children':[1]},
        {'name':'stem_mid','translation':[joints[1][k]-joints[0][k] for k in range(3)],'children':[2]},
        {'name':'stem_tip','translation':[joints[2][k]-joints[1][k] for k in range(3)]}]
    g.doc['scenes'][0]['nodes']=[0]
    inv=[]
    for x,y,z in joints: inv.append([1,0,0,0,0,1,0,0,0,0,1,0,-x,-y,-z,1])
    g.doc['skins']=[{'name':'stem_three_bone_skin','joints':[0,1,2],'skeleton':0,'inverseBindMatrices':g.acc(inv,'MAT4')}]
    for i in range(3):
        thorn=[];tf=[];tu=[];y=-.20+i*.046;th=i*2.4
        tube(thorn,tf,tu,[.002*math.cos(th),y,.002*math.sin(th)],[.008*math.cos(th),y-.007,.008*math.sin(th)],.002,.0003,6)
        offset=len(v);v.extend(warp_stem(p) for p in thorn);f.extend(tuple(offset+j for j in tri) for tri in tf);uv.extend(tu)
    total+=len(f);g.mesh('Stem_Skinned',v,f,uv,green,True)
    # Round the cup into its lip and close the shallow stamen bed toward the center.
    v, f, uv = [], [], []
    profile = [
        (-.021, .0027), (-.018, .0032), (-.015, .0045), (-.012, .0062),
        (-.009, .0081), (-.006, .0099), (-.003, .0110), (-.0006, .0115),
        (.0013, .0111), (.0008, .0082), (.00025, .0042),
    ]
    rings = [section([0,y,0], [1,0,0], [0,1,0], r, r, 16) for y,r in profile]
    append_rings(v, f, uv, rings, 'stem')
    add_sepals(v, f, uv)
    receptacle = g.mesh('Hypanthium_and_five_sepals', v, f, uv, green)
    parts = [(receptacle, len(f))] + add_flower_parts(g, pale, gold)
    for node, triangles in parts:
        total += triangles
        g.doc['scenes'][0]['nodes'].remove(node)
        g.doc['nodes'][2].setdefault('children', []).append(node)
    # Standalone GLB uses the same resting axis as the app: +Y pitched 35 degrees toward +Z.
    # Runtime reparents these core parts under CrownMotion and uses +90 degrees locally,
    # so the core and petals then inherit exactly the same rest/interaction transformation.
    for ni in g.doc['nodes'][2]['children']:
        g.doc['nodes'][ni]['rotation']=[math.sin(math.radians(17.5)),0,0,math.cos(math.radians(17.5))]
    assert total<=5000;g.save(OUT/'stem.glb');return total

def petal_uvs(uv, variant_index, channel):
    rng=Random(8921 + variant_index*47 + channel*997)
    width=rng.uniform(.75,.95)
    inset=(1-width)/2
    rim_offset=rng.uniform(-.94,.94)*inset
    middle_offset=rng.uniform(-.85,.85)*inset
    vertical_curve=rng.uniform(.87,1.16)
    direction=(-1)**(variant_index+channel)
    return [
        [.5+direction*(u-.5)*width + (1-v)*((1-v)*rim_offset+v*middle_offset), v**vertical_curve]
        for u,v in uv
    ]


def cup_petal(vertices, variant):
    """Wrap the membrane around the blossom, with a small edge-to-edge depth offset."""
    profile = PETAL_CUP_PROFILES[variant]
    radius = profile['radius']
    tilt = math.radians(profile['tilt'])
    c, s = math.cos(tilt), math.sin(tilt)
    front_count = len(vertices) // 2
    order = sorted(PETAL_CUP_PROFILES, key=lambda name: PETAL_CUP_PROFILES[name]['tilt'])
    rank = order.index(variant)
    wrapped = []
    width = next(spec[1] for spec in SPECS if spec[0] == variant)
    for index, (x, y, _) in enumerate(vertices):
        radial = radius + y*c
        angle = x/radial
        collar = min(1, max(0, y/PETAL_COLLAR_LENGTH))
        lift = rank*PETAL_COLLAR_LIFT_STEP*collar*collar*(3-2*collar)
        height = y*s + x*PETAL_OVERLAP_SLOPES.get(variant, PETAL_OVERLAP_SLOPE) + lift
        if variant.startswith('outer'):
            height -= .004*(y/.095)**6
            height -= OUTER_EXTRA_ROLL*(y/.095)**4
            height += OUTER_SIDE_CUP*(2*x/width)**2*(y/.095)**2
        height += PETAL_CUP_HALF_THICKNESS if index < front_count else -PETAL_CUP_HALF_THICKNESS
        tangent = radial*math.sin(angle)
        outward = radial*math.cos(angle) - radius
        # Store in the petal's local frame, before the runtime's radial lift.
        wrapped.append([tangent, outward*c + height*s, -outward*s + height*c])
    return wrapped


def main():
    records=[]
    for i,spec in enumerate(SPECS):
        v,f,uv=petal(spec)
        v=cup_petal(v,spec[0])
        g=Glb()
        mat=g.material('Neutral_botanical_membrane',[1,1,1,1],True)
        g.mesh(spec[0],v,f,petal_uvs(uv,i,0),mat,uv1=petal_uvs(uv,i,1))
        p=OUT/(spec[0]+'.glb')
        g.save(p)
        records.append({'variantId':spec[0],'layer':spec[0].split('_')[0],'triangles':len(f),'surfaceRows':8,'surfaceColumns':15 if spec[0].startswith('inner') else 13,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'method':'Independent LLM-authored parametric mesh, not cloud text-to-3D','brief':spec[-1],'shapeParameters':list(spec[1:-1]),'pivot':[0,0,0],'bounds':[[min(x[k] for x in v) for k in range(3)],[max(x[k] for x in v) for k in range(3)]]})
    stem=build_stem()
    manifest={'assetLibraryVersion':'rose-llm-v9','layoutVersion':'interleaved-petals-v6','coordinates':'meters, Y-up, +Z front','source':'New LLM-authored meshes based on user-provided petaluck.zip reference images. No third-party stock models.','petals':records,'stem':{'file':'stem.glb','triangles':stem,'sha256':hashlib.sha256((OUT/'stem.glb').read_bytes()).hexdigest(),'geometryVersion':STEM_VERSION,'joints':['stem_root','stem_mid','stem_tip'],'skinned':True,'bodyRings':STEM_RINGS,'bodyRingStride':STEM_SEGMENTS+1,'bodySegments':STEM_SEGMENTS,'centerline':[stem_center(i/(STEM_RINGS-1)) for i in range(STEM_RINGS)],'neckStartFraction':NECK_START,'tipAxis':[0,math.cos(math.radians(35)),math.sin(math.radians(35))],'standaloneCorePitch':35,'runtimeCoreLocalPitch':90},'max30PetalTriangles':stem+30*max(r['triangles'] for r in records)}
    manifest['petalCupProfiles'] = PETAL_CUP_PROFILES
    manifest['petalOverlapSlope'] = PETAL_OVERLAP_SLOPE
    manifest['petalOverlapSlopeOverrides'] = PETAL_OVERLAP_SLOPES
    manifest['petalCollarLength'] = PETAL_COLLAR_LENGTH
    manifest['petalCupAxialHalfThickness'] = PETAL_CUP_HALF_THICKNESS
    manifest['petalCollarLiftStep'] = PETAL_COLLAR_LIFT_STEP
    manifest['outerCurlRefinement'] = {'version': 'outer-curl-v1', 'extraRollMeters': OUTER_EXTRA_ROLL, 'sideCupMeters': OUTER_SIDE_CUP}
    manifest['runtimeLayoutNotes']='Interleaved-petals-v6 gives each mesh its own seated cup profile. Outer openings span 18-42 degrees, middle 36-66, inner 60-82, so neighboring families overlap. All membranes wrap around the blossom with nearly zero local roll. Outer rims curl gently outward. Seeded placement varies angular spacing and fine pose offsets. A small baked surface offset separates overlapping edges. Each mesh has two asymmetric UV maps. Per-petal neutral brightness and the three layer albedos are shared with the prior material revision. Petals and core share CrownMotion.'
    manifest['textures']={'version':TEXTURE_VERSION,'provenance':'art/textures/generation-v5.md; built-in ImageGen edits with localized secondary pigment, locally cropped and resized only',
        'petal':{'file':'neutral.png','size':[512,512],'sha256':hashlib.sha256(texture_png()).hexdigest(),'tintable':True,'uv':'root V=1, rim V=0; same mapping on both faces'},
        'stem':{'file':'stem-atlas.png','size':[1024,1024],'sha256':hashlib.sha256(texture_png('stem')).hexdigest(),'quadrants':['stem','sepal','pollen','filament'],'uvInsetPixels':4},
        'masters':{name:hashlib.sha256((TEXTURE_SOURCES/name).read_bytes()).hexdigest() for name in ['petal-neutral-master-v2.png','stem-atlas-master-v2.png',*PETAL_MASTERS]}}
    manifest['textures']['palettes']=[{'colorId':i,'name':name,'layers':[
        {'layer':layer,'file':f'petal-{i}-{layer}.png','size':[512,512],'sha256':hashlib.sha256(texture_png((i,layer))).hexdigest(),'baseColorFactor':[1,1,1,1]}
        for layer in LAYERS]} for i,name in enumerate(PALETTE_NAMES)]
    (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False));print(json.dumps({'petals':len(records),'petalTriangles':records[0]['triangles'],'stemTriangles':stem,'max30':manifest['max30PetalTriangles']}))
    (OUT/'neutral.png').write_bytes(texture_png())
    (OUT/'stem-atlas.png').write_bytes(texture_png('stem'))
    for i in range(8):
        for layer in LAYERS:(OUT/f'petal-{i}-{layer}.png').write_bytes(texture_png((i,layer)))
        legacy=OUT/f'petal-{i}.png'
        if legacy.exists():legacy.unlink() # Earlier full-color textures are in the preserved snapshots.

if __name__=='__main__':main()
