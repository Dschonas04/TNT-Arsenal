# Autor: Dschonas04 (https://github.com/Dschonas04)
"""Renders the block models isometrically into the preview sheets in docs/ — pure Python,
a little rasteriser with a depth buffer that reads the models and textures as the game would.

    python3 tools/preview.py
"""
import json
import math
import os
import struct
import sys
import zlib

"""Lays PNGs side by side, scaled up, into one sheet (pure Python). Usage: sheet.py out.png scale file..."""
import struct
import sys
import zlib


def read_png(path):
    data = open(path, "rb").read()
    pos, idat, w, h, ct = 8, b"", 0, 0, 6
    plte = None
    trns = b""
    bd = 8
    while pos < len(data):
        n = struct.unpack(">I", data[pos:pos + 4])[0]
        t = data[pos + 4:pos + 8]
        d = data[pos + 8:pos + 8 + n]
        if t == b"IHDR":
            w, h, bd, ct = struct.unpack(">IIBB", d[:10])
        elif t == b"tRNS":
            trns = d
        elif t == b"PLTE":
            plte = [tuple(d[i:i + 3]) for i in range(0, len(d), 3)]
        elif t == b"IDAT":
            idat += d
        pos += 12 + n
    raw = zlib.decompress(idat)
    chans = {6: 4, 2: 3, 3: 1, 4: 2, 0: 1}[ct]
    bpp = max(1, chans * bd // 8)
    stride = (w * chans * bd + 7) // 8
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    if bd < 8:
        unpacked = []
        for line in rows:
            vals = bytearray()
            for x in range(w):
                bit = x * bd
                vals.append((line[bit // 8] >> (8 - bd - bit % 8)) & ((1 << bd) - 1))
            unpacked.append(vals)
        rows = unpacked
    elif bd == 16:
        rows = [line[::2] for line in rows]
    px = []
    for line in rows:
        for x in range(w):
            if ct == 6:
                px.append(tuple(line[x * 4:x * 4 + 4]))
            elif ct == 2:
                px.append(tuple(line[x * 3:x * 3 + 3]) + (255,))
            elif ct == 3:
                px.append(plte[line[x]] + ((trns[line[x]] if line[x] < len(trns) else 255),))
            elif ct == 4:
                px.append((line[x * 2],) * 3 + (line[x * 2 + 1],))
            else:
                key = struct.unpack(">H", trns[:2])[0] if len(trns) >= 2 else -1
                px.append((line[x],) * 3 + ((0 if line[x] == key else 255),))
    return w, h, px


def write_png(path, w, h, px):
    raw = b"".join(b"\x00" + bytes(c for p in px[y * w:(y + 1) * w] for c in p) for y in range(h))

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    open(path, "wb").write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                           + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))





K = 7            # pixels per model unit
CELL_W, CELL_H = 26 * K, 40 * K
cache = {}


def tex(ref):
    ns, path = ref.split(":")
    if ref not in cache:
        cache[ref] = read_png(os.path.join(assets, ns, "textures", path + ".png"))
    return cache[ref]


def rotate(p, rot):
    if not rot:
        return p
    a = math.radians(rot["angle"])
    ox, oy, oz = rot["origin"]
    x, y, z = p[0] - ox, p[1] - oy, p[2] - oz
    c, s = math.cos(a), math.sin(a)
    if rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif rot["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + ox, y + oy, z + oz)


FACES = {  # corners (as indices into from/to) and shade
    "up": ([(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], 1.0),
    "down": ([(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], 0.5),
    "north": ([(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)], 0.8),
    "south": ([(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)], 0.8),
    "west": ([(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)], 0.6),
    "east": ([(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)], 0.6),
}


def uv_of(face, p):
    x, y, z = p
    return {"up": (x, z), "down": (x, 16 - z), "north": (16 - x, 16 - y), "south": (x, 16 - y),
            "west": (z, 16 - y), "east": (16 - z, 16 - y)}[face]


def render(model):
    textures = model["textures"]
    w, h = CELL_W, CELL_H
    buf = [(0, 0, 0, 0)] * (w * h)
    depth = [-1e9] * (w * h)

    def project(p):
        x, y, z = p
        x, z = 16 - x, 16 - z  # seen from the north-west, so the fronts of the figures show
        sx = (x - z) * 0.7071 * K + w / 2
        sy = h - 6 * K - (y * 0.8165 - (x + z) * 0.2887 + 4.6) * K
        return sx, sy, x + y + z

    for e in model["elements"]:
        f, t = e["from"], e["to"]
        for face, spec in e["faces"].items():
            corners, shade = FACES[face]
            pts3 = [rotate((f[0] if cx == 0 else t[0], f[1] if cy == 0 else t[1], f[2] if cz == 0 else t[2]), e.get("rotation"))
                    for cx, cy, cz in corners]
            raw = [(f[0] if cx == 0 else t[0], f[1] if cy == 0 else t[1], f[2] if cz == 0 else t[2]) for cx, cy, cz in corners]
            ref = textures[spec["texture"][1:]]
            while ref.startswith("#"):
                ref = textures[ref[1:]]
            tw, th, tpx = tex(ref)
            flat = None
            if "uv" in spec and spec["uv"][2] - spec["uv"][0] < 4:
                u = (spec["uv"][0] + spec["uv"][2]) / 2 * tw / 16
                v = (spec["uv"][1] + spec["uv"][3]) / 2 * th / 16
                flat = tpx[int(v) * tw + int(u)]
            pr = [project(p) for p in pts3]
            for tri in ((0, 1, 2), (0, 2, 3)):
                A, B, C = (pr[i] for i in tri)
                RA, RB, RC = (raw[i] for i in tri)
                minx, maxx = int(max(0, min(A[0], B[0], C[0]))), int(min(w - 1, max(A[0], B[0], C[0]) + 1))
                miny, maxy = int(max(0, min(A[1], B[1], C[1]))), int(min(h - 1, max(A[1], B[1], C[1]) + 1))
                den = (B[1] - C[1]) * (A[0] - C[0]) + (C[0] - B[0]) * (A[1] - C[1])
                if abs(den) < 1e-9:
                    continue
                for py in range(miny, maxy + 1):
                    for px in range(minx, maxx + 1):
                        X, Y = px + 0.5, py + 0.5
                        a = ((B[1] - C[1]) * (X - C[0]) + (C[0] - B[0]) * (Y - C[1])) / den
                        b = ((C[1] - A[1]) * (X - C[0]) + (A[0] - C[0]) * (Y - C[1])) / den
                        c = 1 - a - b
                        if a < -1e-6 or b < -1e-6 or c < -1e-6:
                            continue
                        d = a * A[2] + b * B[2] + c * C[2]
                        i = py * w + px
                        if d <= depth[i]:
                            continue
                        if flat is not None:
                            col = flat
                        else:
                            q = tuple(a * RA[k] + b * RB[k] + c * RC[k] for k in range(3))
                            u, v = uv_of(face, q)
                            if "uv" in spec:  # mapped whole: stretch the face over the texture
                                lo = [min(r_[k] for r_ in raw) for k in range(3)]
                                hi = [max(r_[k] for r_ in raw) for k in range(3)]
                                ax = {"up": (0, 2), "down": (0, 2), "north": (0, 1), "south": (0, 1), "west": (2, 1), "east": (2, 1)}[face]
                                fu = (q[ax[0]] - lo[ax[0]]) / max(1e-6, hi[ax[0]] - lo[ax[0]])
                                fv = (q[ax[1]] - lo[ax[1]]) / max(1e-6, hi[ax[1]] - lo[ax[1]])
                                if face in ("north", "east"):
                                    fu = 1 - fu
                                if face not in ("up", "down"):
                                    fv = 1 - fv
                                u, v = fu * 16, fv * 16
                            col = tpx[min(th - 1, int(v / 16 * th)) * tw + min(tw - 1, int(u / 16 * tw))]
                        if col[3] == 0:
                            continue
                        depth[i] = d
                        buf[i] = (int(col[0] * shade), int(col[1] * shade), int(col[2] * shade), 255)
    return buf




def sheet(path, where, names, labels, cols=8):
    global assets
    assets = where
    rows = (len(names) + cols - 1) // cols
    W, H = cols * CELL_W, rows * (CELL_H + 16)
    out = [(0, 0, 0, 0)] * (W * H)
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import textures as T
    for n, name in enumerate(names):
        model = json.load(open(os.path.join(assets, "tnt_arsenal", "models", "block", name + ".json")))
        img = render(model)
        ox, oy = (n % cols) * CELL_W, (n // cols) * (CELL_H + 16)
        for y in range(CELL_H):
            for x in range(CELL_W):
                p = img[y * CELL_W + x]
                if p[3]:
                    out[(oy + y) * W + ox + x] = p
        label = labels[n]
        T.text(out, W, ox + (CELL_W - T.text_width(label)) // 2, oy + CELL_H - 2, label, (150, 150, 150, 255))
    write_png(path, W, H, out)


def main():
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import textures as T
    import nuclear_assets as A
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    assets = os.path.join(repo, "src", "main", "resources", "assets")
    sheet(os.path.join(repo, "docs", "textures.png"), assets, [k["id"] for k in T.K], [k["en"].replace(" TNT", "") for k in T.K])
    sheet(os.path.join(repo, "docs", "nuclear.png"), assets, [b["id"] for b in A.BOMBS], [b["en"] for b in A.BOMBS], cols=3)
    print("ok")


if __name__ == "__main__":
    main()
