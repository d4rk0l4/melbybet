package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Os itens de um corpo: clique num item para pegar só ele. */
final class MenuCorpo {
    private MenuCorpo() {}

    /** Posição no menu do slot de inventário de origem (0-8 hotbar, 9-35 mochila, 36-39 botas a cabeça, 40 mão secundária). -1 se não houver. */
    static int guiDoSlot(int slot) {
        if (slot >= 9 && slot <= 35) return slot;
        if (slot >= 0 && slot <= 8) return 36 + slot;
        return switch (slot) {
            case 39 -> 0;
            case 38 -> 1;
            case 37 -> 2;
            case 36 -> 3;
            case 40 -> 4;
            default -> -1;
        };
    }

    static void abrir(ServerPlayer p, Casa.Corpo c) {
        if (Jogo.casa.corpo(c.id) == null) {
            p.sendSystemMessage(Jogo.prefixo("Esse corpo já foi esvaziado."));
            return;
        }
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        g.setTitle(Component.literal("Corpo de " + c.nome).withStyle(ChatFormatting.DARK_GRAY));
        // mesma disposição do inventário: armadura e mão secundária em cima, mochila no meio, hotbar embaixo
        boolean[] ocupado = new boolean[54];
        int[] lugar = new int[c.itens.size()];
        java.util.Arrays.fill(lugar, -1);
        for (int i = 0; i < c.itens.size(); i++) {
            int s = guiDoSlot(c.slotDe(i));
            if (s >= 0 && !ocupado[s]) {
                lugar[i] = s;
                ocupado[s] = true;
            }
        }
        // itens sem slot de origem (corpos antigos) ou com o lugar tomado: primeiro vazio da mochila, depois do resto
        int[] reserva = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 5, 6, 7, 8, 45, 46, 47, 48, 50, 51, 52};
        for (int i = 0; i < c.itens.size(); i++) {
            if (lugar[i] >= 0) continue;
            for (int r : reserva) {
                if (!ocupado[r]) {
                    lugar[i] = r;
                    ocupado[r] = true;
                    break;
                }
            }
        }
        for (int i = 0; i < c.itens.size(); i++) {
            ItemStack st = Corpos.deJson(c.itens.get(i));
            if (st.isEmpty() || lugar[i] < 0) continue;
            final int indice = i;
            g.setSlot(lugar[i], GuiElementBuilder.from(st).setCallback((slot, tipo, acao, gui) -> {
                Corpos.pegar(p, c, indice);
                if (Jogo.casa.corpo(c.id) == null) gui.close();
                else abrir(p, c);
            }));
        }
        g.setSlot(49, new GuiElementBuilder(Items.CHEST).setName(Component.literal("Transferir itens").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .addLoreLine(Component.literal("Leva todos os itens que couberem").withStyle(ChatFormatting.GRAY))
            .setCallback((slot, tipo, acao, gui) -> {
                Corpos.pegarTudo(p, c);
                if (Jogo.casa.corpo(c.id) == null) gui.close();
                else abrir(p, c);
            }));
        g.setSlot(53, new GuiElementBuilder(Items.BARRIER).setName(Component.literal("Fechar").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .setCallback((slot, tipo, acao, gui) -> gui.close()));
        g.open();
    }
}
