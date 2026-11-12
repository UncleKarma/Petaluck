"""Verify blade intersections and root contacts using production layouts and actual GLB triangles."""
from functools import lru_cache
from itertools import combinations
import json

import numpy as np

from create_review_assemblies import ROOT, OUT, mesh_data, read_glb, rotation
from verify_rose_assets import closed_shells, inside_shell


@lru_cache(maxsize=12)
def petal_mesh(variant):
    doc, data = read_glb(OUT / (variant + '.glb'))
    vertices, faces, _ = mesh_data(doc, data, doc['meshes'][0]['primitives'][0])
    return np.array(vertices), np.array(faces)


def transformed_mesh(petal):
    vertices, faces = petal_mesh(petal['variant'])
    vertices = np.array([
        rotation(point * petal['scale'], x=petal['tilt'], y=petal.get('roll', 0), z=petal['angle'])
        for point in vertices
    ]) + petal['position']
    triangles = vertices[faces]
    return triangles, triangles.min(axis=1), triangles.max(axis=1)


def intersecting_triangles(first, second):
    """Separating-axis tests, with triangle AABBs and coplanar edge axes."""
    a, a_min, a_max = first
    b, b_min, b_max = second
    if np.any(a_min.min(0) > b_max.max(0)) or np.any(b_min.min(0) > a_max.max(0)):
        return []
    a_keep = np.all((a_max >= b_min.min(0)) & (a_min <= b_max.max(0)), axis=1)
    b_keep = np.all((b_max >= a_min.min(0)) & (b_min <= a_max.max(0)), axis=1)
    a, a_min, a_max = a[a_keep], a_min[a_keep], a_max[a_keep]
    b, b_min, b_max = b[b_keep], b_min[b_keep], b_max[b_keep]
    overlap = np.all(
        (a_max[:, None] >= b_min[None, :]) & (b_max[None, :] >= a_min[:, None]), axis=2
    )
    i, j = np.nonzero(overlap)
    if not len(i):
        return []
    a, b = a[i], b[j]
    a_edges = np.roll(a, -1, axis=1) - a
    b_edges = np.roll(b, -1, axis=1) - b
    a_normal = np.cross(a_edges[:, 0], a_edges[:, 1])
    b_normal = np.cross(b_edges[:, 0], b_edges[:, 1])
    axes = [a_normal, b_normal]
    axes.extend(np.cross(a_edges[:, k], b_edges[:, l]) for k in range(3) for l in range(3))
    axes.extend(np.cross(a_normal, a_edges[:, k]) for k in range(3))
    axes.extend(np.cross(b_normal, b_edges[:, k]) for k in range(3))
    hit = np.ones(len(i), dtype=bool)
    for axis in axes:
        length = np.linalg.norm(axis, axis=1)
        axis = axis / np.maximum(length[:, None], 1e-30)
        p = np.einsum('nvi,ni->nv', a, axis)
        q = np.einsum('nvi,ni->nv', b, axis)
        separate = (p.max(1) < q.min(1) - 1e-9) | (q.max(1) < p.min(1) - 1e-9)
        hit &= ~(separate & (length > 1e-18))
    return list(zip(a[hit], b[hit]))


def intersection_count(first, second):
    return len(intersecting_triangles(first, second))


def edge_intersections(triangle, target):
    normal = np.cross(target[1] - target[0], target[2] - target[0])
    normal /= np.linalg.norm(normal)
    e0, e1 = target[1] - target[0], target[2] - target[0]
    uu, vv, uv = np.dot(e0, e0), np.dot(e1, e1), np.dot(e0, e1)
    denominator = uu*vv - uv*uv
    points = []
    for start, end in zip(triangle, np.roll(triangle, -1, axis=0)):
        d0 = np.dot(start - target[0], normal)
        d1 = np.dot(end - target[0], normal)
        if d0*d1 > 0 or abs(d0 - d1) < 1e-12:
            continue
        point = start + d0/(d0 - d1)*(end - start)
        offset = point - target[0]
        a = (vv*np.dot(offset, e0) - uv*np.dot(offset, e1))/denominator
        b = (uu*np.dot(offset, e1) - uv*np.dot(offset, e0))/denominator
        if a >= -1e-6 and b >= -1e-6 and a + b <= 1 + 1e-6:
            points.append(point)
    return points


def verify():
    parameters = json.loads((ROOT / 'app/build/reports/rose-layouts.json').read_text())
    manifest = json.loads((OUT / 'manifest.json').read_text())
    assert parameters['layoutVersion'] == manifest['layoutVersion']
    variation = parameters['variation']
    doc, data = read_glb(OUT / 'stem.glb')
    cup_mesh = next(mesh for mesh in doc['meshes'] if mesh['name'] == 'Hypanthium_and_five_sepals')
    cup = closed_shells(doc, data, cup_mesh['primitives'][0])[0]
    failures = []
    root_contacts = []
    root_region = {'radius': .015, 'minZ': -.010, 'maxZ': .006}
    samples = []
    pair_count = 0
    bounds_min = np.full(3, np.inf)
    bounds_max = np.full(3, -np.inf)
    for sample_index, config in enumerate(parameters['rounds']):
        meshes = {petal['id']: transformed_mesh(petal) for petal in config['petals']}
        vertices = np.concatenate([mesh[0].reshape(-1, 3) for mesh in meshes.values()])
        world = np.array([rotation(point, x=parameters['headPitch']) for point in vertices])
        world[:, 1] += .04
        world += parameters.get('presentation', {}).get('rootOffset', [0, 0, 0])
        bounds_min = np.minimum(bounds_min, world.min(0))
        bounds_max = np.maximum(bounds_max, world.max(0))
        if np.any(world.min(0) < [-.30, -.35, -.20]) or np.any(world.max(0) > [.30, .35, .20]):
            failures.append({'count': config['count'], 'seed': config['seed'], 'outsideNominalVolume': True})
        for petal in config['petals']:
            x, y, z = petal['position']
            profile = manifest['petalCupProfiles'][petal['variant']]
            assert abs(petal['tilt'] - profile['tilt']) <= variation['whorlTilt'] + variation['petalTilt'] + 1e-5
            assert abs(petal['roll']) <= variation['roll'] + 1e-5
            assert abs(np.hypot(x, y) - profile['radius']) < 1e-7
            assert abs(z - profile['height']) < 1e-7
            if not inside_shell(np.array([x, z, -y]), cup):
                failures.append({'count': config['count'], 'seed': config['seed'], 'floatingRoot': petal['id']})
        for first, second in combinations(config['petals'], 2):
            pair_count += 1
            hits = intersecting_triangles(meshes[first['id']], meshes[second['id']])
            if not hits:
                continue
            points = []
            for a, b in hits:
                crossing = edge_intersections(a, b) + edge_intersections(b, a)
                # Coincident faces are conservatively checked using their full vertices.
                points.extend(crossing if crossing else np.concatenate([a, b]))
            points = np.array(points)
            radius = np.linalg.norm(points[:, :2], axis=1)
            in_root = (radius <= root_region['radius']) & (points[:, 2] >= root_region['minZ']) & (points[:, 2] <= root_region['maxZ'])
            record = {
                'count': config['count'], 'seed': config['seed'],
                'petals': [first['id'], second['id']], 'trianglePairs': len(hits),
                'min': points.min(0).tolist(), 'max': points.max(0).tolist(),
            }
            if np.all(in_root):
                root_contacts.append(record)
            else:
                failures.append(record)
                if len(failures) <= 12:
                    print("Blade contact", config["count"], config["seed"], first["variant"], second["variant"], record["min"], record["max"], flush=True)
        samples.append({'count': config['count'], 'seed': config['seed'], 'colorId': config['colorId']})
        if (sample_index + 1) % 50 == 0:
            print(f'Checked {sample_index + 1} layouts, {len(failures)} blade failures', flush=True)
    report = {
        'passed': not failures,
        'layoutVersion': parameters['layoutVersion'],
        'variation': variation,
        'sampleCount': len(samples),
        'petalPairsChecked': pair_count,
        'sampledPetalBounds': {'min': bounds_min.tolist(), 'max': bounds_max.tolist()},
        'samples': samples,
        'failures': failures,
        'rootContactRegion': root_region,
        'rootContacts': root_contacts,
        'scope': 'Rest-pose blade intersections across all petal pairs, plus seated root pivots. Contacts within the declared mounting region are reported. Runtime capture and animated dragging pending.',
    }
    (ROOT / 'docs/layout-v6-verification.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({key: report[key] for key in ('passed', 'sampleCount', 'petalPairsChecked')}))
    if failures:
        print(json.dumps(failures[:15], indent=2))
        raise SystemExit(1)


if __name__ == '__main__':
    verify()
