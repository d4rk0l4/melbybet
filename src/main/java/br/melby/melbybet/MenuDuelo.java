package br.melby.melbybet;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Janela para escolher, no inventário do adversário, os itens que ele vai apostar no duelo. */
final class MenuDuelo {
    private MenuDuelo() {}

    static void abrir(ServerPlayer quem, ServerPlayer dono, Duelos.Itens st, boolean souA) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, quem, false);
        g.setTitle(Component.literal("Itens de " + dono.getGameProfile().name()));
        Set<Integer> escolhidos = new LinkedHashSet<>();
        desenhar(g, quem, dono, st, souA, escolhidos);
        g.open();
    }

    private static void desenhar(SimpleGui g, ServerPlayer quem, ServerPlayer dono, Duelos.Itens st, boolean souA, Set<Integer> escolhidos) {
        for (int slot = 0; slot < 54; slot++) {
            int idx = Vista.indiceDoInventario(slot);
            ItemStack item = idx >= 0 && idx < dono.getInventory().getContainerSize() ? dono.getInventory().getItem(idx) : ItemStack.EMPTY;
            if (item.isEmpty()) {
                g.setSlot(slot, Vista.enfeite().build());
                continue;
            }
            boolean marcado = escolhidos.contains(idx);
            GuiElementBuilder e = GuiElementBuilder.from(item.copy());
            e.addLoreLine(Component.literal(marcado ? "ESCOLHIDO. Clique para tirar" : "Clique para escolher").withStyle(marcado ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            if (marcado) e.glow();
            e.setCallback((i, ty, a, gg) -> {
                if (escolhidos.contains(idx)) {
                    escolhidos.remove(idx);
                } else if (escolhidos.size() >= Duelos.MAX_ITENS) {
                    quem.sendSystemMessage(Jogo.prefixo("No máximo " + Duelos.MAX_ITENS + " itens."));
                } else {
                    escolhidos.add(idx);
                }
                desenhar(g, quem, dono, st, souA, escolhidos);
            });
            g.setSlot(slot, e.build());
        }
        g.setSlot(9, new GuiElementBuilder(Items.BARRIER).setName(Component.literal("Cancelar duelo").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .setCallback((i, ty, a, gg) -> {
                gg.close();
                Duelos.cancelarEscolha(quem, st);
            }).build());
        boolean pronto = !escolhidos.isEmpty();
        GuiElementBuilder ok = new GuiElementBuilder(pronto ? Items.EMERALD : Items.BARRIER)
            .setName(Component.literal(pronto ? "Confirmar escolha (" + escolhidos.size() + ")" : "Escolha pelo menos 1 item")
                .withStyle(pronto ? ChatFormatting.GREEN : ChatFormatting.GRAY, ChatFormatting.BOLD));
        if (pronto) {
            ok.addLoreLine(Component.literal("Nada sai agora. Só o perdedor entrega, depois do duelo.").withStyle(ChatFormatting.GRAY));
            ok.setCallback((i, ty, a, gg) -> {
                List<ItemStack> lista = new ArrayList<>();
                for (int idx : escolhidos) {
                    ItemStack it = dono.getInventory().getItem(idx);
                    if (!it.isEmpty()) lista.add(it.copy());
                }
                gg.close();
                if (lista.isEmpty()) {
                    quem.sendSystemMessage(Jogo.prefixo("Esses itens não estão mais com " + dono.getGameProfile().name() + "."));
                    Duelos.cancelarEscolha(quem, st);
                    return;
                }
                Duelos.confirmarEscolha(quem, st, souA, lista);
            });
        }
        g.setSlot(13, ok.build());
    }
}
