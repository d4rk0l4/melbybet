import subprocess, glob, os, re
allj = glob.glob(os.path.expanduser('~/.gradle/**/*.jar'), recursive=True)
jar = None
for j in sorted(allj, key=os.path.getsize, reverse=True):
    if os.path.getsize(j) < 3_000_000: break
    names = subprocess.run(['unzip','-Z1',j],capture_output=True,text=True).stdout
    if 'net/minecraft/world/item/ItemStack.class' in names:
        jar = j
        break
def jp(cls, grep=None, flags=('-p',)):
    r = subprocess.run(['javap', *flags, '-cp', jar, cls], capture_output=True, text=True)
    out = r.stdout + r.stderr
    lines = [l.strip() for l in out.splitlines()[1:]]
    if grep:
        lines = [l for l in lines if re.search(grep, l)]
    lines = [re.sub(r'(net\.minecraft\.[a-z.]*\.|java\.[a-z.]*\.|com\.mojang\.[a-z.]*\.)', '', l) for l in lines]
    return '\n'.join(lines)
k = [0]
def note(title, body):
    k[0] += 1
    body = body[:3900].replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print(f'::notice file=probe/probe.py,line={k[0]},title={title}::{body}')
note('CompoundTag', jp('net.minecraft.nbt.CompoundTag', r'(getByte|getInt|getList|getCompound|getString|contains|keySet|getShort)'))
note('NbtIo', jp('net.minecraft.nbt.NbtIo', r'read') + '\n' + jp('net.minecraft.nbt.NbtAccounter', r'static'))
note('ItemStack', jp('net.minecraft.world.item.ItemStack', r'(CODEC|parse|isEmpty|static .*ItemStack)'))
note('PlayerChatMessage', jp('net.minecraft.network.chat.PlayerChatMessage', r'Content|decorated|unsigned|signed'))
cands = subprocess.run(['unzip','-Z1',jar],capture_output=True,text=True).stdout.splitlines()
note('Storage', '\n'.join(c for c in cands if 'PlayerData' in c or 'LevelStorageSource' in c or 'storage/Tag' in c or 'ValueInput' in c)[:1500])
note('ListTag', jp('net.minecraft.nbt.ListTag', r'(get|size)'))
note('RegistryOps', jp('net.minecraft.resources.RegistryOps', r'static'))
note('PlayerTeam', jp('net.minecraft.world.scores.PlayerTeam', r'(Color|Prefix|Suffix)'))
note('MessageArgument', jp('net.minecraft.commands.arguments.MessageArgument'))
note('ServerPlayer', jp('net.minecraft.server.level.ServerPlayer', r'(TabList|DisplayName|sendSystemMessage|sendChatMessage|getChatSession)'))
