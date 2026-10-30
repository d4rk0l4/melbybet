package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Fmt;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** /limpar: configura a limpeza automática dos itens do chão. */
final class MenuLimpeza {
    private MenuLimpeza() {}

    private static GuiElementBuilder btn(Item item, String nome, ChatFormatting cor, String... lore) {
        GuiElementBuilder e = new GuiElementBuilder(item).setName(Component.literal(nome).withStyle(cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        for (String l : lore) e.addLoreLine(Component.literal(l).withStyle(ChatFormatting.GRAY));
        return e;
    }

    private static void numero(ServerPlayer p, String pergunta, int min, int max, java.util.function.IntConsumer ok, Runnable depois) {
        Chat.perguntar(p, pergunta, txt -> {
            try {
                int v = Integer.parseInt(txt.trim());
                if (v < min || v > max) p.sendSystemMessage(Jogo.prefixo("Use um número de " + min + " a " + max + "."));
                else {
                    ok.accept(v);
                    Jogo.salvar();
                }
            } catch (NumberFormatException e) {
                p.sendSystemMessage(Jogo.prefixo("Isso não é um número."));
            }
            depois.run();
        });
    }

    static void abrir(ServerPlayer p, Runnable voltar) {
        Casa.Config c = Jogo.casa.config;
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x4, p, false);
        g.setTitle(Component.literal("Limpeza do chão").withStyle(ChatFormatting.GOLD));

        long resta = Limpeza.restanteMs();
        g.setSlot(4, btn(Items.HOPPER, c.limpezaMinutos > 0 ? "Ligada: a cada " + c.limpezaMinutos + " min" : "Desligada", c.limpezaMinutos > 0 ? ChatFormatting.GREEN : ChatFormatting.RED,
            resta >= 0 ? "Próxima limpeza em " + Fmt.duracao(resta) : "Escolha um intervalo abaixo.").build());

        int[] mins = {5, 10, 15, 30, 60};
        for (int i = 0; i < mins.length; i++) {
            int m = mins[i];
            GuiElementBuilder e = btn(Items.CLOCK, "A cada " + m + " min", ChatFormatting.AQUA, "Clique para escolher");
            if (c.limpezaMinutos == m) e.glow();
            e.setCallback((ix, ty, a, gg) -> {
                c.limpezaMinutos = m;
                Jogo.salvar();
                Limpeza.reagendar();
                abrir(p, voltar);
            });
            g.setSlot(10 + i, e.build());
        }
        g.setSlot(15, btn(Items.NAME_TAG, "Outro intervalo", ChatFormatting.YELLOW, "Digite os minutos no chat (1 a 720)")
            .setCallback((i, ty, a, gg) -> numero(p, "De quantos em quantos minutos limpar? (1 a 720)", 1, 720, v -> {
                c.limpezaMinutos = v;
                Limpeza.reagendar();
            }, () -> abrir(p, voltar))).build());
        g.setSlot(16, btn(Items.BARRIER, "Desligar", ChatFormatting.RED, "Para de apagar sozinho")
            .setCallback((i, ty, a, gg) -> {
                c.limpezaMinutos = 0;
                Jogo.salvar();
                Limpeza.reagendar();
                abrir(p, voltar);
            }).build());

        g.setSlot(19, btn(Items.BELL, "Aviso: " + c.limpezaAvisoSeg + "s antes", ChatFormatting.GOLD, "Depois vem a contagem 10, 5, 3, 2, 1", "Clique para digitar os segundos (5 a 300)")
            .setCallback((i, ty, a, gg) -> numero(p, "Avisar quantos segundos antes? (5 a 300)", 5, 300, v -> c.limpezaAvisoSeg = v, () -> abrir(p, voltar))).build());
        g.setSlot(20, btn(Items.CLOCK, "Itens novos: " + c.limpezaIdadeMinSeg + "s", ChatFormatting.GOLD, "Item largado há menos que isso não é apagado", "Clique para digitar os segundos (0 a 600)")
            .setCallback((i, ty, a, gg) -> numero(p, "Segundos que um item recém-largado fica protegido? (0 a 600)", 0, 600, v -> c.limpezaIdadeMinSeg = v, () -> abrir(p, voltar))).build());
        g.setSlot(21, btn(c.limpezaProtege ? Items.DIAMOND : Items.COAL, c.limpezaProtege ? "Proteger valiosos: sim" : "Proteger valiosos: não",
            c.limpezaProtege ? ChatFormatting.GREEN : ChatFormatting.RED, "Diamante, netherite, elytra, totem, estrela do Nether,", "itens encantados e itens com nome ficam no chão.")
            .setCallback((i, ty, a, gg) -> {
                c.limpezaProtege = !c.limpezaProtege;
                Jogo.salvar();
                abrir(p, voltar);
            }).build());
        g.setSlot(22, btn(c.limpezaAviso ? Items.BELL : Items.BARRIER, c.limpezaAviso ? "Avisar no chat: sim" : "Avisar no chat: não",
            c.limpezaAviso ? ChatFormatting.GREEN : ChatFormatting.RED, "Liga ou desliga o aviso antes da limpeza", "(o mesmo que /limpar aviso sim|nao)")
            .setCallback((i, ty, a, gg) -> {
                c.limpezaAviso = !c.limpezaAviso;
                Jogo.salvar();
                abrir(p, voltar);
            }).build());
        g.setSlot(23, btn(Items.TNT, "Limpar agora", ChatFormatting.RED, "Apaga já, sem aviso, e conta de novo o tempo")
            .setCallback((i, ty, a, gg) -> {
                Limpeza.executar();
                abrir(p, voltar);
            }).build());

        if (voltar != null) g.setSlot(27, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> voltar.run()).build());
        else g.setSlot(27, btn(Items.BARRIER, "Fechar", ChatFormatting.RED).setCallback((i, ty, a, gg) -> gg.close()).build());
        g.open();
    }
}
