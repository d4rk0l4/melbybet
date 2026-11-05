package br.melby.melbybet;

import br.melby.melbybet.core.DueloLog;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/** Aba "Duelos" do /melby (staff): log dos duelos, rastro do prêmio e, para OP, anulação. */
final class MenuDuelosStaff {
    private MenuDuelosStaff() {}

    private static final int POR_PAGINA = 45;

    private static GuiElementBuilder btn(Item item, String nome, ChatFormatting cor, String... lore) {
        GuiElementBuilder e = new GuiElementBuilder(item).setName(Component.literal(nome).withStyle(cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        for (String l : lore) e.addLoreLine(Component.literal(l).withStyle(ChatFormatting.GRAY));
        return e;
    }

    static void mandar(ServerPlayer p, List<String> linhas) {
        for (String l : linhas) p.sendSystemMessage(Component.literal(l).withStyle(ChatFormatting.GRAY));
    }

    static void lista(ServerPlayer p, int pagina, Runnable voltar) {
        List<DueloLog.Reg> todos = DueloLogs.log.recentes(500);
        int paginas = Math.max(1, (todos.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pg = Math.max(0, Math.min(pagina, paginas - 1));
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        g.setTitle(Component.literal("Duelos (" + todos.size() + ")").withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pg * POR_PAGINA + i;
            if (idx >= todos.size()) break;
            DueloLog.Reg r = todos.get(idx);
            Item item = switch (r.estado) {
                case "RESOLVIDO" -> Items.PAPER;
                case "ANULADO" -> Items.BARRIER;
                case "CANCELADO" -> Items.COAL;
                default -> Items.SLIME_BALL;
            };
            String quando = DueloLog.data(r.inicio > 0 ? r.inicio : r.criadoEm, DueloLogs.fuso());
            g.setSlot(i, btn(item, "#" + r.id + " " + r.nomeA + " x " + r.nomeB, ChatFormatting.YELLOW,
                quando,
                r.itens ? "Duelo de itens" : r.valor + " diamante(s) cada",
                r.estado + (r.vencedorNome.isEmpty() ? "" : ": " + r.vencedorNome),
                "Clique para ver os detalhes").setCallback((ix, ty, a, gg) -> detalhe(p, r.id, () -> lista(p, pg, voltar))).build());
        }
        g.setSlot(45, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> voltar.run()).build());
        if (pg > 0) g.setSlot(48, btn(Items.SPECTRAL_ARROW, "Página anterior", ChatFormatting.AQUA).setCallback((i, ty, a, gg) -> lista(p, pg - 1, voltar)).build());
        g.setSlot(49, btn(Items.BOOK, "Página " + (pg + 1) + " de " + paginas, ChatFormatting.WHITE, "Do mais novo para o mais antigo").build());
        if (pg < paginas - 1) g.setSlot(50, btn(Items.SPECTRAL_ARROW, "Próxima página", ChatFormatting.AQUA).setCallback((i, ty, a, gg) -> lista(p, pg + 1, voltar)).build());
        g.open();
    }

    static void detalhe(ServerPlayer p, int id, Runnable voltar) {
        DueloLog.Reg r = DueloLogs.log.achar(id);
        if (r == null) {
            p.sendSystemMessage(Jogo.prefixo("Esse duelo não está mais no log."));
            voltar.run();
            return;
        }
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x4, p, false);
        g.setTitle(Component.literal("Duelo #" + id).withStyle(ChatFormatting.GOLD));
        String f = DueloLogs.fuso();
        g.setSlot(4, btn(Items.PAPER, r.nomeA + " x " + r.nomeB, ChatFormatting.YELLOW,
            "Início: " + DueloLog.data(r.inicio > 0 ? r.inicio : r.criadoEm, f),
            "Fim: " + DueloLog.data(r.fim, f),
            "Estado: " + r.estado,
            r.vencedorNome.isEmpty() ? "Sem vencedor" : "Vencedor: " + r.vencedorNome,
            r.itens ? "Duelo de itens" : r.valor + " diamante(s) cada").build());
        g.setSlot(11, btn(Items.WRITABLE_BOOK, "Registro completo no chat", ChatFormatting.AQUA, "Eventos do duelo, arena e origem dos jogadores")
            .setCallback((i, ty, a, gg) -> {
                mandar(p, DueloLogs.detalhe(r, false));
                gg.close();
            }).build());
        g.setSlot(13, btn(Items.COMPASS, "Rastro do prêmio", ChatFormatting.GREEN, "O que o vencedor fez com o que ganhou",
            r.rastroAte > System.currentTimeMillis() ? "Ainda acompanhando" : "Janela encerrada")
            .setCallback((i, ty, a, gg) -> {
                mandar(p, DueloLogs.detalhe(r, true));
                gg.close();
            }).build());
        boolean pode = Comandos.admin(p) && (r.estado.equals("RESOLVIDO") || r.estado.equals("EM ANDAMENTO"));
        if (pode) {
            g.setSlot(15, btn(Items.TNT, "Anular duelo", ChatFormatting.RED,
                r.itens ? "Devolve os itens ao perdedor (ele precisa estar online)" : "Devolve a aposta a cada um",
                "Pede o motivo e uma confirmação").setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Motivo da anulação do duelo #" + id + "? (digite no chat; 'cancelar' desiste)", txt -> {
                    String motivo = txt.trim();
                    if (motivo.isEmpty() || motivo.equalsIgnoreCase("cancelar")) {
                        detalhe(p, id, voltar);
                        return;
                    }
                    confirmar(p, id, motivo, voltar);
                })).build());
        } else if (!r.anuladoPor.isEmpty()) {
            g.setSlot(15, btn(Items.BARRIER, "Anulado por " + r.anuladoPor, ChatFormatting.RED, r.anuladoMotivo, r.anuladoResumo).build());
        }
        g.setSlot(27, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> voltar.run()).build());
        g.open();
    }

    private static void confirmar(ServerPlayer p, int id, String motivo, Runnable voltar) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(Component.literal("Anular duelo #" + id + "?").withStyle(ChatFormatting.RED));
        g.setSlot(4, btn(Items.PAPER, "Motivo", ChatFormatting.YELLOW, motivo).build());
        g.setSlot(11, btn(Items.EMERALD_BLOCK, "CONFIRMAR ANULAÇÃO", ChatFormatting.GREEN, "Isso mexe com diamantes e itens e fica registrado com o seu nome")
            .setCallback((i, ty, a, gg) -> {
                String res = DueloLogs.anular(p.getGameProfile().name(), id, motivo);
                p.sendSystemMessage(Jogo.prefixo(res));
                detalhe(p, id, voltar);
            }).build());
        g.setSlot(15, btn(Items.REDSTONE_BLOCK, "Voltar sem anular", ChatFormatting.RED).setCallback((i, ty, a, gg) -> detalhe(p, id, voltar)).build());
        g.open();
    }
}
