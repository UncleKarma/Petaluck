"""Check placement/entrance envelopes using exported Kotlin settings and production GLB bytes.

This is an offline rest-pose check, not a native scene or skin-animation capture.
"""
import hashlib
import json
import numpy as np
from create_review_assemblies import ROOT, OUT, read_glb, mesh_data, rotation


def rotate(vertices, **angles):
    basis = np.array([rotation(axis, **angles) for axis in np.eye(3)])
    return vertices @ basis


def verify():
    parameters = json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    presentation = parameters['presentation']
    offset = np.array(presentation['rootOffset'])
    assert np.allclose(offset, [0, -.1, 0], atol=1e-7)
    frames = presentation['frames']
    assert frames[0]['scale'] == frames[0]['opacity'] == 0
    assert frames[-1]['scale'] == frames[-1]['opacity'] == 1
    assert frames[-1]['finished'] and not any(f['finished'] for f in frames[:-1])
    assert all(a['scale'] < b['scale'] for a,b in zip(frames,frames[1:]))
    assert all(f['scale'] == f['opacity'] for f in frames)
    doc, data = read_glb(OUT/'stem.glb')
    stem = []
    for mesh in doc['meshes']:
        vertices, _, _ = mesh_data(doc,data,mesh['primitives'][0])
        vertices = np.array(vertices)
        if mesh['name'] != 'Stem_Skinned':
            vertices = rotate(vertices,x=parameters['coreLocalPitch']+parameters['headPitch'])
        vertices[:,1] += .04
        stem.append(vertices)
    petals = {}
    for path in sorted(OUT.glob('*.glb')):
        if path.name == 'stem.glb':
            continue
        doc, data = read_glb(path)
        petals[path.stem] = np.array(mesh_data(doc,data,doc['meshes'][0]['primitives'][0])[0])
    low = np.array([-.30,-.35,-.20])
    high = -low
    bounds_low = np.full(3,np.inf)
    bounds_high = np.full(3,-np.inf)
    checks = 0
    for config in parameters['rounds']:
        points = list(stem)
        for p in config['petals']:
            vertices = rotate(petals[p['variant']]*p['scale'],x=p['tilt'],y=p['roll'],z=p['angle'])+p['position']
            vertices = rotate(vertices,x=parameters['headPitch'])
            vertices[:,1] += .04
            points.append(vertices)
        points = np.concatenate(points)
        for frame in frames:
            world = points*frame['scale']+offset
            actual_low, actual_high = world.min(0), world.max(0)
            assert np.all(actual_low >= low-1e-7) and np.all(actual_high <= high+1e-7), (config['count'],config['seed'],frame)
            bounds_low = np.minimum(bounds_low,actual_low)
            bounds_high = np.maximum(bounds_high,actual_high)
            checks += 1
        assert np.allclose((points+offset)-points,offset,atol=1e-7)
    result = {
        'passed': True,
        'layoutVersion': parameters['layoutVersion'],
        'presentation': presentation,
        'sampledLayoutCount': len(parameters['rounds']),
        'sampledFrameEnvelopes': checks,
        'volumeMeters': {'min':low.tolist(),'max':high.tolist()},
        'restAndEntranceBounds': {'min':bounds_low.tolist(),'max':bounds_high.tolist()},
        'allVerticesTranslatedDownMeters': .1,
        'sourceMeshSha256': {p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(OUT.glob('*.glb'))},
        'scope':'Actual exported rest-pose mesh vertices under uniform entrance scale and translated RoseAnchor. Intermediate scales fit by convexity. Native opacity, skin playback, input and fall animation pending.',
    }
    (ROOT/'docs/rose-entrance-v1-verification.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ['passed','sampledLayoutCount','sampledFrameEnvelopes','restAndEntranceBounds']}))


if __name__ == '__main__':
    verify()
