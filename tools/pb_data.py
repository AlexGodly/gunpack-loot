"""Build Point Blank: Loot data: the original tiered loot plus Default/Equal modes and commands.

Mode lives in the scoreboard objective `pb_loot_mode` on the fake player `#mode`:
0 = Default (tiered), 1 = Equal (data pack), 2 = handled by the mod.
"""
import copy, json, os, shutil, sys

OBJ = "pb_loot_mode"


def mode_is(lo, hi):
    return {"condition": "minecraft:value_check",
            "value": {"type": "minecraft:score", "target": {"type": "minecraft:fixed", "name": "#mode"}, "score": OBJ},
            "range": {"min": lo, "max": hi}}


def ref(entry):
    return entry.get("name") or entry.get("value") or ""


def process_root(root):
    """root is a folder containing data/ (base) or an overlay folder."""
    lt = "loot_tables" if os.path.isdir(os.path.join(root, "data/alexs_pb_loot/loot_tables")) else "loot_table"
    key = "name" if lt == "loot_tables" else "value"
    inj_dir = os.path.join(root, "data/alexs_pb_loot", lt, "inject")
    counts = {}
    for dp, _, fs in os.walk(inj_dir):
        for f in fs:
            for pool in json.load(open(os.path.join(dp, f)))["pools"]:
                for e in pool["entries"]:
                    counts.setdefault(ref(e), [fn for fn in e.get("functions", []) if "count" in fn.get("function", "")])
    items = sorted(os.listdir(os.path.join(root, "data/alexs_pb_loot", lt, "item")))
    equal = []
    for f in items:
        name = "alexs_pb_loot:item/" + f[:-5]
        e = {"type": "minecraft:loot_table", key: name, "weight": 1}
        if counts.get(name):
            e["functions"] = counts[name]
        equal.append(e)
    json.dump({"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2},
                                                      "bonus_rolls": 0, "entries": equal}]},
              open(os.path.join(inj_dir, "equal.json"), "w"), indent=2)
    for ns in ("minecraft", "pointblank"):
        base = os.path.join(root, "data", ns, lt, "chests")
        for dp, _, fs in os.walk(base):
            for f in fs:
                p = os.path.join(dp, f)
                t = json.load(open(p))
                for pool in t["pools"]:
                    if any(ref(e).startswith("alexs_pb_loot:inject/") for e in pool["entries"]):
                        pool["conditions"] = [mode_is(0, 0)] + pool.get("conditions", [])
                t["pools"].append({"rolls": 1, "bonus_rolls": 0,
                                   "conditions": [mode_is(1, 1), {"condition": "minecraft:random_chance", "chance": 0.35}],
                                   "entries": [{"type": "minecraft:loot_table", key: "alexs_pb_loot:inject/equal"}]})
                json.dump(t, open(p, "w"), indent=2)
    return [os.path.splitext(f)[0] for f in items]


def functions(root):
    say = lambda t: f'tellraw @s {{"text":"{t}","color":"gold"}}'
    gray = lambda t: f'tellraw @s {{"text":"{t}","color":"gray"}}'
    files = {
        "load.mcfunction": f"scoreboard objectives add {OBJ} dummy\n",
        "mode/default.mcfunction": f"scoreboard players set #mode {OBJ} 0\n" + say("Point Blank: Loot mode set to Default (tiered loot)") + "\n",
        "mode/equal.mcfunction": f"scoreboard players set #mode {OBJ} 1\n" + say("Point Blank: Loot mode set to Equal (same chance for every gun and item)") + "\n",
        "status.mcfunction": (f"execute unless score #mode {OBJ} matches 1.. run " + say("Point Blank: Loot mode: Default (tiered loot)") + "\n"
                              f"execute if score #mode {OBJ} matches 1 run " + say("Point Blank: Loot mode: Equal (same chance for every gun and item)") + "\n"
                              f"execute if score #mode {OBJ} matches 2 run " + say("Point Blank: Loot mode is set by the mod: use /pbloot") + "\n"),
        "help.mcfunction": "\n".join([say("Point Blank: Loot data pack commands:"),
                                      gray("/function point_blank_loot:mode/default  - tiered loot (better guns in harder structures)"),
                                      gray("/function point_blank_loot:mode/equal  - every gun, ammo and attachment has the same chance"),
                                      gray("/function point_blank_loot:status  - show the current mode"),
                                      gray("Gun pack guns need the mod version of Point Blank: Loot.")]) + "\n",
    }
    for folder in ("functions", "function"):
        for name, text in files.items():
            p = os.path.join(root, "data/point_blank_loot", folder, name)
            os.makedirs(os.path.dirname(p), exist_ok=True)
            open(p, "w").write(text)
        p = os.path.join(root, "data/minecraft/tags", folder, "load.json")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump({"values": ["point_blank_loot:load"]}, open(p, "w"), indent=2)


def build(src, out):
    if os.path.exists(out):
        shutil.rmtree(out)
    shutil.copytree(src, out)
    defaults = None
    for r in [out] + [os.path.join(out, d) for d in sorted(os.listdir(out)) if d.startswith("mc")]:
        names = process_root(r)
        defaults = defaults or names
    functions(out)
    return defaults


def default_ids(out):
    ids = set()
    d = os.path.join(out, "data/alexs_pb_loot/loot_tables/item")
    for f in os.listdir(d):
        for pool in json.load(open(os.path.join(d, f)))["pools"]:
            for e in pool["entries"]:
                if e.get("type") == "minecraft:item":
                    ids.add(e["name"])
    return sorted(ids)


if __name__ == "__main__":
    out = sys.argv[2]
    build(sys.argv[1], out)
    if len(sys.argv) > 3:
        shutil.copy(sys.argv[3], os.path.join(out, "pack.png"))
    open(os.path.join(out, "point_blank_loot_defaults.txt"), "w").write("\n".join(default_ids(out)) + "\n")
