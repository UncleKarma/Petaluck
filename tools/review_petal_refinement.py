"""Measure actual exported edge refinement and render before/after geometry.

Requires immutable local snapshots from the pre-refinement and curl-only revisions.
No image synthesis: previews rasterize the exported GLB meshes and original textures.
"""
import argparse
import json
from pathlib import Path
from tempfile import TemporaryDirectory

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import generate_rose_assets as generator
import create_review_assemblies as assembly
from create_review_assemblies import ROOT, OUT, read_glb, mesh_data
from render_rose_review import render


def read_mesh(path):
    doc, data = read_glb(path)
    vertices, faces, _ = mesh_data(doc, data, doc['meshes'][0]['primitives'][0])
    return np.array(vertices), np.array(faces)


def perimeter(grid):
    # Exclude the short attachment/root edge from the visible blade contour measurement.
    return np.concatenate([grid[:, 0], grid[-1, 1:], grid[-2::-1, -1]])


def chord_error(polyline, reference, subdivisions):
    assert len(reference) == (len(polyline)-1)*subdivisions+1
    start = polyline[:-1, None, :]
    delta = np.diff(polyline, axis=0)[:, None, :]
    points = reference[:-1].reshape(-1, subdivisions, 3)
    u = np.clip(np.sum((points-start)*delta, axis=2) / np.sum(delta*delta, axis=2), 0, 1)
    distance = np.linalg.norm(points-start-u[:, :, None]*delta, axis=2)
    return float(distance.max())


def measure(curled_before):
    manifest = json.loads((OUT/'manifest.json').read_text())
    old_manifest = json.loads((curled_before/'manifest.json').read_text())
    records = []
    rows, columns = generator.PETAL_ROWS, generator.PETAL_COLUMNS.copy()
    limit = generator.PETAL_TRIANGLE_LIMIT
    try:
        # A denser analytic reference measures curvature error independently of either mesh.
        generator.PETAL_ROWS = (rows-1)*4+1
        generator.PETAL_COLUMNS = {layer: (count-1)*4+1 for layer, count in columns.items()}
        # Dense reference stays in memory and is never exported as a production asset.
        generator.PETAL_TRIANGLE_LIMIT = 4*generator.PETAL_ROWS*max(generator.PETAL_COLUMNS.values())
        for spec, old, rec in zip(generator.SPECS, old_manifest['petals'], manifest['petals']):
            name = rec['variantId']
            assert name == old['variantId'] == spec[0]
            v, faces = read_mesh(OUT/(name+'.glb'))
            assert len(faces) <= limit
            previous, _ = read_mesh(curled_before/(name+'.glb'))
            grid = v.reshape(2, rec['surfaceRows'], rec['surfaceColumns'], 3)
            old_grid = previous.reshape(2, old['surfaceRows'], old['surfaceColumns'], 3)
            assert np.array_equal(grid[:, ::2, ::2], old_grid), f'{name}: moved an original sample'
            # Every exported triangle must have nonzero area, including the thin closed rim.
            triangles = v[faces]
            area = np.linalg.norm(np.cross(triangles[:, 1]-triangles[:, 0], triangles[:, 2]-triangles[:, 0]), axis=1)
            assert area.min() > 1e-12, f'{name}: degenerate face'
            points, _, _ = generator.refined_petal(spec)
            reference = np.array(points).reshape(
                2, generator.PETAL_ROWS, generator.PETAL_COLUMNS[rec['layer']], 3)
            before_error = chord_error(perimeter(old_grid[0]), perimeter(reference[0]), 8)
            after_error = chord_error(perimeter(grid[0]), perimeter(reference[0]), 4)
            assert after_error < before_error*.55, f'{name}: insufficient edge smoothing: {before_error} -> {after_error}'
            # Consistent 70%-length cross sections compare the three corresponding families.
            cross_section = grid[0, round(.7*(rows-1))]
            tangent = np.diff(cross_section, axis=0)
            tangent /= np.linalg.norm(tangent, axis=1)[:, None]
            turn = np.degrees(np.arccos(np.clip(np.sum(tangent[:-1]*tangent[1:], axis=1), -1, 1))).sum()
            records.append({'variant': name, 'triangles': len(faces), 'originalSamplesUnchanged': True,
                            'maxEdgeChordErrorBeforeMm': before_error*1000,
                            'maxEdgeChordErrorAfterMm': after_error*1000,
                            'crossSectionTurnDegrees': float(turn)})
    finally:
        generator.PETAL_ROWS, generator.PETAL_COLUMNS = rows, columns
        generator.PETAL_TRIANGLE_LIMIT = limit
    for i in range(4):
        assert records[i]['crossSectionTurnDegrees'] < records[i+4]['crossSectionTurnDegrees'] < records[i+8]['crossSectionTurnDegrees']
    report = {'passed': True, 'assetLibraryVersion': manifest['assetLibraryVersion'], 'petals': records,
              'max30PetalTriangles': manifest['max30PetalTriangles'],
              'scope': 'Actual GLB vertex preservation, nondegenerate faces and contour approximation; paired-family cross-section comparison. Not headset appearance or performance.'}
    (ROOT/'docs/petal-smooth-edges-v1-measurement.json').write_text(json.dumps(report, indent=2)+'\n')
    return report


def previews(before):
    font = ImageFont.load_default(size=22)
    canvas = Image.new('RGB', (2400, 1480), '#f5f4f0')
    draw = ImageDraw.Draw(canvas)
    draw.text((20, 15), 'All 12 petals / BEFORE and AFTER / actual GLB, offline diagnostic lighting', fill='#333333', font=font)
    for row, layer in enumerate(('outer', 'middle', 'inner')):
        for column in range(4):
            name = f'{layer}_{column+1:02d}'
            x, y = column*600, 60+row*470
            for offset, label, source in [(0, 'Before', before), (300, 'After', OUT)]:
                canvas.paste(render(source/(name+'.glb'), (0, 0, 1), (300, 425), False, 3300, (0, .044, 0)), (x+offset, y+30))
                draw.text((x+offset+10, y), name+' / '+label, fill='#333333', font=font)
    canvas.save(ROOT/'art/petal-smooth-edges-v1-review.png')

    params = json.loads((ROOT/'app/build/reports/rose-layouts.json').read_text())
    config = next(c for c in params['rounds'] if c['count']==24 and c['seed']==7 and c['colorId']==1)
    canvas = Image.new('RGB', (1800, 1400), '#f5f4f0')
    draw = ImageDraw.Draw(canvas)
    draw.text((20, 15), '24-petal production rose / BEFORE and AFTER / offline Unlit, not a headset capture', fill='#333333', font=font)
    original_dest = assembly.DEST
    try:
        with TemporaryDirectory(prefix='petal-refinement-review-') as temp:
            assembly.DEST = Path(temp)
            for row, (label, source) in enumerate([('Before', before), ('After', OUT)]):
                path = assembly.assemble(config, params, label, source)
                for column, (view, direction) in enumerate([('Front', (0, .25, 1)), ('Oblique', (1, .3, 1)), ('Side', (1, .08, .15))]):
                    x, y = column*600, 60+row*660
                    canvas.paste(render(path, direction, (600, 620), True, 2900, (0, -.022, .025)), (x, y+30))
                    draw.text((x+15, y), label+' / '+view, fill='#333333', font=font)
    finally:
        assembly.DEST = original_dest
    canvas.save(ROOT/'art/rose-petal-refinement-v1-review.png')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--before', type=Path, required=True)
    parser.add_argument('--curled-before', type=Path, required=True)
    args = parser.parse_args()
    report = measure(args.curled_before)
    print(json.dumps(report), flush=True)
    previews(args.before)
