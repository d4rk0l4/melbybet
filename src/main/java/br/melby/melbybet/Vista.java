package br.melby.melbybet;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Janelas que mostram inventário e ender chest: ao vivo (OP mexe de verdade) ou foto (só olhar). */
public final class Vista {
    private Vista() {}

    /** Posição na janela de 54 slots -> índice no inventário do jogador (-1 = enfeite). */
    static int indiceDoInventario(int slotJanela) {
        if (slotJanela < 27) return slotJanela + 9;      // mochila
        if (slotJanela < 36) return slotJanela - 27;     // hotbar
        if (slotJanela < 40) return 39 - (slotJanela - 36); // capacete, peitoral, calça, bota
        if (slotJanela == 40) return 40;                 // mão secundária
        return -1;
    }

    private static GuiElementBuilder enfeite() {
        return new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Component.empty()).hideTooltip();
    }

    /** Inventário de outro jogador ao vivo: quem abre pode pegar e colocar itens. */
    public static void inventarioAoVivo(ServerPlayer quem, ServerPlayer alvo) {
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, quem, false);
        gui.setTitle(Component.literal("Inventário de " + alvo.getGameProfile().name()));
        Container inv = alvo.getInventory();
        for (int i = 0; i < 54; i++) {
            int idx = indiceDoInventario(i);
            if (idx < 0) gui.setSlot(i, enfeite().build());
            else gui.setSlot(i, new Slot(inv, idx, 0, 0));
        }
        gui.open();
    }

    /** Ender chest ao vivo (de si mesmo ou de outro jogador). */
    public static void enderAoVivo(ServerPlayer quem, ServerPlayer alvo) {
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, quem, false);
        gui.setTitle(Component.literal("Ender Chest de " + alvo.getGameProfile().name()));
        Container ec = alvo.getEnderChestInventory();
        for (int i = 0; i < 27; i++) gui.setSlot(i, new Slot(ec, i, 0, 0));
        gui.open();
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
