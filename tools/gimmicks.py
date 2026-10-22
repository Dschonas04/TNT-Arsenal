"""The extras around the TNT: the TNT sling, the handbook, the advancements of the "TNT Arsenal" tab,
the statistics and the texts of the linking detonator and the floating timer. Runs after nuclear_assets,
because it adds to (and partly overrides) the translations written before it."""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))

SLING_PAL = {"w": "#8a5a2b", "l": "#b07a3e", "d": "#5a3a1a", "r": "#3a3a3a", "p": "#7a4a2a", "t": "#d42a1a", "s": "#f1e6c8"}
SLING = [
    "................",
    "..w.........w...",
    "..wl.......wl...",
    "..r.wl...wl.r...",
    "..r..wl.wl..r...",
    "...r..wwl..r....",
    "....r.wl..r.....",
    ".....pwlpp......",
    ".....ptst.......",
    "......wl........",
    "......wl........",
    "......dl........",
    "......wl........",
    "......dl........",
    "......dd........",
    "................",
]

ADVANCEMENTS = [
    # name, parent, icon, frame, English title, English line, German title, German line
    ("root", None, "tnt_arsenal:mega_tnt", "task", "TNT Arsenal", "Sixty-four ways to make a bang",
     "TNT-Arsenal", "Vierundsechzig Arten, es krachen zu lassen"),
    ("first_bang", "root", "tnt_arsenal:fire_tnt", "task", "Boom!", "Set off your first arsenal TNT",
     "Rumms!", "Zünde dein erstes Arsenal-TNT"),
    ("ten_kinds", "first_bang", "tnt_arsenal:cluster_tnt", "goal", "Demolition Expert", "Set off ten different kinds of TNT",
     "Sprengmeister", "Zünde zehn verschiedene TNT-Arten"),
    ("all_kinds", "ten_kinds", "tnt_arsenal:giga_tnt", "challenge", "The Whole Arsenal", "Set off every single kind of TNT",
     "Das volle Arsenal", "Zünde jede einzelne TNT-Art"),
    ("hundred", "first_bang", "tnt_arsenal:chain_tnt", "challenge", "Pyromaniac", "Set off a hundred arsenal TNT",
     "Pyromane", "Zünde hundert Arsenal-TNT"),
    ("salvo", "first_bang", "tnt_arsenal:detonator", "goal", "At the Push of a Button", "Fire eight or more bombs at once with one detonator",
     "Auf Knopfdruck", "Zünde acht oder mehr Bomben auf einmal mit einem Fernzünder"),
    ("sticky", "first_bang", "tnt_arsenal:tnt_sling", "task", "Sticks Like Glue", "Stick a TNT onto a creature with the TNT sling",
     "Klebt!", "Kleb mit der TNT-Schleuder ein TNT an ein Lebewesen"),
    ("party", "first_bang", "tnt_arsenal:confetti_tnt", "task", "Party Animal", "Set off a party TNT",
     "Partylöwe", "Zünde ein Party-TNT"),
]

EN = {
    "item.tnt_arsenal.tnt_sling": "TNT Sling",
    "item.tnt_arsenal.tnt_sling.desc": "Throws lit TNT from your other hand or inventory; sneak to throw it sticky",
    "item.tnt_arsenal.tnt_sling.empty": "No TNT to throw",
    "item.tnt_arsenal.detonator.linked": "%s bomb(s) linked — newest at %s, %s, %s",
    "item.tnt_arsenal.detonator.unlinked": "Unlinked — %s bomb(s) left",
    "item.tnt_arsenal.detonator.full": "Full — a detonator holds %s bombs",
    "item.tnt_arsenal.detonator.cleared": "All bombs unlinked",
    "item.tnt_arsenal.detonator.unbound": "Nothing linked — right-click bombs first",
    "item.tnt_arsenal.detonator.fired": "Firing %s bomb(s)",
    "item.tnt_arsenal.detonator.desc": "Right-click up to 16 bombs to link them, right-click the air to set them all off; sneak to unlink all",
    "item.tnt_arsenal.timer.desc": "Sneak + right-click: set the time; right-click a bomb: start the countdown, shown above it",
    "stat.tnt_arsenal.tnt_detonated": "Arsenal TNT set off",
    "stat.tnt_arsenal.tnt_thrown": "TNT thrown with the sling",
    "book.tnt_arsenal.intro": "The TNT Arsenal\n\nSixty-four kinds of TNT, the atomic bombs and their tools.\n\nEvery kind is crafted shapeless at a crafting table. One page per kind follows.",
    "book.tnt_arsenal.tools": "Tools\n\nDetonator: link up to 16 bombs, fire them all at once.\n\nTimer: a countdown that floats above the bomb.\n\nTNT sling: throws lit TNT, sneak for sticky TNT.",
    "book.tnt_arsenal.recipe": "Recipe:",
}
DE = {
    "item.tnt_arsenal.tnt_sling": "TNT-Schleuder",
    "item.tnt_arsenal.tnt_sling.desc": "Wirft brennendes TNT aus der anderen Hand oder dem Inventar; schleichend wird es zum Klebe-TNT",
    "item.tnt_arsenal.tnt_sling.empty": "Kein TNT zum Werfen",
    "item.tnt_arsenal.detonator.linked": "%s Bombe(n) verbunden – zuletzt bei %s, %s, %s",
    "item.tnt_arsenal.detonator.unlinked": "Getrennt – noch %s Bombe(n)",
    "item.tnt_arsenal.detonator.full": "Voll – ein Fernzünder fasst %s Bomben",
    "item.tnt_arsenal.detonator.cleared": "Alle Bomben getrennt",
    "item.tnt_arsenal.detonator.unbound": "Nichts verbunden – erst Bomben rechtsklicken",
    "item.tnt_arsenal.detonator.fired": "%s Bombe(n) werden gezündet",
    "item.tnt_arsenal.detonator.desc": "Bis zu 16 Bomben rechtsklicken zum Verbinden, in die Luft rechtsklicken zündet alle; schleichend alle trennen",
    "item.tnt_arsenal.timer.desc": "Schleichen + Rechtsklick: Zeit einstellen; Rechtsklick auf eine Bombe: Countdown starten, schwebt über ihr",
    "stat.tnt_arsenal.tnt_detonated": "Arsenal-TNT gezündet",
    "stat.tnt_arsenal.tnt_thrown": "TNT mit der Schleuder geworfen",
    "book.tnt_arsenal.intro": "Das TNT-Arsenal\n\nVierundsechzig TNT-Arten, die Atombomben und ihr Werkzeug.\n\nJede Art wird formlos an der Werkbank hergestellt. Auf den nächsten Seiten steht je eine Art.",
    "book.tnt_arsenal.tools": "Werkzeug\n\nFernzünder: bis zu 16 Bomben verbinden und alle auf einmal zünden.\n\nZeitzünder: ein Countdown schwebt über der Bombe.\n\nTNT-Schleuder: wirft brennendes TNT, schleichend als Klebe-TNT.",
    "book.tnt_arsenal.recipe": "Rezept:",
}


def page(kind, ingredient_keys):
    """One handbook page: the name, what it does and what goes into it — all translated on the reader's side."""
    parts = [{"translate": f"block.tnt_arsenal.{kind['id']}", "bold": True, "color": "dark_red"}, "\n\n",
             {"translate": f"block.tnt_arsenal.{kind['id']}.desc"}, "\n\n",
             {"translate": "book.tnt_arsenal.recipe", "italic": True}]
    counts = {}
    for item in kind["recipe"]:
        counts[item] = counts.get(item, 0) + 1
    for item, n in counts.items():
        key = ingredient_keys.get(item) or f"block.{item.replace(':', '.')}"
        parts += ["\n• " + (f"{n} × " if n > 1 else ""), {"translate": key}]
    return {"text": "", "extra": parts}


def main(root, ns, kinds, write_json, write_png, hexc):
    # the sling: item texture, model, recipe
    px = []
    for row in SLING:
        for ch in row:
            px.append(hexc(SLING_PAL[ch]) + (255,) if ch != "." else (0, 0, 0, 0))
    write_png(f"{root}/assets/{ns}/textures/item/tnt_sling.png", 16, 16, px)
    write_json(f"{root}/assets/{ns}/models/item/tnt_sling.json",
               {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{ns}:item/tnt_sling"}})
    write_json(f"{root}/assets/{ns}/items/tnt_sling.json", {"model": {"type": "minecraft:model", "model": f"{ns}:item/tnt_sling"}})
    write_json(f"{root}/data/{ns}/recipe/tnt_sling.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["S S", "LSL", " S "],
        "key": {"S": "minecraft:stick", "L": "minecraft:string"},
        "result": {"id": f"{ns}:tnt_sling", "count": 1}})

    # the handbook: a written book, crafted from a book and a TNT
    with open(os.path.join(HERE, "ingredient_keys.json"), encoding="utf-8") as f:
        ingredient_keys = json.load(f)
    pages = [{"translate": "book.tnt_arsenal.intro"}, {"translate": "book.tnt_arsenal.tools"}]
    pages += [page(k, ingredient_keys) for k in kinds]
    assert len(pages) <= 100, len(pages)
    write_json(f"{root}/data/{ns}/recipe/handbook.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:book", "minecraft:tnt"],
        "result": {"id": "minecraft:written_book", "count": 1, "components": {
            "minecraft:written_book_content": {"title": "TNT Arsenal", "author": "Dschonas04", "pages": pages, "resolved": True}}}})

    # the advancements: all but the root are awarded by the mod itself (criterion "done")
    en, de = dict(EN), dict(DE)
    for name, parent, icon, frame, en_t, en_d, de_t, de_d in ADVANCEMENTS:
        key = f"advancements.{ns}.arsenal.{name}"
        en[key + ".title"], en[key + ".description"] = en_t, en_d
        de[key + ".title"], de[key + ".description"] = de_t, de_d
        adv = {
            "display": {"icon": {"id": icon}, "title": {"translate": key + ".title"},
                        "description": {"translate": key + ".description"}, "frame": frame,
                        "show_toast": parent is not None, "announce_to_chat": parent is not None},
            "criteria": {"done": {"trigger": "minecraft:impossible"}},
        }
        if parent is None:
            adv["display"]["background"] = "minecraft:gui/advancements/backgrounds/stone"
            adv["criteria"] = {"done": {"trigger": "minecraft:inventory_changed",
                                        "conditions": {"items": [{"items": "minecraft:tnt"}]}}}
        else:
            adv["parent"] = f"{ns}:arsenal/{parent}"
        write_json(f"{root}/data/{ns}/advancement/arsenal/{name}.json", adv)

    for code, extra in (("en_us", en), ("de_de", de)):
        path = f"{root}/assets/{ns}/lang/{code}.json"
        with open(path, encoding="utf-8") as f:
            merged = json.load(f)
        merged.update(extra)
        write_json(path, merged)
    print("ok gimmicks,", len(pages), "handbook pages")
