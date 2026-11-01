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

    static void abrir(ServerPlayer p, Casa.Corpo c) {
        if (Jogo.casa.corpo(c.id) == null) {
            p.sendSystemMessage(Jogo.prefixo("Esse corpo já foi esvaziado."));
            return;
        }
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        g.setTitle(Component.literal("Corpo de " + c.nome).withStyle(ChatFormatting.DARK_GRAY));
        for (int i = 0; i < c.itens.size() && i < 45; i++) {
            ItemStack st = Corpos.deJson(c.itens.get(i));
            if (st.isEmpty()) continue;
            final int indice = i;
            g.setSlot(i, GuiElementBuilder.from(st).setCallback((slot, tipo, acao, gui) -> {
                Corpos.pegar(p, c, indice);
                if (Jogo.casa.corpo(c.id) == null) gui.close();
                else abrir(p, c);
            }));
        }
        g.setSlot(49, new GuiElementBuilder(Items.CHEST).setName(Component.literal("Pegar tudo").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
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
