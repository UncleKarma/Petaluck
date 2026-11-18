"""Reduce each standard petal to a closed 152-triangle membrane in its original local frame."""
from collections import Counter
from pathlib import Path
import hashlib
import json

import numpy as np

from create_review_assemblies import Glb, accessor, mesh_data, read_glb

ROSE = Path(__file__).resolve().parents[1] / "app/src/main/assets/rose"
ROWS = 8
POINTS_PER_SIDE = 39


def select_columns(front):
    # Keep every side-edge vertex and each row's center. Spend the remaining
    # vertices on the largest chord deviations, giving the exposed lip priority.
    rows, cols, _ = front.shape
    kept = [{0, cols // 2, cols - 1} for _ in range(rows)]
    while sum(map(len, kept)) < POINTS_PER_SIDE:
        candidates = []
        for row, columns in enumerate(kept):
            ordered = sorted(columns)
            for left, right in zip(ordered, ordered[1:]):
                for col in range(left + 1, right):
                    weight = (col - left) / (right - left)
                    chord = (1 - weight) * front[row, left] + weight * front[row, right]
                    error = np.linalg.norm(front[row, col] - chord)
                    candidates.append((error * (12 if row == rows - 1 else 1), row, col))
        _, row, col = max(candidates)
        kept[row].add(col)
    return [sorted(columns) for columns in kept]


def simplify(path):
    doc, data = read_glb(path)
    prim = doc["meshes"][0]["primitives"][0]
    verts, _, uv = mesh_data(doc, data, prim)
    uv1 = accessor(doc, data, prim["attributes"]["TEXCOORD_1"])
    sheets = np.array(verts).reshape(2, ROWS, -1, 3)
    cols = sheets.shape[2]
    rows = select_columns(sheets[0])
    retained = [r * cols + c for r, row in enumerate(rows) for c in row]
    lookup = {divmod(idx, cols): k for k, idx in enumerate(retained)}
    front = []
    for r, (bottom, top) in enumerate(zip(rows, rows[1:])):
        i = j = 0
        while i + 1 < len(bottom) or j + 1 < len(top):
            a, b = lookup[r, bottom[i]], lookup[r + 1, top[j]]
            # Choose the shorter available cross-row diagonal in column space.
            if j + 1 == len(top) or (i + 1 < len(bottom) and
                    abs(bottom[i + 1] - top[j]) <= abs(top[j + 1] - bottom[i])):
                i += 1
                front.append((a, lookup[r, bottom[i]], b))
            else:
                j += 1
                front.append((a, lookup[r + 1, top[j]], b))
    n = len(retained)
    faces = front + [(a+n, c+n, b+n) for a, b, c in front]
    border = ([(0, c) for c in rows[0]] + [(r, cols-1) for r in range(1, ROWS)] +
              [(ROWS-1, c) for c in reversed(rows[-1][:-1])] + [(r, 0) for r in range(ROWS-2, 0, -1)])
    ring = [lookup[key] for key in border]
    for a, b in zip(ring, ring[1:] + ring[:1]):
        faces.extend([(a, a+n, b), (b, a+n, b+n)])
    ids = retained + [k + ROWS * cols for k in retained]
    edges = Counter((a,b) for f in faces for a,b in zip(f, f[1:] + f[:1]))
    assert len(faces) == 152 and all(count == edges[b,a] == 1 for (a,b),count in edges.items())
    triangles = np.array([verts[k] for k in ids])[faces, :]
    assert np.min(np.linalg.norm(np.cross(triangles[:,1]-triangles[:,0], triangles[:,2]-triangles[:,0]), axis=1)) > 1e-12

    # Every longitudinal side edge is exact. Bound both other boundary chains
    # by comparing corresponding points on each piecewise-linear source edge.
    error = max(np.linalg.norm(sheet[r,c] - ((b-c)*sheet[r,a]+(c-a)*sheet[r,b])/(b-a))
                for sheet in sheets for r in (0, ROWS-1)
                for a,b in zip(rows[r],rows[r][1:]) for c in range(a,b+1))
    glb = Glb()
    mat = glb.material("Petal", [1,1,1,1])
    glb.mesh(path.stem, [verts[k] for k in ids], faces, [uv[k] for k in ids], mat,
             uv1=[uv1[k] for k in ids])
    assert glb.doc["nodes"] == doc["nodes"] and glb.doc["scenes"] == doc["scenes"]
    return glb, round(float(error)*1000, 4)


def main():
    standard = ROSE / "collision"
    dest = ROSE / "collision-low"
    dest.mkdir(exist_ok=True)
    records = []
    for source in json.loads((standard/"manifest.json").read_text())["petals"]:
        name = source["variantId"]
        path = standard / (name + ".glb")
        assert hashlib.sha256(path.read_bytes()).hexdigest() == source["sha256"]
        model, error = simplify(path)
        target = dest / path.name
        model.save(target)
        records.append(dict(variantId=name, triangles=152, maxBoundaryErrorMm=error,
                            sourceSha256=source["sha256"], sha256=hashlib.sha256(target.read_bytes()).hexdigest()))
    (dest/"manifest.json").write_text(json.dumps(dict(source="rose/collision", petals=records), indent=2) + "\n")
    print(f"{len(records)} variants; 152 triangles; worst perimeter error {max(r['maxBoundaryErrorMm'] for r in records)} mm")


if __name__ == "__main__":
    main()
