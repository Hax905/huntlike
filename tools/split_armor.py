"""Splits a combined 64x64 armor texture (top half: helmet, chestplate, arms, boots; bottom half: leggings) into the
mod's two worn-armor textures: python3 tools/split_armor.py <combined.png> <set id>, e.g. carrion."""
import sys
from PIL import Image

E = 'src/main/resources/assets/huntlike/textures/entity/equipment/'
src, name = sys.argv[1], sys.argv[2]
im = Image.open(src).convert('RGBA')
if im.size != (64, 64):
    sys.exit('expected a 64x64 image, got %dx%d' % im.size)
# Faint stray pixels (alpha below 100) are dropped, as for every entity texture.
px = im.load()
for y in range(64):
    for x in range(64):
        if 0 < px[x, y][3] < 100:
            px[x, y] = (0, 0, 0, 0)
im.crop((0, 0, 64, 32)).save(E + 'humanoid/' + name + '.png')
im.crop((0, 32, 64, 64)).save(E + 'humanoid_leggings/' + name + '.png')
print('wrote humanoid/%s.png and humanoid_leggings/%s.png' % (name, name))
