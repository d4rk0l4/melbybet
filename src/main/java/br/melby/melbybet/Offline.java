package br.melby.melbybet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lê inventário e ender chest de jogador que está offline, direto do arquivo .dat dele. */
public final class Offline {
    private Offline() {}

    private static CompoundTag ler(String uuid) {
        Path mundo = Jogo.mundo();
        for (Path f : List.of(mundo.resolve("players").resolve("data").resolve(uuid + ".dat"),
                              mundo.resolve("playerdata").resolve(uuid + ".dat"))) {
            if (!Files.isRegularFile(f)) continue;
            try {
                return NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap());
            } catch (Exception e) {
                Melbybet.LOGGER.warn("Não consegui ler {}", f, e);
            }
        }
        return null;
    }

    private static ItemStack item(RegistryOps<Tag> ops, CompoundTag t) {
        return ItemStack.OPTIONAL_CODEC.parse(ops, t).result().orElse(ItemStack.EMPTY);
    }

    private static RegistryOps<Tag> ops() {
        return RegistryOps.create(NbtOps.INSTANCE, Jogo.server.registryAccess());
    }

    /** 41 itens: 0-35 mochila e hotbar, 36-39 botas, calça, peitoral, capacete, 40 mão secundária. Null se não achar o arquivo. */
    public static List<ItemStack> inventario(String uuid) {
        CompoundTag raiz = ler(uuid);
        if (raiz == null) return null;
        RegistryOps<Tag> ops = ops();
        List<ItemStack> r = new ArrayList<>(Collections.nCopies(41, ItemStack.EMPTY));
        ListTag inv = raiz.getListOrEmpty("Inventory");
        for (int i = 0; i < inv.size(); i++) {
            CompoundTag c = inv.getCompoundOrEmpty(i);
            int slot = c.getIntOr("Slot", -999);
            ItemStack st = item(ops, c);
            if (slot >= 0 && slot < 36) r.set(slot, st);
            else if (slot >= 100 && slot <= 103) r.set(36 + (slot - 100), st);
            else if (slot == -106) r.set(40, st);
        }
        CompoundTag eq = raiz.getCompoundOrEmpty("equipment");
        String[] chaves = {"feet", "legs", "chest", "head", "offhand"};
        for (int i = 0; i < chaves.length; i++) {
            if (eq.contains(chaves[i])) r.set(36 + i, item(ops, eq.getCompoundOrEmpty(chaves[i])));
        }
        return r;
    }

    public static List<ItemStack> ender(String uuid) {
        CompoundTag raiz = ler(uuid);
        if (raiz == null) return null;
        RegistryOps<Tag> ops = ops();
        List<ItemStack> r = new ArrayList<>(Collections.nCopies(27, ItemStack.EMPTY));
        ListTag l = raiz.getListOrEmpty("EnderItems");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            int slot = c.getIntOr("Slot", -1);
            if (slot >= 0 && slot < 27) r.set(slot, item(ops, c));
        }
        return r;
    }
}
