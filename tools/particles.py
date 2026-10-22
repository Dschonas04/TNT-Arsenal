"""The sprites of the mod's own particles (white or grey, the client tints them) and their particle definitions."""
import math
import random


def blank(n):
    return [[(0, 0, 0, 0)] * n for _ in range(n)]


def soft_dot(n, radius, core=1.0, falloff=1.6, grey=255):
    img = blank(n)
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c) / radius
            if d <= 1:
                a = 1.0 if d < core * 0.5 else max(0.0, 1 - (d - core * 0.5) / (1 - core * 0.5)) ** falloff
                img[y][x] = (grey, grey, grey, int(255 * a))
    return img


def star(n, arm, thick=0):
    """A four-pointed star with a bright centre: arms of length `arm` pixels."""
    img = blank(n)
    c = n // 2
    for i in range(-arm, arm + 1):
        a = int(255 * (1 - abs(i) / (arm + 1)) ** 0.7)
        for t in range(-thick, thick + 1):
            for x, y in ((c + i, c + t), (c + t, c + i)):
                if 0 <= x < n and 0 <= y < n:
                    old = img[y][x][3]
                    img[y][x] = (255, 255, 255, max(old, a if t == 0 else a // 2))
    img[c][c] = (255, 255, 255, 255)
    return img


def ring(n):
    """The shock wave: a bright thin rim, a soft glow inside it, transparent in the middle."""
    img = blank(n)
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c) / (n / 2)
            if 0.55 < d <= 1.0:
                if d > 0.88:
                    a = 1 - (d - 0.88) / 0.12 * 0.6
                else:
                    a = ((d - 0.55) / 0.33) ** 2 * 0.6
                # a little grain so the ring does not look like a vector drawing
                a *= 0.85 + 0.15 * ((x * 7 + y * 13) % 5) / 4
                img[y][x] = (255, 255, 255, int(255 * max(0, min(1, a))))
    return img


def puff(n, frame, frames, rnd):
    """A cloud of smoke made of overlapping soft blobs; later frames are looser and thinner."""
    img = blank(n)
    spread = 0.18 + 0.12 * frame / frames
    blobs = [(n / 2 + rnd.uniform(-1, 1) * n * spread, n / 2 + rnd.uniform(-1, 1) * n * spread, n * rnd.uniform(0.18, 0.3))
             for _ in range(7)]
    thin = 1 - 0.45 * frame / frames
    for y in range(n):
        for x in range(n):
            a = 0.0
            for bx, by, r in blobs:
                d = math.hypot(x - bx, y - by) / r
                if d < 1:
                    a = max(a, (1 - d) ** 0.8)
            if a > 0:
                shade = int(255 * (0.75 + 0.25 * (1 - y / n)))
                img[y][x] = (shade, shade, shade, int(255 * min(1, a * 1.3) * thin))
    return img


def zigzag(n, rnd):
    """An electric arc: a jagged line across the sprite with a short fork."""
    img = blank(n)
    x, y = 0, n // 2 + rnd.randint(-2, 2)
    points = []
    while x < n:
        points.append((x, y))
        x += 1
        if rnd.random() < 0.45:
            y = max(1, min(n - 2, y + rnd.choice((-1, 1))))
    for px, py in points:
        img[py][px] = (255, 255, 255, 255)
        for dy in (-1, 1):
            if 0 <= py + dy < n and img[py + dy][px][3] == 0:
                img[py + dy][px] = (255, 255, 255, 90)
    fx, fy = points[n // 2]
    for i in range(n // 3):
        fx += 1
        fy += 1 if rnd.random() < 0.6 else 0
        if 0 <= fx < n and 0 <= fy < n:
            img[fy][fx] = (255, 255, 255, 200)
    return img


def confetti(n, variant):
    img = blank(n)
    w, h = ((6, 3), (5, 4), (7, 2))[variant]
    x0, y0 = (n - w) // 2, (n - h) // 2
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            edge = y == y0 + h - 1
            v = 210 if edge else 255
            img[y][x] = (v, v, v, 255)
    return img


def flat(img):
    return [p for row in img for p in row]


def main(root, ns, write_png, write_json):
    rnd = random.Random(26)
    sprites = {}
    sprites["fuse_spark"] = [star(8, a, 0) for a in (3, 3, 2, 1)]
    sprites["shockwave"] = [ring(64)]
    sprites["smoke_ball"] = [puff(16, f, 6, random.Random(100 + f)) for f in range(6)]
    sprites["ember"] = [soft_dot(8, 3.2, core=0.7)]
    sprites["confetti"] = [confetti(8, v) for v in range(3)]
    sprites["frost_sparkle"] = [star(9, a, 0) for a in (1, 2, 3, 4)]
    sprites["void_swirl"] = [soft_dot(8, 3.5, core=0.4, falloff=1.2)]
    sprites["radiation"] = [soft_dot(8, 3.6, core=0.5, falloff=2.0)]
    sprites["arc"] = [zigzag(16, rnd) for _ in range(4)]
    for name, frames in sprites.items():
        ids = []
        for i, img in enumerate(frames):
            tex = name if len(frames) == 1 else f"{name}_{i}"
            n = len(img)
            write_png(f"{root}/assets/{ns}/textures/particle/{tex}.png", n, n, flat(img))
            ids.append(f"{ns}:{tex}")
        write_json(f"{root}/assets/{ns}/particles/{name}.json", {"textures": ids})
    print("ok particles,", len(sprites), "kinds")
