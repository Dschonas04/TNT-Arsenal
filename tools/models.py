"""The 3D models of every TNT: each kind is an object of its own — a fuel barrel,
a round bomb on a stand, a crate of building material, a drilling machine, a
crystal in a cage, a rocket on its ramp — so it can be told apart at a glance.

Textures are calm 16x16 materials (painted metal with a sheen, planks, stone,
glass, glowing cores) plus one small sign per kind with its emblem. Faces take
their UV from their position, so the materials keep one pixel density across a
model, like vanilla blocks; only signs are mapped whole.

    models.build(write_png, write_json, root, ns, kinds, icons)
"""
import colorsys
import math
import os

N = 16
# how big the figure on top of the block is, relative to a full block
FIGURE_SCALE = 0.62


# --- colours -------------------------------------------------------------------

def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def tone(c, dl, ds=0.0):
    """Lighter or darker along a hue-shifted ramp: shadows lean blue, light leans warm."""
    h, l, s = colorsys.rgb_to_hls(*(v / 255 for v in c))
    target = 0.62 if dl < 0 else 0.13
    diff = ((target - h + 0.5) % 1.0) - 0.5
    h = (h + max(-0.03, min(0.03, diff)) * min(1, abs(dl) * 4)) % 1.0
    l = max(0, min(1, l + dl))
    s = max(0, min(1, s + ds))
    return tuple(int(round(v * 255)) for v in colorsys.hls_to_rgb(h, l, s))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# --- materials -----------------------------------------------------------------
# Each painter returns 256 RGB tuples. They are smooth on purpose: shading in
# bands, a highlight, edges — no random speckle.

def m_metal(c):
    """Painted sheet metal with a soft vertical sheen and a seam."""
    c = hexc(c)
    across = [-0.10, -0.05, 0.0, 0.05, 0.11, 0.14, 0.09, 0.04, 0.0, -0.02, -0.04, -0.06, -0.08, -0.11, -0.14, -0.17]
    out = []
    for y in range(N):
        for x in range(N):
            v = tone(c, across[x] - 0.03 * (y / 15))
            if y == 0:
                v = tone(v, 0.06)
            out.append(v)
    return out


def m_metal_top(c):
    """A lid: a rim, a recessed middle, a bung."""
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            d = max(abs(x - 7.5), abs(y - 7.5))
            v = tone(c, 0.05) if d > 6.5 else tone(c, -0.04) if d > 5.5 else tone(c, -0.08 + 0.01 * (x - y) / 4)
            if (x - 11) ** 2 + (y - 4) ** 2 <= 2:
                v = tone(c, -0.2)
            if (x - 11) ** 2 + (y - 4) ** 2 == 0:
                v = tone(c, 0.1)
            out.append(v)
    return out


def m_band(c):
    """A steel hoop or strap: light top edge, dark bottom edge, rivets."""
    c = hexc(c)
    rows = [0.16, 0.06, 0.0, 0.0, -0.04, -0.08, -0.12, -0.2] * 2
    out = []
    for y in range(N):
        for x in range(N):
            v = tone(c, rows[y])
            if x % 8 == 3 and y % 8 in (2, 3):
                v = tone(c, 0.24 if y % 8 == 2 else -0.06)
            out.append(v)
    return out


def m_planks(c):
    """Horizontal boards, four pixels high, with a little grain."""
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            r = y % 4
            v = tone(c, [0.06, 0.0, -0.02, -0.16][r])
            board = y // 4
            if r == 1 and (x + board * 5) % 9 in (0, 1):
                v = tone(c, -0.06)
            if (x + board * 7) % 16 == 0 and r != 3:
                v = tone(c, -0.12)  # the end of a board
            out.append(v)
    return out


def m_beam(c):
    """A darker frame board, grain along it."""
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            v = tone(c, -0.04 + (0.05 if x % 4 == 1 else -0.03 if x % 4 == 3 else 0))
            if (y * 3 + x) % 11 == 0:
                v = tone(c, -0.1)
            out.append(v)
    return out


def m_bricks(c):
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            off = 0 if (y // 4) % 2 == 0 else 4
            if y % 4 == 3 or (x + off) % 8 == 7:
                v = tone(c, -0.17)
            elif y % 4 == 0 or (x + off) % 8 == 0:
                v = tone(c, 0.06)
            else:
                v = tone(c, 0.0 if ((x + off) // 8 + y // 4) % 2 else -0.03)
            out.append(v)
    return out


def m_stone(c):
    """Rough stone: a few big, soft patches."""
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            n = math.sin(x * 0.9 + y * 0.4) + math.sin(y * 1.1 - x * 0.3) + math.sin((x + y) * 0.55)
            v = tone(c, 0.05 * round(n))
            out.append(v)
    return out


def m_glass(c):
    """Clear glass: a pale fill, a frame line, two diagonal glints."""
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            v = tone(c, 0.12)
            if (x - y) % 16 in (3, 4) or (x - y) % 16 == 9:
                v = tone(c, 0.3)
            if x in (0, 15) or y in (0, 15):
                v = tone(c, -0.12)
            out.append(v)
    return out


def m_glow(c):
    """A glowing core: bright in the middle, falling off to the edge."""
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - 7.5, y - 7.5) / 10.6
            out.append(mix(tone(c, 0.3), tone(c, -0.12), min(1, d)))
    return out


def m_plain(c):
    """A flat colour lit from above."""
    c = hexc(c)
    return [tone(c, 0.06 - 0.12 * y / 15) for y in range(N) for x in range(N)]


def m_stripes(c):
    """Yellow and black hazard stripes."""
    a, b = hexc(c), hexc("#1c1c1c")
    return [tone(a if (x + y) % 8 < 4 else b, 0.04 - 0.08 * y / 15) for y in range(N) for x in range(N)]


def m_leaves(c):
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            n = (x * 5 + y * 3) % 7
            out.append(tone(c, [0.08, 0.0, -0.06, 0.03, -0.1, 0.0, 0.05][n]))
    return out


def m_grid(c):
    """A cage or grating: bars every four pixels."""
    c = hexc(c)
    out = []
    for y in range(N):
        for x in range(N):
            bar = x % 4 == 0 or y % 4 == 0
            out.append(tone(c, 0.08 if bar else -0.25))
    return out


PAINTERS = {"metal": m_metal, "lid": m_metal_top, "band": m_band, "planks": m_planks, "beam": m_beam, "bricks": m_bricks,
            "stone": m_stone, "glass": m_glass, "glow": m_glow, "plain": m_plain, "stripes": m_stripes, "leaves": m_leaves,
            "grid": m_grid}


class Textures:
    """Collects the textures the models use and writes them once."""

    def __init__(self, ns):
        self.ns = ns
        self.images = {}

    def mat(self, kind, colour):
        name = f"m_{kind}_{colour.lstrip('#').lower()}"
        if name not in self.images:
            self.images[name] = [p + (255,) for p in PAINTERS[kind](colour)]
        return name

    def sign(self, kid, icon_rows, pal, plate):
        """A small enamel sign: a dark rim, a plate, the emblem with an outline."""
        name = f"sign_{kid}"
        plate = hexc(plate)
        rim = tone(plate, -0.35)
        px = []
        for y in range(N):
            for x in range(N):
                if x in (0, 15) or y in (0, 15):
                    px.append(rim)
                elif x == 1 or y == 1:
                    px.append(tone(plate, 0.1))
                elif x == 14 or y == 14:
                    px.append(tone(plate, -0.12))
                else:
                    px.append(tone(plate, 0.02 - 0.06 * y / 15))
        p = {k: hexc(v) for k, v in pal.items()}
        first = next(iter(p.values()))
        p.setdefault("o", tone(first, -0.3))
        p.setdefault("w", (255, 255, 255))
        rows = icon_rows
        h, w = len(rows), len(rows[0])
        ox, oy = (N - w) // 2, (N - h) // 2
        cells = {(x + ox, y + oy): ch for y, r in enumerate(rows) for x, ch in enumerate(r) if ch != "."}
        outline = tone(plate, -0.45)
        for (x, y) in cells:
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if (nx, ny) not in cells and 1 < nx < 14 and 1 < ny < 14:
                    px[ny * N + nx] = outline
        for (x, y), ch in cells.items():
            c = p.get(ch, first)
            lit = (x - 1, y) not in cells or (x, y - 1) not in cells
            px[y * N + x] = tone(c, 0.08) if lit else c
        self.images[name] = [q + (255,) for q in px]
        return name

    def write(self, write_png, root):
        for name, px in self.images.items():
            write_png(os.path.join(root, "assets", self.ns, "textures", "block", name + ".png"), N, N, px)


# --- building blocks --------------------------------------------------------------

class Model:
    """Elements are given in figure space (0..16) and placed scaled down on top of the
    TNT block: FIGURE_SCALE, centred, standing on y = 16."""

    def __init__(self, tex):
        self.tex = tex
        self.elements = []
        self.used = {}

    @staticmethod
    def place(p):
        s = FIGURE_SCALE
        return (8 + (p[0] - 8) * s, 16 + p[1] * s, 8 + (p[2] - 8) * s)

    def key(self, name):
        if name not in self.used:
            self.used[name] = f"t{len(self.used)}"
        return "#" + self.used[name]

    def box(self, a, b, tex, rot=None, faces=None, sign=None, sign_faces=()):
        """One cuboid from a to b, every face in material `tex` (a texture name) unless `faces`
        overrides some. A sign is mapped whole onto `sign_faces`."""
        x1, y1, z1 = a
        x2, y2, z2 = b
        assert x1 < x2 and y1 < y2 and z1 < z2, (a, b)
        a, b = self.place(a), self.place(b)
        assert -16 <= min(a) and max(b) <= 32, (a, b)
        fs = {}
        for f in ("down", "up", "north", "south", "west", "east"):
            t = (faces or {}).get(f, tex)
            if t is None:
                continue
            fs[f] = {"texture": self.key(t)}
        if sign:
            for f in sign_faces:
                fs[f] = {"texture": self.key(sign), "uv": [0, 0, 16, 16]}
        e = {"from": [round(v, 3) for v in a], "to": [round(v, 3) for v in b], "faces": fs}
        if rot:
            axis, angle, origin = rot
            e["rotation"] = {"angle": angle, "axis": axis, "origin": [round(v, 3) for v in self.place(origin)]}
        self.elements.append(e)
        return self

    def json(self, kid):
        """The TNT block with the figure on top."""
        ns = self.tex.ns
        textures = {v: f"{ns}:block/{k}" for k, v in self.used.items()}
        textures.update({"particle": f"{ns}:block/{kid}_side", "side": f"{ns}:block/{kid}_side",
                         "top": f"{ns}:block/{kid}_top", "bottom": f"{ns}:block/{kid}_bottom"})
        cube = {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            "down": {"texture": "#bottom", "cullface": "down"}, "up": {"texture": "#top"},
            "north": {"texture": "#side", "cullface": "north"}, "south": {"texture": "#side", "cullface": "south"},
            "west": {"texture": "#side", "cullface": "west"}, "east": {"texture": "#side", "cullface": "east"}}}
        return {"parent": "minecraft:block/block", "textures": textures, "elements": [cube] + self.elements,
                # block plus figure is about one and a half blocks tall: a little smaller in the inventory
                "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]},
                            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.22, 0.22, 0.22]},
                            "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.42, 0.42, 0.42]}}}


def cylinder(m, cx, cz, y1, y2, r, side, top=None, bottom=None):
    """A round column the Minecraft way: a stepped circle of three boxes that do not
    overlap (so no two lids flicker against each other)."""
    top = top or side
    bottom = bottom or side
    w = r * 0.72
    f = {"up": top, "down": bottom}
    m.box((cx - r, y1, cz - w), (cx + r, y2, cz + w), side, faces=f)
    m.box((cx - w, y1, cz + w), (cx + w, y2, cz + r), side, faces=f)
    m.box((cx - w, y1, cz - r), (cx + w, y2, cz - w), side, faces=f)


def sphere(m, cx, cy, cz, r, tex, slices=5):
    """A ball of eight-sided slices."""
    for i in range(slices):
        a = -1 + 2 * i / slices
        b = -1 + 2 * (i + 1) / slices
        mid = (a + b) / 2
        rr = r * math.sqrt(max(0.05, 1 - mid * mid))
        cylinder(m, cx, cz, cy + a * r, cy + b * r, rr, tex)


def signs(m, sign, y1, y2, inset, width, sides=("north", "south", "east", "west")):
    """Square signs on the sides of an object whose faces lie `inset` from the block edge."""
    half = width / 2
    for side in sides:
        if side == "north":
            m.box((8 - half, y1, inset - 0.1), (8 + half, y2, inset), None, sign=sign, sign_faces=("north",))
        elif side == "south":
            m.box((8 - half, y1, 16 - inset), (8 + half, y2, 16 - inset + 0.1), None, sign=sign, sign_faces=("south",))
        elif side == "west":
            m.box((inset - 0.1, y1, 8 - half), (inset, y2, 8 + half), None, sign=sign, sign_faces=("west",))
        else:
            m.box((16 - inset, y1, 8 - half), (16 - inset + 0.1, y2, 8 + half), None, sign=sign, sign_faces=("east",))


def fuse(m, x, y, z, h=3):
    """A cord fuse with a glowing tip."""
    T = m.tex
    m.box((x - 0.5, y, z - 0.5), (x + 0.5, y + h, z + 0.5), T.mat("plain", "#c9b28a"))
    m.box((x - 0.6, y + h, z - 0.6), (x + 0.6, y + h + 0.8, z + 0.6), T.mat("glow", "#ffb12b"))


# --- families -----------------------------------------------------------------------

def barrel(m, body, band, sign, lid=None):
    """A steel drum with two hoops and signs on four sides."""
    T = m.tex
    cylinder(m, 8, 8, 0, 15, 6.2, T.mat("metal", body), top=T.mat("lid", lid or body), bottom=T.mat("lid", body))
    for y in (2, 11.5):
        cylinder(m, 8, 8, y, y + 1.5, 6.6, T.mat("band", band), top=T.mat("band", band))
    signs(m, sign, 4.5, 10.5, 8 - 6.2, 6)


def crate(m, wood, frame, sign):
    """A wooden crate: planked sides inside a frame, signs front and back."""
    T = m.tex
    m.box((1, 0, 1), (15, 13, 15), T.mat("planks", wood), faces={"up": T.mat("planks", wood)})
    for x, z in ((0.5, 0.5), (13, 0.5), (0.5, 13), (13, 13)):
        m.box((x, 0, z), (x + 2.5, 13.5, z + 2.5), T.mat("beam", frame))
    for y in (0, 11):
        m.box((0.5, y, 0.5), (15.5, y + 2.5, 15.5), T.mat("beam", frame), faces={"up": T.mat("beam", frame)})
    signs(m, sign, 3.5, 10, 0.5, 6.5)


def bomb(m, colour, r, sign, stand="#6b4a2b"):
    """A round bomb with a neck and fuse sitting in a wooden cradle."""
    T = m.tex
    m.box((3, 0, 3), (13, 2, 13), T.mat("beam", stand))
    m.box((4, 2, 2.5), (12, 4, 4), T.mat("beam", stand))
    m.box((4, 2, 12), (12, 4, 13.5), T.mat("beam", stand))
    cy = 2 + r
    sphere(m, 8, cy, 8, r, T.mat("metal", colour), slices=6)
    cylinder(m, 8, 8, cy + r - 0.5, cy + r + 1.2, 1.6, T.mat("band", "#8a8f96"))
    fuse(m, 8, cy + r + 1.2, 8, 2.2)
    m.box((8 - 2.5, cy - 2.5, 8 - r - 0.1), (8 + 2.5, cy + 2.5, 8 - r + 0.5), None, sign=sign, sign_faces=("north",))
    m.box((8 - 2.5, cy - 2.5, 8 + r - 0.5), (8 + 2.5, cy + 2.5, 8 + r + 0.1), None, sign=sign, sign_faces=("south",))


def pedestal(m, stone, trim):
    """A stone plinth for orbs and crystals."""
    T = m.tex
    m.box((1.5, 0, 1.5), (14.5, 2, 14.5), T.mat("bricks", stone))
    m.box((3, 2, 3), (13, 4, 13), T.mat("stone", stone))
    m.box((2.5, 4, 2.5), (13.5, 5, 13.5), T.mat("band", trim))


def cage(m, metal, y1, y2, r=5.5):
    """Four corner posts and two rings around a core."""
    T = m.tex
    for x, z in ((8 - r, 8 - r), (8 + r - 1, 8 - r), (8 - r, 8 + r - 1), (8 + r - 1, 8 + r - 1)):
        m.box((x, y1, z), (x + 1, y2, z + 1), T.mat("band", metal))
    for y in (y1, y2 - 1):
        m.box((8 - r, y, 8 - r), (8 + r, y + 1, 8 - r + 1), T.mat("band", metal))
        m.box((8 - r, y, 8 + r - 1), (8 + r, y + 1, 8 + r), T.mat("band", metal))
        m.box((8 - r, y, 8 - r), (8 - r + 1, y + 1, 8 + r), T.mat("band", metal))
        m.box((8 + r - 1, y, 8 - r), (8 + r, y + 1, 8 + r), T.mat("band", metal))


def machine(m, body, sign, base="#3a3f44"):
    """A boxy machine housing on skids."""
    T = m.tex
    m.box((1, 0, 2), (15, 1.5, 4), T.mat("metal", base))
    m.box((1, 0, 12), (15, 1.5, 14), T.mat("metal", base))
    m.box((2, 1.5, 2), (14, 10, 14), T.mat("metal", body), faces={"up": T.mat("lid", body)})
    m.box((1.5, 9, 1.5), (14.5, 10.5, 14.5), T.mat("band", base))
    signs(m, sign, 3, 8.5, 2, 5.5)


def rocket(m, body, tip, fins, x=8, z=8, y=1, h=11, r=2.2):
    T = m.tex
    cylinder(m, x, z, y, y + h, r, T.mat("metal", body), top=T.mat("lid", body))
    cylinder(m, x, z, y + h, y + h + 2, r * 0.7, T.mat("metal", tip))
    cylinder(m, x, z, y + h + 2, y + h + 3.2, r * 0.35, T.mat("metal", tip))
    m.box((x - r - 2, y, z - 0.3), (x + r + 2, y + 4, z + 0.3), T.mat("metal", fins))
    m.box((x - 0.3, y, z - r - 2), (x + 0.3, y + 4, z + r + 2), T.mat("metal", fins))


def pot(m, clay, soil):
    T = m.tex
    m.box((3, 0, 3), (13, 6, 13), T.mat("plain", clay), faces={"up": T.mat("plain", soil)})
    m.box((2.5, 5, 2.5), (13.5, 7, 13.5), T.mat("band", clay), faces={"up": T.mat("plain", soil)})
    m.box((3.5, 6.9, 3.5), (12.5, 7.1, 12.5), T.mat("stone", soil))


# --- every kind ----------------------------------------------------------------------

def make(kid, k, T, sign):
    """The model of one kind; `sign` is its sign texture."""
    m = Model(T)
    body = k["body"]
    M = T.mat
    if kid == "mega_tnt":
        bomb(m, "#2a2a32", 5, sign)
    elif kid == "giga_tnt":
        bomb(m, "#3a1414", 5.6, sign)
        for y in (4.5, 10.5):
            cylinder(m, 8, 8, y, y + 1, 5.3 if y < 8 else 4.4, M("band", "#c9a227"))
    elif kid == "cluster_tnt":
        crate(m, "#a8783a", "#6b4a2b", sign)
        for x, z, h in ((4.5, 4.5, 0), (11.5, 4.5, 0.5), (4.5, 11.5, 0.3), (11.5, 11.5, 0)):
            sphere(m, x, 15.6 + h, z, 2.6, M("metal", "#2a2a32"), slices=4)
            fuse(m, x, 18 + h, z, 1)
    elif kid == "chain_tnt":
        bomb(m, "#7a1a14", 4.6, sign)
        for i, y in enumerate((1, 4, 7)):
            if i % 2:
                m.box((13, y, 7.5), (15, y + 3, 8.5), M("band", "#9aa0a8"))
            else:
                m.box((13.5, y, 7), (14.5, y + 3, 9), M("band", "#9aa0a8"))
    elif kid in ("fire_tnt", "water_tnt", "lava_tnt", "poison_tnt", "frost_tnt", "sponge_tnt", "smoke_tnt"):
        colours = {"fire_tnt": ("#c8321f", "#3a3a3a", None), "water_tnt": ("#2f6fc0", "#c4ccd4", None),
                   "lava_tnt": ("#3a2a26", "#ff7a1a", None), "poison_tnt": ("#4e8a2a", "#2a3a1e", None),
                   "frost_tnt": ("#dff2ff", "#6fb8de", None), "sponge_tnt": ("#d9c23a", "#8a7a20", None),
                   "smoke_tnt": ("#6c737a", "#3a3f44", None)}[kid]
        barrel(m, colours[0], colours[1], sign)
        if kid == "fire_tnt":
            for x, z, h in ((8, 8, 5), (6, 9, 3), (10, 7, 3.5)):
                m.box((x - 1, 15, z - 1), (x + 1, 15 + h, z + 1), M("glow", "#ff7a1a"))
                m.box((x - 0.5, 15 + h, z - 0.5), (x + 0.5, 15 + h + 1.2, z + 0.5), M("glow", "#ffe066"))
        elif kid == "water_tnt":
            cylinder(m, 8, 8, 15, 15.4, 5, M("glass", "#3a8fe0"))
        elif kid == "lava_tnt":
            cylinder(m, 8, 8, 15, 15.4, 5, M("glow", "#ff8a1a"))
            for y in (2, 11.5):
                pass
        elif kid == "poison_tnt":
            for x, z, r in ((6, 7, 1.4), (9.5, 9, 1.8), (8, 5.5, 1)):
                sphere(m, x, 15.4 + r * 0.5, z, r, M("glow", "#7fe03a"), slices=3)
        elif kid == "frost_tnt":
            for x, z, h in ((7, 7, 5), (9.5, 9, 3.5), (6, 10, 2.5)):
                m.box((x - 0.9, 15, z - 0.9), (x + 0.9, 15 + h, z + 0.9), M("glass", "#bfe8ff"), rot=("y", 45, (x, 15, z)))
        elif kid == "sponge_tnt":
            m.box((5, 15, 5), (11, 18, 11), M("plain", "#e3cc4a"))
        elif kid == "smoke_tnt":
            cylinder(m, 8, 8, 15, 17.5, 1.2, M("metal", "#3a3f44"))
            sphere(m, 8, 19.5, 8, 2.2, M("plain", "#c4c8cc"), slices=3)
    elif kid == "tower_tnt":
        # a round castle tower: stone base, crenellations, windows, a red cone roof with a flag
        stone = M("bricks", "#9a9a9a")
        m.box((1, 0, 1), (15, 1.5, 15), M("stone", "#7a7a7a"))
        cylinder(m, 8, 8, 1.5, 12, 4.5, stone)
        cylinder(m, 8, 8, 12, 13, 5.2, M("bricks", "#8a8a8a"))
        for x, z in ((3, 7), (12, 7), (7, 3), (7, 12)):
            m.box((x, 13, z), (x + 2, 14.5, z + 2), stone)
        for i, r in enumerate((4.6, 3.6, 2.6, 1.6, 0.7)):
            cylinder(m, 8, 8, 13 + i * 1.6, 14.6 + i * 1.6, r, M("plain", "#b83a2a"))
        m.box((7.8, 21, 7.8), (8.2, 25, 8.2), M("beam", "#6b4a2b"))
        m.box((8.2, 23, 7.9), (11, 24.6, 8.1), M("plain", "#ffd23a"))
        for a, b in (((7.2, 8, 3.4), (8.8, 10.5, 3.5)), ((7.2, 4, 3.4), (8.8, 6, 3.5)), ((12.5, 8, 7.2), (12.6, 10.5, 8.8))):
            m.box(a, b, M("plain", "#1a1a1a"))
        m.box((6.6, 1.5, 3.4), (9.4, 4.5, 3.5), M("beam", "#6b4a2b"))
        m.box((5.5, 0.2, 0.9), (10.5, 1.3, 1), None, sign=sign, sign_faces=("north",))
    elif kid in ("bridge_tnt", "wall_tnt", "platform_tnt", "staircase_tnt", "bunker_tnt", "dome_tnt", "torch_tnt"):
        crate(m, "#b98a4a", "#6b4a2b", sign)
        stone = M("bricks", "#8e8e8e")
        if kid == "bridge_tnt":
            m.box((2, 15, 6), (14, 16.5, 10), stone)
            m.box((3, 13.5, 6.5), (5, 15, 9.5), stone)
            m.box((11, 13.5, 6.5), (13, 15, 9.5), stone)
            m.box((2, 16.5, 6), (14, 17.5, 6.6), M("band", "#5a5a5a"))
            m.box((2, 16.5, 9.4), (14, 17.5, 10), M("band", "#5a5a5a"))
        elif kid == "wall_tnt":
            m.box((2, 13.5, 6), (14, 17, 10), stone)
            for x in (2, 6.5, 11):
                m.box((x, 17, 6), (x + 3, 18.5, 10), stone)
        elif kid == "platform_tnt":
            m.box((2, 16.5, 2), (14, 17.5, 14), stone)
            for x, z in ((3, 3), (12, 3), (3, 12), (12, 12)):
                m.box((x, 13.5, z), (x + 1, 16.5, z + 1), M("beam", "#6b4a2b"))
        elif kid == "staircase_tnt":
            for i in range(4):
                m.box((3 + i * 2.5, 13.5, 5), (5.5 + i * 2.5, 15 + i * 1.5, 11), stone)
        elif kid == "bunker_tnt":
            m.box((3, 13.5, 3), (13, 17, 13), M("stone", "#8a8a80"))
            m.box((4, 15, 2.9), (12, 16, 3), M("plain", "#111111"))
            m.box((4, 17, 4), (12, 17.5, 12), M("leaves", "#5f6a4a"))
        elif kid == "dome_tnt":
            sphere(m, 8, 13.5, 8, 5.5, M("glass", "#bfe8f5"), slices=6)
        elif kid == "torch_tnt":
            for x, z in ((5, 5), (11, 5), (8, 10)):
                m.box((x - 0.6, 13.5, z - 0.6), (x + 0.6, 18, z + 0.6), M("beam", "#8a5a2b"))
                m.box((x - 0.8, 18, z - 0.8), (x + 0.8, 19.4, z + 0.8), M("glow", "#ffb12b"))
    elif kid in ("drill_tnt", "tunnel_tnt", "miner_tnt", "leveler_tnt"):
        machine(m, {"drill_tnt": "#e0b21a", "tunnel_tnt": "#c87a1a", "miner_tnt": "#4a5a6a", "leveler_tnt": "#5f9a3a"}[kid], sign)
        steel = M("metal", "#b8bec6")
        if kid == "drill_tnt":
            m.box((7, 10, 7), (9, 18, 9), M("metal", "#3a3f44"))
            m.box((5, 18, 5), (11, 19.5, 11), M("metal", "#3a3f44"))
            for i in range(4):
                r = 2.6 - i * 0.55
                m.box((8 - r, 10.5 + i * 1.8, 8 - r), (8 + r, 11.8 + i * 1.8, 8 + r), steel, rot=("y", 22.5 if i % 2 else -22.5, (8, 11, 8)))
        elif kid == "tunnel_tnt":
            cylinder(m, 8, 8, 10, 11, 4.5, M("band", "#3a3f44"))
            m.box((4, 2.5, 0), (12, 9.5, 2), M("metal", "#3a3f44"))
            for i, r in enumerate((3.2, 2.2, 1.2)):
                m.box((8 - r, 6 - r, -1.5 + i * -0.01), (8 + r, 6 + r, 0 - i * 0.6), steel, rot=("z", 45 if i % 2 else 22.5, (8, 6, 0)))
        elif kid == "miner_tnt":
            m.box((3, 10.5, 3), (13, 14, 13), M("metal", "#3a3f44"), faces={"up": M("stone", "#5a5a5a")})
            for x, z, c in ((5, 5, "#3ec6d6"), (9, 6, "#f2c84a"), (6, 10, "#e8342a"), (10, 10, "#3ad672")):
                m.box((x, 14, z), (x + 2, 15.5, z + 2), M("glow", c), rot=("y", 45, (x + 1, 14, z + 1)))
        elif kid == "leveler_tnt":
            m.box((0, 0.5, 0), (16, 6, 1.5), steel)
            m.box((3, 3, 1.5), (5, 5, 3), M("metal", "#3a3f44"))
            m.box((11, 3, 1.5), (13, 5, 3), M("metal", "#3a3f44"))
            m.box((5, 10, 8), (11, 14, 13), M("glass", "#bfe8ff"))
    elif kid in ("ender_tnt", "end_tnt", "antimatter_tnt", "black_hole_tnt", "gravity_tnt", "antigravity_tnt", "stasis_tnt",
                 "xray_tnt", "sonic_tnt", "sculk_tnt", "nether_tnt"):
        stone, trim = {"ender_tnt": ("#1a1424", "#2aa58a"), "end_tnt": ("#d6cf8e", "#a87ac8"), "antimatter_tnt": ("#d8dce4", "#6ef0f0"),
                       "black_hole_tnt": ("#2a1840", "#9b5cff"), "gravity_tnt": ("#3a2a4a", "#b06cff"),
                       "antigravity_tnt": ("#2f6f7a", "#7af4ff"), "stasis_tnt": ("#2a4a6a", "#c9a227"),
                       "xray_tnt": ("#5a6068", "#bfeeff"), "sonic_tnt": ("#0d2a30", "#38e0e8"), "sculk_tnt": ("#0b2e3a", "#19a3b0"),
                       "nether_tnt": ("#4a1414", "#ff8a1a")}[kid]
        pedestal(m, stone, trim)
        for x, z, face in ((8, 2.9, "north"), (8, 13.1, "south")):
            pass
        m.box((5, 1, 2.9), (11, 4.5, 3), None, sign=sign, sign_faces=("north",))
        m.box((5, 1, 13), (11, 4.5, 13.1), None, sign=sign, sign_faces=("south",))
        if kid == "ender_tnt":
            sphere(m, 8, 10, 8, 4, M("glow", "#2aa58a"), slices=6)
            m.box((6.5, 8.5, 3.8), (9.5, 11.5, 4.2), M("plain", "#0b0b0b"))
            m.box((6.5, 8.5, 11.8), (9.5, 11.5, 12.2), M("plain", "#0b0b0b"))
        elif kid == "end_tnt":
            cage(m, "#a87ac8", 5, 15, 4.5)
            m.box((5.5, 7, 5.5), (10.5, 12, 10.5), M("glass", "#e8d8ff"), rot=("y", 45, (8, 9.5, 8)))
            m.box((6.8, 8.2, 6.8), (9.2, 10.8, 9.2), M("glow", "#ff6ad6"), rot=("x", 45, (8, 9.5, 8)))
        elif kid == "antimatter_tnt":
            cage(m, "#d8dce4", 5, 15, 5)
            sphere(m, 8, 10, 8, 3, M("glow", "#f4f8ff"), slices=5)
            m.box((2.5, 9.7, 7.5), (13.5, 10.3, 8.5), M("glow", "#6ef0f0"))
            m.box((7.5, 9.7, 2.5), (8.5, 10.3, 13.5), M("glow", "#6ef0f0"))
        elif kid == "black_hole_tnt":
            sphere(m, 8, 10.5, 8, 3.6, M("plain", "#05030a"), slices=6)
            m.box((1, 10.2, 5.5), (15, 10.8, 10.5), M("glow", "#ff8a2a"), rot=("x", 22.5, (8, 10.5, 8)))
            m.box((5.5, 10.2, 1), (10.5, 10.8, 15), M("glow", "#9b5cff"), rot=("z", 22.5, (8, 10.5, 8)))
        elif kid == "gravity_tnt":
            sphere(m, 8, 10.5, 8, 2.6, M("glow", "#b06cff"), slices=5)
            for x, y, z, c in ((3, 8, 7, "#8a8a8a"), (12, 12, 8, "#6b4a2b"), (8, 14.5, 3, "#5fa03a"), (7, 7, 13, "#8a8a8a")):
                m.box((x - 1, y - 1, z - 1), (x + 1, y + 1, z + 1), M("stone", c), rot=("y", 22.5, (x, y, z)))
        elif kid == "antigravity_tnt":
            for i, (x, y, z, s) in enumerate(((8, 7.5, 8, 3.5), (7.5, 11.5, 8.5, 2.6), (8.5, 15, 7.5, 1.8))):
                m.box((x - s, y, z - s), (x + s, y + s, z + s), M("stone", "#7a8a90"), faces={"up": M("leaves", "#5fa03a")})
                m.box((x - s - 0.3, y - 0.8, z - s - 0.3), (x + s + 0.3, y - 0.5, z + s + 0.3), M("glow", "#7af4ff"))
        elif kid == "stasis_tnt":
            m.box((4, 5, 4), (12, 6, 12), M("band", "#c9a227"))
            m.box((4, 15, 4), (12, 16, 12), M("band", "#c9a227"))
            cylinder(m, 8, 8, 6, 9.5, 3, M("glass", "#bfe8ff"))
            cylinder(m, 8, 8, 9.5, 11.5, 1, M("glass", "#bfe8ff"))
            cylinder(m, 8, 8, 11.5, 15, 3, M("glass", "#bfe8ff"))
            cylinder(m, 8, 8, 6, 7.5, 2.6, M("plain", "#e8c13a"))
            for x, z in ((4.2, 4.2), (11, 4.2), (4.2, 11), (11, 11)):
                m.box((x, 6, z), (x + 0.8, 15, z + 0.8), M("band", "#8a6a1e"))
        elif kid == "xray_tnt":
            m.box((7.5, 5, 7.5), (8.5, 9, 8.5), M("band", "#5a6068"))
            m.box((3, 9, 7.2), (13, 10.2, 8.8), M("band", "#c9a227"))
            m.box((3, 17, 7.2), (13, 18.2, 8.8), M("band", "#c9a227"))
            m.box((3, 9, 7.2), (4.2, 18.2, 8.8), M("band", "#c9a227"))
            m.box((11.8, 9, 7.2), (13, 18.2, 8.8), M("band", "#c9a227"))
            m.box((4.2, 10.2, 7.7), (11.8, 17, 8.3), M("glass", "#bfeeff"))
        elif kid == "sonic_tnt":
            m.box((4, 5, 4), (12, 13, 12), M("metal", "#0d2a30"))
            cylinder(m, 8, 3.9, 6.5, 11.5, 2.5, M("grid", "#38e0e8"))
            for i, r in enumerate((2.5, 3.8, 5)):
                m.box((8 - r, 13 + i * 1.6, 8 - r), (8 + r, 13.6 + i * 1.6, 8 + r), M("glow", "#38e0e8"))
        elif kid == "sculk_tnt":
            m.box((3, 5, 3), (13, 8, 13), M("stone", "#0b2e3a"), faces={"up": M("leaves", "#14535f")})
            for x, z in ((4.5, 4.5), (10.5, 4.5), (4.5, 10.5), (10.5, 10.5)):
                m.box((x, 8, z), (x + 1, 13, z + 1), M("plain", "#1a6a78"))
                m.box((x - 0.3, 13, z - 0.3), (x + 1.3, 14.4, z + 1.3), M("glow", "#7af4ff"))
        elif kid == "nether_tnt":
            m.box((4, 5, 7), (12, 6.5, 9), M("plain", "#1a1424"))
            m.box((4, 16, 7), (12, 17.5, 9), M("plain", "#1a1424"))
            m.box((4, 6.5, 7), (5.5, 16, 9), M("plain", "#1a1424"))
            m.box((10.5, 6.5, 7), (12, 16, 9), M("plain", "#1a1424"))
            m.box((5.5, 6.5, 7.6), (10.5, 16, 8.4), M("glow", "#8a3aff"))
    elif kid == "earthquake_tnt":
        # torn ground: two slabs of earth tilted apart, a glowing crack between them, rocks thrown up
        earth, grass = M("stone", "#7a5a3a"), M("leaves", "#5f9a3a")
        rock = M("stone", "#8a8a8a")
        m.box((0.5, 0, 1), (7.3, 6, 15), earth, faces={"up": grass}, rot=("z", 22.5, (7.3, 0, 8)))
        m.box((8.7, 0, 1), (15.5, 6, 15), earth, faces={"up": grass}, rot=("z", -22.5, (8.7, 0, 8)))
        m.box((5.5, 0, 1.5), (7, 1.5, 14.5), rock, rot=("z", 22.5, (7.3, 0, 8)))
        m.box((7, 0, 2), (9, 7.5, 14), M("glow", "#ff6a1a"))
        m.box((7.6, 7.5, 5), (8.4, 9, 11), M("glow", "#ffb03a"))
        for x, y, z, r in ((4, 12, 5, 1.6), (12, 13, 9.5, 1.4), (8, 16, 6.5, 1.1), (5.5, 14.5, 11.5, 1.2)):
            m.box((x - r, y - r, z - r), (x + r, y + r, z + r), rock, rot=("y", 22.5, (x, y, z)))
        m.box((2, 3.5, 0.95), (6, 7, 1), None, sign=sign, sign_faces=("north",))
    elif kid in ("lightning_tnt", "storm_tnt", "magnet_tnt", "shockwave_tnt"):
        machine(m, {"lightning_tnt": "#2a3a8c", "storm_tnt": "#3a4458", "magnet_tnt": "#b8262a", "shockwave_tnt": "#c27a2a"}[kid], sign)
        if kid == "lightning_tnt":
            cylinder(m, 8, 8, 10.5, 17, 1, M("band", "#b87333"))
            for y, r in ((11.5, 3), (13.5, 2.5), (15.5, 2)):
                cylinder(m, 8, 8, y, y + 0.8, r, M("band", "#b87333"))
            sphere(m, 8, 18.5, 8, 2, M("metal", "#c9ced4"), slices=4)
        elif kid == "storm_tnt":
            m.box((7.5, 10.5, 7.5), (8.5, 15, 8.5), M("band", "#8a8f96"))
            for x, z in ((6, 8), (10, 8), (8, 6.5)):
                sphere(m, x, 17, z, 2.4, M("plain", "#5a606b"), slices=3)
            m.box((7.5, 13.5, 4), (8.7, 16, 5.2), M("glow", "#ffe23a"), rot=("z", 22.5, (8, 14, 4.5)))
        elif kid == "magnet_tnt":
            m.box((3, 10.5, 6.5), (5.5, 17, 9.5), M("metal", "#d42a2a"))
            m.box((10.5, 10.5, 6.5), (13, 17, 9.5), M("metal", "#d42a2a"))
            m.box((3, 17, 6.5), (13, 19.5, 9.5), M("metal", "#d42a2a"))
            m.box((3, 10.5, 6.5), (5.5, 12.5, 9.5), M("metal", "#c4c8cc"))
            m.box((10.5, 10.5, 6.5), (13, 12.5, 9.5), M("metal", "#c4c8cc"))
        elif kid == "shockwave_tnt":
            cylinder(m, 8, 8, 10.5, 13, 5, M("metal", "#8a3a0a"), top=M("lid", "#f2e6d0"))
            cylinder(m, 8, 8, 13, 13.6, 2, M("glow", "#ff9a3a"))
    elif kid in ("firework_tnt", "flak_tnt", "meteor_tnt", "arrow_rain_tnt"):
        T_ = T
        m.box((1, 0, 1), (15, 2, 15), M("metal", "#3a3f44"), faces={"up": M("stripes", "#f2c81b")})
        m.box((4.5, 0.5, 0.9), (11.5, 1.8, 1), None, sign=sign, sign_faces=("north",))
        m.box((4.5, 0.5, 15), (11.5, 1.8, 15.1), None, sign=sign, sign_faces=("south",))
        if kid == "firework_tnt":
            # a firework battery: a lacquered box of launch tubes, each with its own colour and fuse
            m.box((2, 2, 2), (14, 6, 14), M("metal", "#8a1a2a"), faces={"up": M("lid", "#5a1018")})
            m.box((1.8, 4.5, 1.8), (14.2, 5.3, 14.2), M("band", "#c9a227"))
            m.box((5, 2.5, 1.9), (11, 4.4, 2), None, sign=sign, sign_faces=("north",))
            for x, z, r, h, c in ((8, 8, 2, 9, "#e8302a"), (4.5, 4.5, 1.3, 6, "#3b8cff"), (11.5, 4.5, 1.3, 7, "#ffd23a"),
                                  (4.5, 11.5, 1.3, 7.5, "#4be36b"), (11.5, 11.5, 1.3, 6.5, "#b04aff")):
                cylinder(m, x, z, 6, 6 + h, r, M("metal", c), top=M("lid", "#2a2a2a"))
                cylinder(m, x, z, 6 + h * 0.55, 6 + h * 0.55 + 0.8, r + 0.15, M("plain", "#f2f2f2"))
                m.box((x - 0.3, 6 + h, z - 0.3), (x + 0.3, 7.2 + h, z + 0.3), M("plain", "#c9b28a"))
                m.box((x - 0.45, 7.2 + h, z - 0.45), (x + 0.45, 7.8 + h, z + 0.45), M("glow", "#ffb12b"))
        elif kid == "flak_tnt":
            m.box((4, 2, 4), (12, 6, 12), M("metal", "#4a5a3a"))
            for x in (6, 10):
                m.box((x - 0.8, 5, 7.2), (x + 0.8, 17, 8.8), M("metal", "#6d747c"), rot=("x", -22.5, (x, 5, 8)))
        elif kid == "meteor_tnt":
            cage(m, "#3a3f44", 2, 15, 5)
            sphere(m, 8, 8.5, 8, 3.8, M("stone", "#4a2a1a"), slices=5)
            for x, y, z in ((6, 9, 5), (10, 7, 9), (8, 11, 10)):
                m.box((x - 0.6, y - 0.6, z - 0.6), (x + 0.6, y + 0.6, z + 0.6), M("glow", "#ff6a1a"))
        elif kid == "arrow_rain_tnt":
            cylinder(m, 8, 8, 2, 11, 3.5, M("plain", "#7a4a20"), top=M("plain", "#3a2410"))
            cylinder(m, 8, 8, 4, 5, 3.8, M("band", "#c9a227"))
            for x, z, h in ((6.5, 7, 17), (9, 6.5, 16), (8, 9.5, 18), (10, 9, 15.5)):
                m.box((x - 0.4, 10, z - 0.4), (x + 0.4, h, z + 0.4), M("beam", "#8a5a2b"))
                m.box((x - 1, h - 2, z - 0.2), (x + 1, h, z + 0.2), M("plain", "#f2f2f2"))
    elif kid in ("nature_tnt", "harvest_tnt", "lumber_tnt", "desert_tnt", "bee_tnt", "halloween_tnt"):
        if kid == "nature_tnt":
            pot(m, "#b8643a", "#4a2e1a")
            m.box((7, 7, 7), (9, 11, 9), M("beam", "#6b4a2b"))
            sphere(m, 8, 13, 8, 4.2, M("leaves", "#3f9a2f"), slices=5)
            signs(m, sign, 1, 5.5, 3, 4.5)
        elif kid == "desert_tnt":
            pot(m, "#c9a85a", "#e8d49a")
            m.box((6.5, 7, 6.5), (9.5, 17, 9.5), M("leaves", "#4f9a3a"))
            m.box((9.5, 10, 7), (12, 11.5, 9), M("leaves", "#4f9a3a"))
            m.box((10.5, 11.5, 7), (12, 14.5, 9), M("leaves", "#4f9a3a"))
            m.box((4, 12, 7), (6.5, 13.5, 9), M("leaves", "#4f9a3a"))
            m.box((4, 13.5, 7), (5.5, 15.5, 9), M("leaves", "#4f9a3a"))
            m.box((7.5, 17, 7.5), (8.5, 18, 8.5), M("plain", "#ff6ab0"))
            signs(m, sign, 1, 5.5, 3, 4.5)
        elif kid == "harvest_tnt":
            # a patch of farmland with rows of ripe wheat and a basket of the harvest
            m.box((1, 0, 1), (15, 3, 15), M("planks", "#5a3a22"), faces={"up": M("plain", "#4a2e1a")})
            for z in (3, 6.5, 10):
                m.box((1.5, 3, z), (14.5, 3.4, z + 1), M("plain", "#3a2414"))
                for x in (2.5, 5, 7.5, 10, 12.5):
                    h = 7 + ((int(x * 3 + z * 7)) % 3)
                    m.box((x - 0.35, 3, z + 0.15), (x + 0.35, 3 + h, z + 0.85), M("plain", "#8aa83a"))
                    m.box((x - 0.6, 3 + h - 2.2, z), (x + 0.6, 3 + h + 0.3, z + 1), M("leaves", "#e0b83a"))
            m.box((10.5, 3, 12), (14.5, 6, 15), M("planks", "#b98a4a"))
            for x, c in ((11.5, "#e8c050"), (13, "#ff8a1a"), (12.2, "#d42a2a")):
                m.box((x - 0.7, 6, 12.8), (x + 0.7, 7.2, 14.2), M("plain", c))
            m.box((4, 0.4, 0.95), (9, 2.6, 1), None, sign=sign, sign_faces=("north",))
        elif kid == "lumber_tnt":
            cylinder(m, 8, 8, 0, 9, 6, M("planks", "#6b4a2b"), top=M("lid", "#c89a5a"))
            m.box((8.5, 9, 7.5), (9.7, 17, 8.5), M("beam", "#8a5a2b"), rot=("z", -22.5, (9, 9, 8)))
            m.box((9.5, 13, 7.4), (13.5, 16.5, 8.6), M("metal", "#9aa0a8"), rot=("z", -22.5, (9, 9, 8)))
            signs(m, sign, 2, 7, 2, 4.5, sides=("north", "south"))
        elif kid == "bee_tnt":
            m.box((1.5, 0, 1.5), (14.5, 12, 14.5), M("planks", "#d9b84a"), faces={"up": M("planks", "#c9a227")})
            m.box((1, 12, 1), (15, 13.5, 15), M("beam", "#8a6a2a"))
            m.box((5, 3, 1.4), (11, 4.5, 1.5), M("plain", "#2a1a0a"))
            sphere(m, 8, 16, 8, 2.2, M("plain", "#f2c81b"), slices=3)
            m.box((5.6, 15, 6.9), (10.4, 17, 9.1), M("plain", "#1a1a1a"))
            m.box((6, 17.5, 5), (8, 18, 11), M("glass", "#e8f4ff"))
            m.box((5.5, 6, 1.4), (10.5, 11, 1.5), None, sign=sign, sign_faces=("north",))
        elif kid == "halloween_tnt":
            m.box((1.5, 0, 1.5), (14.5, 12, 14.5), M("plain", "#e07a1a"))
            for x in (4, 7.5, 11):
                m.box((x, 0, 1.3), (x + 1, 12, 14.7), M("plain", "#c45a10"))
            m.box((7.2, 12, 7.2), (8.8, 15, 8.8), M("leaves", "#4a6a2a"))
            for a, b in (((3.5, 7, 1.2), (6, 9.5, 1.5)), ((10, 7, 1.2), (12.5, 9.5, 1.5)), ((4.5, 3, 1.2), (11.5, 5, 1.5))):
                m.box(a, b, M("glow", "#ffd23a"))
    elif kid in ("horde_tnt", "wolf_tnt", "golem_tnt"):
        if kid == "horde_tnt":
            m.box((1, 0, 1), (15, 2, 15), M("leaves", "#4a3a2a"))
            m.box((3, 2, 9), (13, 13, 12), M("bricks", "#8a8a8a"), faces={"up": M("stone", "#8a8a8a")})
            m.box((4.5, 7, 8.9), (11.5, 11.5, 9), None, sign=sign, sign_faces=("north",))
            m.box((6, 2, 4.5), (8, 7, 6.5), M("plain", "#4f8a3a"))
            for x in (5.3, 6.7, 8.1):
                m.box((x, 7, 5), (x + 0.6, 8.2, 6), M("plain", "#4f8a3a"))
        elif kid == "wolf_tnt":
            # a sitting wolf on a cushion, facing north
            fur, dark, snout = M("plain", "#dadada"), M("plain", "#b4b4b4"), M("plain", "#c8c2bc")
            m.box((2, 0, 2), (14, 1.5, 14), M("plain", "#8a2a22"), faces={"up": M("leaves", "#a8382c")})
            m.box((5.5, 0.4, 1.9), (10.5, 1.2, 2), None, sign=sign, sign_faces=("north",))
            m.box((5, 1.5, 8), (11, 6.5, 13.5), fur)
            m.box((4.6, 1.5, 9), (5, 5, 13), dark)
            m.box((11, 1.5, 9), (11.4, 5, 13), dark)
            m.box((5.5, 5, 5.5), (10.5, 11, 9.5), fur)
            m.box((6, 1.5, 5.5), (7.5, 5, 7), fur)
            m.box((8.5, 1.5, 5.5), (10, 5, 7), fur)
            m.box((5.3, 9.5, 5.3), (10.7, 10.5, 9.7), M("plain", "#c8322a"))
            m.box((5, 10, 3.5), (11, 15, 8.5), fur)
            m.box((6.5, 10.5, 1.5), (9.5, 13, 3.5), snout)
            m.box((7.2, 12, 1.4), (8.8, 13, 1.5), M("plain", "#1a1a1a"))
            for x in (5.4, 9.2):
                m.box((x, 15, 5), (x + 1.4, 17, 6.4), dark)
            for x in (6, 8.8):
                m.box((x, 13.2, 3.4), (x + 1.2, 14.2, 3.5), M("plain", "#1a1a1a"))
            m.box((7.3, 3, 13.5), (8.7, 4.4, 17.5), fur, rot=("x", 22.5, (8, 3.7, 13.5)))
        elif kid == "golem_tnt":
            # an iron golem bust: shoulders, hanging arms, the long nose and the brow, vines
            iron, shade = M("stone", "#d6cfc4"), M("stone", "#b8b0a4")
            m.box((1, 0, 4), (15, 1, 12), M("bricks", "#7a7a7a"))
            m.box((3.5, 1, 5), (12.5, 9.5, 11), iron)
            m.box((1, 1, 6), (3.5, 10, 10), shade)
            m.box((12.5, 1, 6), (15, 10, 10), shade)
            m.box((5, 9.5, 5.5), (11, 15.5, 10.5), iron)
            m.box((7.2, 10.2, 3.8), (8.8, 13.5, 5.5), shade)
            m.box((5, 13.4, 5.3), (11, 14.4, 5.5), M("plain", "#8a8276"))
            for x in (5.7, 9.1):
                m.box((x, 12.2, 5.4), (x + 1.2, 13.2, 5.5), M("glow", "#d42a2a"))
            for a, b in (((4, 2, 4.9), (5, 8.5, 5)), ((10.5, 4, 4.9), (11.5, 9.5, 5)), ((5, 15.5, 6), (6, 15.7, 9)), ((1, 4, 5.9), (2, 9, 6))):
                m.box(a, b, M("leaves", "#4f8a3a"))
            m.box((13, 8, 5.4), (14.6, 9.5, 6), M("plain", "#d42a2a"))
            m.box((6, 2, 11), (10, 6.5, 11.1), None, sign=sign, sign_faces=("south",))
    elif kid in ("healing_tnt", "blessing_tnt", "curse_tnt", "rainbow_tnt", "bounce_tnt", "cobweb_tnt", "volcano_tnt"):
        if kid == "healing_tnt":
            m.box((1.5, 0, 3), (14.5, 9, 13), M("plain", "#f2f2f2"), faces={"up": M("plain", "#e8e8e8")})
            m.box((6.5, 2, 2.9), (9.5, 8, 3), M("plain", "#d81b60"))
            m.box((5, 3.5, 2.9), (11, 6.5, 3), M("plain", "#d81b60"))
            m.box((6, 9, 7), (10, 10.5, 9), M("band", "#8a8a8a"))
            cylinder(m, 8, 8, 10.5, 14, 2.5, M("glass", "#ff9ec2"))
            cylinder(m, 8, 8, 14, 15.5, 1, M("glass", "#ffd6e4"))
            m.box((6, 2, 13), (10, 7, 13.1), None, sign=sign, sign_faces=("south",))
            m.box((6.5, 9, 4.5), (9.5, 9.2, 6.5), M("plain", "#d81b60"))
            m.box((7, 9, 4), (9, 9.2, 7), M("plain", "#d81b60"))
            m.box((5, 9, 11), (6, 11.5, 12), M("band", "#5a5f66"))
            m.box((10, 9, 11), (11, 11.5, 12), M("band", "#5a5f66"))
            m.box((5, 11.5, 11), (11, 12.5, 12), M("band", "#5a5f66"))
        elif kid == "blessing_tnt":
            pedestal(m, "#e8dcc0", "#c9a227")
            m.box((5, 1, 2.9), (11, 4.5, 3), None, sign=sign, sign_faces=("north",))
            m.box((7.2, 5, 7.2), (8.8, 13, 8.8), M("glow", "#ffd23a"))
            m.box((5, 9, 7.2), (11, 10.6, 8.8), M("glow", "#ffd23a"))
            for y in (15,):
                m.box((5, y, 5), (11, y + 0.8, 5.8), M("glow", "#ffe98a"))
                m.box((5, y, 10.2), (11, y + 0.8, 11), M("glow", "#ffe98a"))
                m.box((5, y, 5), (5.8, y + 0.8, 11), M("glow", "#ffe98a"))
                m.box((10.2, y, 5), (11, y + 0.8, 11), M("glow", "#ffe98a"))
        elif kid == "curse_tnt":
            pedestal(m, "#2a1a30", "#b04aff")
            m.box((5, 1, 2.9), (11, 4.5, 3), None, sign=sign, sign_faces=("north",))
            m.box((4.5, 5, 4.5), (11.5, 12, 11.5), M("stone", "#6b2fb8"))
            m.box((5.5, 8, 4.4), (7.5, 10, 4.5), M("glow", "#e0b0ff"))
            m.box((8.5, 8, 4.4), (10.5, 10, 4.5), M("glow", "#e0b0ff"))
            m.box((6.5, 6, 4.4), (9.5, 7, 4.5), M("plain", "#1a0a20"))
        elif kid == "rainbow_tnt":
            m.box((1, 0, 1), (15, 2, 15), M("plain", "#f2f2f2"))
            m.box((4.5, 0.4, 0.9), (11.5, 1.6, 1), None, sign=sign, sign_faces=("north",))
            for i, c in enumerate(("#e8302a", "#ff8a1a", "#ffe23a", "#4be36b", "#3b8cff", "#8a4bff")):
                m.box((1.5 + i, 2, 7), (2.5 + i, 13 - i * 1.6, 9), M("plain", c))
                m.box((13.5 - i, 2, 7), (14.5 - i, 13 - i * 1.6, 9), M("plain", c))
                m.box((1.5 + i, 12 - i * 1.6, 7), (14.5 - i, 13 - i * 1.6, 9), M("plain", c))
        elif kid == "bounce_tnt":
            m.box((2, 0, 2), (14, 2, 14), M("metal", "#3a3f44"))
            for i in range(4):
                m.box((4, 2 + i * 2, 4), (12, 3 + i * 2, 12), M("band", "#8a8f96"), rot=("y", 22.5 if i % 2 else -22.5, (8, 2, 8)))
            m.box((2.5, 10, 2.5), (13.5, 15, 13.5), M("glass", "#7fc85a"))
            m.box((5, 11, 2.4), (11, 14.5, 2.5), None, sign=sign, sign_faces=("north",))
        elif kid == "cobweb_tnt":
            cage(m, "#4a4a52", 0, 14, 6.5)
            m.box((2, 0, 2), (14, 1, 14), M("plain", "#4a4a52"))
            m.box((7.7, 1, 2), (8.3, 13, 2.1), M("plain", "#f0f0f0"))
            m.box((2, 6.7, 2), (14, 7.3, 2.1), M("plain", "#f0f0f0"))
            m.box((3, 6.7, 1.9), (13, 7.3, 2), M("plain", "#f0f0f0"), rot=("z", 45, (8, 7, 2)))
            m.box((3, 6.7, 1.9), (13, 7.3, 2), M("plain", "#f0f0f0"), rot=("z", -45, (8, 7, 2)))
            sphere(m, 8, 7, 8, 3.5, M("plain", "#e8e8e8"), slices=4)
            m.box((5.5, 1.5, 13.9), (10.5, 5.5, 14), None, sign=sign, sign_faces=("south",))
        elif kid == "volcano_tnt":
            for i, r in enumerate((7, 6, 5, 4, 3.2)):
                cylinder(m, 8, 8, i * 2.5, (i + 1) * 2.5, r, M("stone", "#3a3330"))
            cylinder(m, 8, 8, 12.5, 12.8, 2.6, M("glow", "#ff6a1a"))
            m.box((9, 4, 1.8), (10.2, 12, 2.4), M("glow", "#ff7a1a"), rot=("x", 22.5, (9.5, 12, 2)))
            m.box((5.5, 2, 1.0), (10.5, 6.5, 1.1), None, sign=sign, sign_faces=("north",))
    elif kid == "mini_nuke":
        m.box((3, 0, 3), (13, 1.5, 13), M("metal", "#3a3f44"), faces={"up": M("stripes", "#f2c81b")})
        for x in (5, 11):
            m.box((x - 1, 1.5, 6), (x + 1, 4.5, 10), M("metal", "#3a3f44"))
        bomb_shell(m, 7.5, 2.6, 9, "#5a6a2a", "#f2c81b", "#3f4a1e", sign)
    elif kid == "nuke":
        m.box((1, 0, 1), (15, 1.5, 15), M("metal", "#3a3f44"), faces={"up": M("stripes", "#f2c81b")})
        cylinder(m, 8, 8, 1.5, 13.5, 4.6, M("metal", "#4a5320"), top=M("lid", "#4a5320"))
        cylinder(m, 8, 8, 4.5, 6.5, 4.9, M("stripes", "#f2c81b"))
        cylinder(m, 8, 8, 13.5, 16, 3.3, M("metal", "#4a5320"))
        cylinder(m, 8, 8, 16, 17.5, 2, M("metal", "#3a3f44"))
        cylinder(m, 8, 8, 17.5, 18.3, 0.9, M("glow", "#ff3a2a"))
        for a, b in (((1.5, 1.5, 7.6), (14.5, 6, 8.4)), ((7.6, 1.5, 1.5), (8.4, 6, 14.5))):
            m.box(a, b, M("metal", "#3a3f44"))
        signs(m, sign, 7.5, 12.5, 8 - 4.6, 4.5)
    elif kid == "tsar_bomba":
        m.box((0.5, 0, 3), (15.5, 2, 13), M("metal", "#3a3f44"), faces={"up": M("band", "#5a5f66")})
        for x in (3.5, 12.5):
            m.box((x - 1, 2, 3.5), (x + 1, 6, 12.5), M("metal", "#2a2a2e"))
        bomb_shell(m, 9.5, 3.8, 10.5, "#2a2a2e", "#c9a227", "#c9a227", sign)
    else:
        raise KeyError(kid)
    return m


def tube_x(m, cy, cz, x1, x2, r, side, end=None):
    """A round body lying along x, stepped like `cylinder`."""
    end = end or side
    w = r * 0.72
    f = {"west": end, "east": end}
    m.box((x1, cy - w, cz - r), (x2, cy + w, cz + r), side, faces=f)
    m.box((x1, cy + w, cz - w), (x2, cy + r, cz + w), side, faces=f)
    m.box((x1, cy - r, cz - w), (x2, cy - w, cz + w), side, faces=f)


def bomb_shell(m, cy, r, length, body, band, fins, sign):
    """An aerial bomb lying along x: a long body, a rounded nose to +x, a ring and fins at the tail."""
    T = m.tex
    x1, x2 = 8 - length / 2 - 1, 8 + length / 2 - 1
    tube_x(m, cy, 8, x1, x2, r, T.mat("metal", body), T.mat("lid", body))
    tube_x(m, cy, 8, x2, x2 + r * 0.55, r * 0.75, T.mat("metal", body))
    tube_x(m, cy, 8, x2 + r * 0.55, min(16, x2 + r * 0.95), r * 0.42, T.mat("metal", body))
    tube_x(m, cy, 8, x1 + length * 0.58, x1 + length * 0.66, r + 0.2, T.mat("band", band))
    m.box((x1 - 1, cy - r - 1.6, 7.6), (x1 + 2.5, cy + r + 1.6, 8.4), T.mat("metal", fins))
    m.box((x1 - 1, cy - 0.4, 8 - r - 1.6), (x1 + 2.5, cy + 0.4, 8 + r + 1.6), T.mat("metal", fins))
    half = r * 0.7
    mid = (x1 + x2) / 2
    m.box((mid - half, cy - half, 8 - r - 0.1), (mid + half, cy + half, 8 - r), None, sign=sign, sign_faces=("north",))
    m.box((mid - half, cy - half, 8 + r), (mid + half, cy + half, 8 + r + 0.1), None, sign=sign, sign_faces=("south",))


def build(write_png, write_json, root, ns, kinds, icons):
    """kinds: dicts with id, body, label, icon, pal (the arsenal's kinds and the bombs)."""
    T = Textures(ns)
    for k in kinds:
        kid = k["id"]
        rows = icons[k.get("sign_icon", k["icon"])]
        assert len(rows) <= 12 and len(rows[0]) <= 12, kid
        sign = T.sign(kid, rows, k["pal"], k["label"])
        m = make(kid, k, T, sign)
        write_json(os.path.join(root, "assets", ns, "models", "block", kid + ".json"), m.json(kid))
    T.write(write_png, root)
    return T
