"""Splits a combined 64x64 armor texture (top half: helmet, chestplate, arms, boots; bottom half: leggings) into the
mod's two worn-armor textures: python3 tools/split_armor.py <combined.png> <set id>, e.g. carrion."""
import sys
from PIL import Image

E = 'src/main/resources/assets/huntlike/textures/entity/equipment/'
# The Cluster Hunt armor is drawn see-through: every painted pixel becomes 50% solid.
SEE_THROUGH = {'icy', 'gel', 'thermodynamic'}

src, name = sys.argv[1], sys.argv[2]
im = Image.open(src).convert('RGBA')
if im.size != (64, 64):
    sys.exit('expected a 64x64 image, got %dx%d' % im.size)
px = im.load()
for y in range(64):
    for x in range(64):
        r, g, b, a = px[x, y]
        if name in SEE_THROUGH:
            # Painted pixels at half opacity (already lighter ones are halved too).
            px[x, y] = (r, g, b, a // 2 if a < 255 else 128) if a else (0, 0, 0, 0)
        elif 0 < a < 100:
            # Faint stray pixels are dropped, as for every entity texture.
            px[x, y] = (0, 0, 0, 0)
im.crop((0, 0, 64, 32)).save(E + 'humanoid/' + name + '.png')
im.crop((0, 32, 64, 64)).save(E + 'humanoid_leggings/' + name + '.png')
print('wrote humanoid/%s.png and humanoid_leggings/%s.png' % (name, name))
