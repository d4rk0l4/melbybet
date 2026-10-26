import subprocess, glob, os, re, sys
jars = [j for j in glob.glob(os.path.expanduser('~/.gradle/**/*.jar'), recursive=True) if 'minecraft' in os.path.basename(j).lower() and 'sources' not in j and os.path.getsize(j) > 5_000_000]
print(jars)
jar = max(jars, key=os.path.getsize)
def jp(cls, flags=('-p',), grep=None, limit=9000):
    r = subprocess.run(['javap', *flags, '-cp', jar, cls], capture_output=True, text=True)
    out = r.stdout + r.stderr
    if grep:
        out = '\n'.join(l for l in out.splitlines() if re.search(grep, l))
    return out[:limit]
n = [0]
def note(title, body):
    n[0] += 1
    body = body.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print(f'::notice title={title}::{body}')
note('MsgCommand', jp('net.minecraft.server.commands.MsgCommand', ('-v', '-p'), r'= String|Method .*(literal|argument)|register'))
note('CompoundTag', jp('net.minecraft.nbt.CompoundTag', grep=r'get(Byte|Int|List|Compound|String)|contains'))
note('NbtIo', jp('net.minecraft.nbt.NbtIo', grep=r'readCompressed'))
note('NbtAccounter', jp('net.minecraft.nbt.NbtAccounter', grep=r'static'))
note('ItemStack', jp('net.minecraft.world.item.ItemStack', grep=r'CODEC|parse|isEmpty\(\)|copy\(\)'))
note('PlayerChatMessage', jp('net.minecraft.network.chat.PlayerChatMessage', grep=r'Content|decorated|public'))
note('PlayerDataStorage', jp('net.minecraft.world.level.storage.PlayerDataStorage'))
note('ListTag', jp('net.minecraft.nbt.ListTag', grep=r'get|size'))
note('RegistryOps', jp('net.minecraft.resources.RegistryOps', grep=r'static'))
note('PlayerTeam', jp('net.minecraft.world.scores.PlayerTeam', grep=r'Color|Prefix|public'))
