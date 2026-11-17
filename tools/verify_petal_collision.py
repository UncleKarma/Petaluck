"""Verify the frozen pre-subdivision colliders against their detailed render partners."""
import hashlib
import json
from collections import Counter
from pathlib import Path

import numpy as np
from create_review_assemblies import read_glb, mesh_data

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets/rose'


def verify():
    manifest = json.loads((ASSETS / 'collision/manifest.json').read_text())
    render_manifest = json.loads((ASSETS / 'manifest.json').read_text())
    renders = {r['variantId']: r for r in render_manifest['petals']}
    assert len(manifest['petals']) == len(renders) == 12
    assert {r['variantId'] for r in manifest['petals']} == set(renders)
    assert manifest['sourceCommit'] == '95872b9'
    results = []
    for record in manifest['petals']:
        variant = record['variantId']
        low_path = ASSETS / 'collision' / (variant + '.glb')
        high_path = ASSETS / (variant + '.glb')
        assert hashlib.sha256(low_path.read_bytes()).hexdigest() == record['sha256']
        assert hashlib.sha256(high_path.read_bytes()).hexdigest() == renders[variant]['sha256']
        low_doc, low_data = read_glb(low_path)
        high_doc, high_data = read_glb(high_path)
        # Imported mesh-local transforms must match: no compensating scene offset or scale.
        assert low_doc['nodes'] == high_doc['nodes']
        assert low_doc['scenes'] == high_doc['scenes']
        assert len(low_doc['meshes']) == len(high_doc['meshes']) == 1
        low, faces, _ = mesh_data(low_doc, low_data, low_doc['meshes'][0]['primitives'][0])
        high, high_faces, _ = mesh_data(high_doc, high_data, high_doc['meshes'][0]['primitives'][0])
        expected = 476 if variant.startswith('inner') else 412
        assert len(faces) == record['triangles'] == expected
        assert len(high_faces) > len(faces)
        # Closed membrane with opposite directed edges, including the thin side wall.
        edges = Counter((a, b) for f in faces for a, b in zip(f, f[1:] + f[:1]))
        assert all(count == 1 and edges[b, a] == 1 for (a, b), count in edges.items())
        low, high = np.array(low), np.array(high)
        assert np.isfinite(low).all()
        differences = np.linalg.norm(low[:, None] - high[None, :], axis=2).min(axis=1)
        assert differences.max() < 1e-8, (variant, differences.max())
        results.append({'variant': variant, 'collisionTriangles': len(faces),
                        'renderTriangles': len(high_faces),
                        'maxOriginalVertexErrorMeters': float(differences.max()),
                        'sha256': record['sha256']})
    report = {'revision': manifest['version'], 'status': 'PASS', 'variants': results,
              'scope': 'Asset identity, matching local frames, closed meshes and retained coarse vertices; native gaze selection requires physical retest'}
    (ROOT / 'docs/petal-collision-v1-verification.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report))


if __name__ == '__main__':
    verify()
