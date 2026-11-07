package br.melby.melbybet.client;

import br.melby.melbybet.core.Organizar;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.nio.file.Path;

/** Perfil de organização do inventário deste jogador (config/melbybet/inventario.json). */
public final class InventarioCliente {
    private InventarioCliente() {}

    public static Organizar.Perfil perfil = Organizar.Perfil.padrao();
    private static Path arquivo;

    public static void carregar() {
        arquivo = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("inventario.json");
        perfil = Organizar.lerPerfil(arquivo);
    }

    public static void salvar() {
        if (arquivo == null) return;
        try {
            Organizar.gravarPerfil(arquivo, perfil);
        } catch (Exception e) {
            // sem disco: perde só esta gravação
        }
    }

    public static Item icone(Organizar.Categoria c) {
        return switch (c) {
            case ESPADA -> Items.IRON_SWORD;
            case MACHADO -> Items.IRON_AXE;
            case PICARETA -> Items.IRON_PICKAXE;
            case PA -> Items.IRON_SHOVEL;
            case ENXADA -> Items.IRON_HOE;
            case ARCO -> Items.BOW;
            case BESTA -> Items.CROSSBOW;
            case TRIDENTE -> Items.TRIDENT;
            case MACA -> Items.MACE;
            case VARA -> Items.FISHING_ROD;
            case FERRAMENTA -> Items.SHEARS;
            case FLECHA -> Items.ARROW;
            case POCAO -> Items.POTION;
            case PEROLA -> Items.ENDER_PEARL;
            case FOGUETE -> Items.FIREWORK_ROCKET;
            case TOTEM -> Items.TOTEM_OF_UNDYING;
            case TOCHA -> Items.TORCH;
            case BALDE -> Items.WATER_BUCKET;
            case COMIDA -> Items.COOKED_BEEF;
            case BLOCO -> Items.COBBLESTONE;
            case MATERIAL -> Items.IRON_INGOT;
            case OUTRO -> Items.NAME_TAG;
        };
    }

    public static void aviso(String texto) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal(texto));
    }
}
