package br.melby.melbybet;

import br.melby.melbybet.core.Ranking;
import br.melby.melbybet.core.StatCat;
import com.google.gson.JsonObject;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/** /top: um item por categoria; o ranking aparece na descrição e, ao clicar, no chat. */
public final class MenuTop {
    private MenuTop() {}

    private static final int NO_ITEM = 10;
    private static final int NO_CHAT = 10;

    static Item icone(StatCat c) {
        switch (c) {
            case DISTANCIA: return Items.COMPASS;
            case ANDOU: return Items.LEATHER_BOOTS;
            case CORREU: return Items.GOLDEN_BOOTS;
            case VOOU: return Items.ELYTRA;
            case NADOU: return Items.TROPICAL_FISH;
            case CAVALGOU: return Items.SADDLE;
            case QUEBROU: return Items.DIAMOND_PICKAXE;
            case COLOCOU: return Items.GRASS_BLOCK;
            case MATOU: return Items.DIAMOND_SWORD;
            case MORREU: return Items.SKELETON_SKULL;
            case TEMPO: return Items.CLOCK;
            default: return Items.CRAFTING_TABLE;
        }
    }

    private static ChatFormatting corDaPosicao(int pos) {
        if (pos == 1) return ChatFormatting.GOLD;
        if (pos == 2) return ChatFormatting.GRAY;
        if (pos == 3) return ChatFormatting.RED;
        return ChatFormatting.DARK_GRAY;
    }

    private static Component linha(int pos, Ranking.Linha l, StatCat c) {
        return Component.literal(pos + "º ").withStyle(corDaPosicao(pos))
            .append(Component.literal(l.nome).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("  " + c.formatar(l.valor)).withStyle(ChatFormatting.YELLOW));
    }

    public static void abrir(ServerPlayer p) {
        Map<String, JsonObject> stats = Jogo.statsTodos();
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        gui.setTitle(Component.literal("Ranking da Gangue"));
        int[] slots = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14};
        StatCat[] cats = StatCat.values();
        for (int i = 0; i < cats.length; i++) {
            StatCat c = cats[i];
            List<Ranking.Linha> rank = Ranking.montar(c, stats);
            GuiElementBuilder b = new GuiElementBuilder(icone(c))
                .setName(Component.literal(c.rotulo).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .hideDefaultTooltip();
            if (rank.isEmpty()) {
                b.addLoreLine(Component.literal("Ninguém pontuou ainda.").withStyle(ChatFormatting.GRAY));
            } else {
                for (int k = 0; k < Math.min(NO_ITEM, rank.size()); k++) b.addLoreLine(linha(k + 1, rank.get(k), c));
            }
            b.addLoreLine(Component.literal("Clique para mandar no chat").withStyle(ChatFormatting.DARK_GRAY));
            b.setCallback((index, type, action, g) -> chat(p, c, rank));
            gui.setSlot(slots[i], b.build());
        }
        gui.setSlot(22, new GuiElementBuilder(Items.BARRIER)
            .setName(Component.literal("Fechar").withStyle(ChatFormatting.RED))
            .setCallback((index, type, action, g) -> g.close()).build());
        gui.open();
    }

    private static void chat(ServerPlayer p, StatCat c, List<Ranking.Linha> rank) {
        p.sendSystemMessage(Component.literal("== " + c.rotulo + " ==").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (rank.isEmpty()) {
            p.sendSystemMessage(Component.literal("Ninguém pontuou ainda.").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int k = 0; k < Math.min(NO_CHAT, rank.size()); k++) p.sendSystemMessage(linha(k + 1, rank.get(k), c));
    }
}
