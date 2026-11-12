"""Review GLBs from actual asset bytes and exported production Kotlin layouts.
Run unit tests first. The app never loads these static review assemblies.
"""
import json
import math
import struct
from pathlib import Path
from generate_rose_assets import Glb, ROOT, OUT

DEST = ROOT / 'art/review-models'


def rotation(p, x=0, y=0, z=0):
    """Runtime: qZ(azimuth) * qX(tilt) * qY(roll), followed by qX(headPitch)."""
    y=math.radians(y)
    a,b,c=p
    p=[a*math.cos(y)+c*math.sin(y), b, -a*math.sin(y)+c*math.cos(y)]
    x, z = math.radians(x), math.radians(z)
    a, b, c = p
    b, c = b*math.cos(x)-c*math.sin(x), b*math.sin(x)+c*math.cos(x)
    return [a*math.cos(z)-b*math.sin(z), a*math.sin(z)+b*math.cos(z), c]


def read_glb(path):
    raw = Path(path).read_bytes()
    length = struct.unpack_from('<I', raw, 12)[0]
    return json.loads(raw[20:20+length]), raw[28+length:]


def accessor(doc, data, index):
    a = doc['accessors'][index]
    b = doc['bufferViews'][a['bufferView']]
    n = {'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16}[a['type']]
    fmt = {5126:'f',5123:'H'}[a['componentType']]
    values = struct.unpack_from('<'+fmt*a['count']*n, data, b.get('byteOffset',0)+a.get('byteOffset',0))
    return [list(values[j:j+n]) for j in range(0,len(values),n)]


def mesh_data(doc, data, primitive, uv_index=0):
    v = accessor(doc, data, primitive['attributes']['POSITION'])
    ix = [a[0] for a in accessor(doc, data, primitive['indices'])]
    f = [tuple(ix[j:j+3]) for j in range(0,len(ix),3)]
    uv = accessor(doc, data, primitive['attributes'][f'TEXCOORD_{uv_index}'])
    return v, f, uv


def material_texture(doc, data, material):
    info=material['pbrMetallicRoughness'].get('baseColorTexture')
    if info is None:return False
    image=doc['images'][doc['textures'][info['index']]['source']]
    view=doc['bufferViews'][image['bufferView']]
    start=view.get('byteOffset',0)
    return data[start:start+view['byteLength']]


def assemble(config, parameters, name, asset_root=OUT):
    g = Glb()
    offset=parameters.get('presentation',{}).get('rootOffset',[0,0,0])
    doc, data = read_glb(asset_root/'stem.glb')
    for mesh in doc['meshes']:
        primitive = mesh['primitives'][0]
        v, f, uv = mesh_data(doc, data, primitive)
        # Match core's +Y to blossom-local +Z, then inherit the common CrownMotion.
        if mesh['name'] != 'Stem_Skinned':
            v = [rotation(rotation(p, x=parameters['coreLocalPitch']),x=parameters['headPitch']) for p in v]
        v = [[p[k]+(.04 if k==1 else 0)+offset[k] for k in range(3)] for p in v]
        material = doc['materials'][primitive['material']]
        color = material['pbrMetallicRoughness']['baseColorFactor']
        g.mesh(mesh['name'], v, f, uv, g.material(mesh['name'], color, material_texture(doc,data,material)))
    for p in config['petals']:
        # This path is exported by the same RoseLayout.texturePath used at runtime.
        path=p['textureFile']
        shade=p.get('albedoScale',1)
        material=g.material(path,[shade,shade,shade,1],(asset_root/Path(path).name).read_bytes())
        doc, data = read_glb(asset_root/(p['variant']+'.glb'))
        v, f, uv = mesh_data(doc, data, doc['meshes'][0]['primitives'][0], p.get('uvIndex',0))
        transformed = []
        for point in v:
            q = rotation([a*p['scale'] for a in point], x=p['tilt'], y=p.get('roll',0), z=p['angle'])
            q = rotation([q[k]+p['position'][k] for k in range(3)], x=parameters['headPitch'])
            q[1] += .04
            q = [q[k]+offset[k] for k in range(3)]
            transformed.append(q)
        g.mesh(f"Petal_{p['id']}_{p['variant']}", transformed, f, uv, material)
    DEST.mkdir(parents=True, exist_ok=True)
    path = DEST/(name+'.glb')
    g.save(path)
    return path


def main():
    parameters = json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    if parameters['layoutVersion'] != 'interleaved-petals-v6':
        raise ValueError('Run current production layout unit tests before building reviews')
    for config in parameters['rounds']:
        name = f"rose_{config['count']}" if config['colorId']==1 else f"color_{config['colorId']}"
        assemble(config, parameters, name)
    pink = next(c for c in parameters['rounds'] if c['count']==24 and c['colorId']==1)
    assemble(pink, parameters, 'color_1')
    assemble({'colorId':1,'petals':[]},parameters,'stem_only')
    print(f"Review assemblies: {parameters['layoutVersion']}, {DEST}")


if __name__ == '__main__':
    main()
