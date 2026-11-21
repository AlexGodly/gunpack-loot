"""Build TACZ: Loot data (loot injectors + mode functions) for one Minecraft version.

Modes are kept in the scoreboard objective `tacz_loot_mode` on the fake player `#mode`:
0 = Default (tiered loot), 1 = Equal (data pack: every default item, same chance everywhere),
2 = handled by the mod (data pack pools off, the mod adds everything itself).
"""
import copy, json, os, sys, zipfile

CHESTS = ["abandoned_mineshaft", "ancient_city", "ancient_city_ice_box", "bastion_bridge", "bastion_hoglin_stable",
          "bastion_other", "bastion_treasure", "buried_treasure", "desert_pyramid", "end_city_treasure",
          "igloo_chest", "jungle_temple", "nether_bridge", "pillager_outpost", "ruined_portal", "shipwreck_map",
          "shipwreck_supply", "shipwreck_treasure", "simple_dungeon", "stronghold_corridor", "stronghold_crossing",
          "stronghold_library", "underwater_ruin_big", "underwater_ruin_small", "woodland_mansion",
          "village/village_weaponsmith", "village/village_armorer", "village/village_toolsmith",
          "village/village_plains_house", "village/village_desert_house", "village/village_savanna_house",
          "village/village_snowy_house", "village/village_taiga_house", "trial_chambers/reward",
          "trial_chambers/supply", "trial_chambers/corridor"]


def mode_is(lo, hi):
    return {"condition": "minecraft:value_check",
            "value": {"type": "minecraft:score", "target": {"type": "minecraft:fixed", "name": "#mode"},
                      "score": "tacz_loot_mode"},
            "range": {"min": lo, "max": hi}}


def build(src_dir, new_format):
    files = {}
    items = {}
    for name in sorted(os.listdir(src_dir)):
        inj = json.load(open(os.path.join(src_dir, name)))
        for pool in inj["pools"]:
            pool["conditions"] = [mode_is(0, 0)] + pool.get("conditions", [])
            for e in pool["entries"]:
                key = json.dumps(e["functions"][0], sort_keys=True)
                items.setdefault(key, e)
        files[f"data/tacz_loot/tacz_loot_injectors/{name}"] = inj
    equal_entries = []
    for e in items.values():
        e = copy.deepcopy(e)
        e["weight"] = 1
        equal_entries.append(e)
    files["data/tacz_loot/tacz_loot_injectors/equal.json"] = {
        "loot_tables": [f"minecraft:chests/{c}" for c in CHESTS],
        "pools": [{"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2},
                   "conditions": [mode_is(1, 1), {"condition": "minecraft:random_chance", "chance": 0.3}],
                   "entries": equal_entries}]}
    fn = "function" if new_format else "functions"
    say = lambda t: f'tellraw @s {{"text":"{t}","color":"gold"}}'
    files[f"data/tacz_loot/{fn}/load.mcfunction"] = "scoreboard objectives add tacz_loot_mode dummy\n"
    files[f"data/tacz_loot/{fn}/mode/default.mcfunction"] = (
        "scoreboard players set #mode tacz_loot_mode 0\n" + say("TACZ: Loot mode set to Default (tiered loot)") + "\n")
    files[f"data/tacz_loot/{fn}/mode/equal.mcfunction"] = (
        "scoreboard players set #mode tacz_loot_mode 1\n" + say("TACZ: Loot mode set to Equal (same chance in every chest)") + "\n")
    files[f"data/minecraft/tags/{fn}/load.json"] = {"values": ["tacz_loot:load"]}
    return files


def write_zip(files, out, pack_meta, icon):
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta", json.dumps(pack_meta, indent=2))
        if icon:
            z.writestr("pack.png", icon)
        for path, data in files.items():
            z.writestr(path, data if isinstance(data, str) else json.dumps(data, indent=2))


def write_dir(files, root):
    for path, data in files.items():
        p = os.path.join(root, path)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, "w").write(data if isinstance(data, str) else json.dumps(data, indent=2))


if __name__ == "__main__":
    src, new, out = sys.argv[1], sys.argv[2] == "new", sys.argv[3]
    write_dir(build(src, new), out)
