package br.melby.melbybet;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Janelas que mostram inventário e ender chest: ao vivo (OP mexe de verdade) ou foto (só olhar). */
public final class Vista {
    private Vista() {}

    /**
     * Posição na janela de 54 slots -> índice no inventário do jogador (-1 = enfeite).
     * Layout parecido com o do inventário: armadura em cima, mochila no meio e hotbar embaixo.
     */
    static int indiceDoInventario(int slotJanela) {
        if (slotJanela >= 0 && slotJanela <= 3) return 39 - slotJanela;   // capacete, peitoral, calça, bota
        if (slotJanela == 5) return 40;                                    // mão secundária
        if (slotJanela >= 18 && slotJanela <= 44) return slotJanela - 18 + 9; // mochila (3 linhas)
        if (slotJanela >= 45 && slotJanela <= 53) return slotJanela - 45;     // hotbar
        return -1;
    }

    private static GuiElementBuilder enfeite() {
        return new GuiElementBuilder(Items.STAINED_GLASS_PANE.gray()).setName(Component.empty()).hideTooltip();
    }

    /** Inventário de outro jogador, só para olhar: nada pode ser pego nem colocado. */
    public static void inventarioAoVivo(ServerPlayer quem, ServerPlayer alvo) {
        List<ItemStack> itens = new ArrayList<>();
        for (int i = 0; i < alvo.getInventory().getContainerSize(); i++) itens.add(alvo.getInventory().getItem(i).copy());
        foto(quem, alvo.getGameProfile().name(), false, itens);
    }

    /** Ender chest de outro jogador, só para olhar. */
    public static void enderAoVivo(ServerPlayer quem, ServerPlayer alvo) {
        List<ItemStack> itens = new ArrayList<>();
        for (int i = 0; i < alvo.getEnderChestInventory().getContainerSize(); i++) itens.add(alvo.getEnderChestInventory().getItem(i).copy());
        foto(quem, alvo.getGameProfile().name(), true, itens);
    }

    /** Foto de um inventário compartilhado no chat: só olhar, nada sai dali. */
    public static void foto(ServerPlayer quem, String dono, boolean ender, List<ItemStack> itens) {
        if (ender) {
            SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, quem, false);
            gui.setTitle(Component.literal("Ender Chest de " + dono));
            for (int i = 0; i < 27 && i < itens.size(); i++) {
                if (!itens.get(i).isEmpty()) gui.setSlot(i, GuiElementBuilder.from(itens.get(i).copy()).build());
            }
            gui.open();
            return;
        }
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, quem, false);
        gui.setTitle(Component.literal("Inventário de " + dono));
        for (int i = 0; i < 54; i++) {
            int idx = indiceDoInventario(i);
            if (idx < 0 || idx >= itens.size()) gui.setSlot(i, enfeite().build());
            else if (itens.get(idx).isEmpty()) gui.setSlot(i, enfeite().build());
            else gui.setSlot(i, GuiElementBuilder.from(itens.get(idx).copy()).build());
        }
        gui.open();
    }
}
