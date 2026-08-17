import os
import shutil

src = r"E:\59409\Desktop\frxx-decompiled\dan"
dst = r"E:\mods\projects\fanrenxiuxian-mengzhi\src\main\resources\assets\frxxmengzhi\textures\item"

realm_map = {"炼气": "lianqi", "筑基": "zhiji", "结丹": "jiedan", "金丹": "jiedan", "元婴": "yuanying", "化神": "huashen"}
type_map = {"护盾上限": "shield_max", "护盾回复": "shield_regen", "护盾吸收": "shield_absorption"}
prefix_map = {"永久": "perm_", "临时": "temp_"}

files = [f for f in os.listdir(src) if f.endswith(".png")]
print(f"Total files: {len(files)}")

ok = 0
errors = []
for f in files:
    base = f[:-4]
    realm = None
    etype = None
    prefix = None
    for k, v in realm_map.items():
        if k in base:
            realm = v
            break
    for k, v in type_map.items():
        if k in base:
            etype = v
            break
    for k, v in prefix_map.items():
        if k in base:
            prefix = v
            break
    if not realm or not etype or not prefix:
        errors.append(f"PARSE FAIL: {f}")
        continue
    dst_name = f"{prefix}{etype}_{realm}.png"
    shutil.copy2(os.path.join(src, f), os.path.join(dst, dst_name))
    print(f"  {f} -> {dst_name}")
    ok += 1

print(f"Copied {ok} files")
if errors:
    print("ERRORS:")
    for e in errors:
        print("  " + e)
