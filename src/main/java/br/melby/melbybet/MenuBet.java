package br.melby.melbybet;

import br.melby.melbybet.core.Bolao;
import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Fmt;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.List;

/** /bet: lista de bolões, opções com cabeças dos jogadores, escolha do valor, histórico e ranking. */
public final class MenuBet {
    private MenuBet() {}

    private static Component t(String s, ChatFormatting... cor) {
        return Component.literal(s).withStyle(cor);
    }

    private static GuiElementBuilder botao(net.minecraft.world.item.Item item, String nome, ChatFormatting cor) {
        return new GuiElementBuilder(item).setName(t(nome, cor)).hideDefaultTooltip();
    }

    private static String odd(double o) {
        return o <= 0 ? "sem apostas" : "paga " + Fmt.decimal(o, 2) + "x";
    }

    // ---------- tela principal ----------

    public static void abrir(ServerPlayer p) {
        Casa casa = Jogo.casa;
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        gui.setTitle(t("MelbyBet", ChatFormatting.GOLD, ChatFormatting.BOLD));

        gui.setSlot(4, new GuiElementBuilder(Items.DIAMOND)
            .setName(t("Casa de apostas da Gangue", ChatFormatting.AQUA, ChatFormatting.BOLD))
            .addLoreLine(t("Seus diamantes: " + Jogo.contar(p), ChatFormatting.WHITE))
            .addLoreLine(t("Cofre da Casa: " + casa.cofre, ChatFormatting.GRAY))
            .addLoreLine(t("Jackpot acumulado: " + casa.jackpot, ChatFormatting.YELLOW))
            .addLoreLine(t("A casa fica com " + casa.config.rakePct + "% de cada pote.", ChatFormatting.DARK_GRAY))
            .build());

        List<Bolao> ativos = casa.ativos();
        int slot = 9;
        for (Bolao b : ativos) {
            if (slot >= 45) break;
            gui.setSlot(slot++, itemBolao(p, b));
        }
        if (ativos.isEmpty()) {
            gui.setSlot(22, botao(Items.BARRIER, "Nenhum bolão ativo", ChatFormatting.GRAY)
                .addLoreLine(t("Um OP cria com /melbybet criar", ChatFormatting.DARK_GRAY)).build());
        }

        gui.setSlot(45, botao(Items.WRITTEN_BOOK, "Como funciona", ChatFormatting.YELLOW)
            .setCallback((i, ty, a, g) -> Tutorial.ajuda(p)).build());
        gui.setSlot(46, botao(Items.BOOK, "Meu histórico", ChatFormatting.GREEN)
            .setCallback((i, ty, a, g) -> historico(p)).build());
        gui.setSlot(47, botao(Items.NETHER_STAR, "Combinada", ChatFormatting.LIGHT_PURPLE)
            .addLoreLine(t("Aposte em vários bolões de uma vez.", ChatFormatting.GRAY))
            .addLoreLine(t("Acertou tudo: bônus da casa!", ChatFormatting.YELLOW))
            .setCallback((i, ty, a, g) -> MenuCombinada.abrir(p)).build());
        gui.setSlot(48, botao(Items.GOLD_INGOT, "Ranking de apostadores", ChatFormatting.GOLD)
            .setCallback((i, ty, a, g) -> rankingApostadores(p)).build());
        gui.setSlot(50, botao(Items.COMPASS, "Ranking de stats (/top)", ChatFormatting.AQUA)
            .setCallback((i, ty, a, g) -> MenuTop.abrir(p)).build());
        gui.setSlot(52, botao(Items.BARRIER, "Fechar", ChatFormatting.RED)
            .setCallback((i, ty, a, g) -> g.close()).build());
        gui.open();
    }

    private static eu.pb4.sgui.api.elements.SimpleGuiElement itemBolao(ServerPlayer p, Bolao b) {
        String uuid = p.getUUID().toString();
        net.minecraft.world.item.Item icone = b.status == Bolao.Status.ABERTO ? Items.WRITABLE_BOOK
            : b.status == Bolao.Status.AGUARDANDO ? Items.CLOCK : Items.BOOK;
        GuiElementBuilder e = new GuiElementBuilder(icone)
            .setName(t("#" + b.id + "  " + b.titulo, ChatFormatting.GOLD, ChatFormatting.BOLD))
            .hideDefaultTooltip()
            .addLoreLine(t(b.dueloId > 0 ? "Resolve: o resultado do duelo" : "Resolve: " + b.gatilho.rotulo, ChatFormatting.GRAY))
            .addLoreLine(t("Pote: " + b.pote() + " diamantes", ChatFormatting.AQUA));
        int meu = b.apostadoPor(uuid);
        if (meu > 0) e.addLoreLine(t("Você apostou: " + meu, ChatFormatting.GREEN));
        switch (b.status) {
            case ABERTO -> e.addLoreLine(t("Clique para apostar", ChatFormatting.YELLOW));
            case FECHADO -> e.addLoreLine(t("Apostas encerradas", ChatFormatting.RED));
            default -> e.addLoreLine(t("Aguardando confirmação de um OP", ChatFormatting.RED));
        }
        e.setCallback((i, ty, a, g) -> opcoes(p, b));
        return e.build();
    }

    // ---------- opções do bolão ----------

    static void opcoes(ServerPlayer p, Bolao b) {
        Casa casa = Jogo.casa;
        String uuid = p.getUUID().toString();
        String nome = p.getGameProfile().name();
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x4, p, false);
        gui.setTitle(t("#" + b.id + " " + b.titulo, ChatFormatting.GOLD));
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < b.opcoes.size() && i < slots.length; i++) {
            String op = b.opcoes.get(i);
            boolean cabeca = casa.config.roster.stream().anyMatch(r -> r.equalsIgnoreCase(op));
            GuiElementBuilder e = new GuiElementBuilder(cabeca ? Items.PLAYER_HEAD : Items.PAPER)
                .setName(t(op, ChatFormatting.WHITE, ChatFormatting.BOLD))
                .hideDefaultTooltip()
                .addLoreLine(t("Apostado nele: " + b.poteDa(op), ChatFormatting.AQUA))
                .addLoreLine(t("Se ganhar agora, " + odd(casa.odd(b, op)), ChatFormatting.YELLOW));
            if (cabeca) e.setProfile(op);
            int minha = 0;
            for (Bolao.Aposta a : b.apostas) if (a.uuid.equals(uuid) && a.opcao.equalsIgnoreCase(op)) minha += a.valor;
            if (minha > 0) e.addLoreLine(t("Sua aposta: " + minha, ChatFormatting.GREEN));
            if (op.equalsIgnoreCase(nome)) e.addLoreLine(t("Você não pode apostar em si mesmo", ChatFormatting.RED));
            else if (b.aberto()) e.addLoreLine(t("Clique para escolher o valor", ChatFormatting.YELLOW));
            e.setCallback((ix, ty, a, g) -> {
                if (!b.aberto()) {
                    p.sendSystemMessage(Jogo.prefixo("Esse bolão não está mais aberto."));
                    return;
                }
                if (op.equalsIgnoreCase(nome)) {
                    p.sendSystemMessage(Jogo.prefixo("Não pode apostar em você mesmo."));
                    return;
                }
                valor(p, b, op);
            });
            gui.setSlot(slots[i], e.build());
        }
        if (b.aberto() && b.apostadoPor(uuid) > 0) {
            int volta = b.apostadoPor(uuid) * (100 - casa.config.cashoutTaxaPct) / 100;
            gui.setSlot(31, botao(Items.REDSTONE, "Desistir das apostas", ChatFormatting.RED)
                .addLoreLine(t("Devolve " + volta + " diamantes (taxa de " + casa.config.cashoutTaxaPct + "%)", ChatFormatting.GRAY))
                .setCallback((ix, ty, a, g) -> {
                    int v = casa.cashout(b, uuid);
                    if (v >= 0) {
                        Jogo.entregar(p);
                        Jogo.salvar();
                        p.sendSystemMessage(Jogo.prefixo("Você desistiu e recebeu " + v + " diamante(s)."));
                    }
                    opcoes(p, b);
                }).build());
        }
        gui.setSlot(27, botao(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((ix, ty, a, g) -> abrir(p)).build());
        gui.open();
    }

    // ---------- valor ----------

    private static void valor(ServerPlayer p, Bolao b, String opcao) {
        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        gui.setTitle(t("Apostar em " + opcao, ChatFormatting.GOLD));
        int[] valores = {1, 5, 10, 32, 64};
        int[] slots = {10, 11, 12, 13, 14};
        for (int i = 0; i < valores.length; i++) {
            int v = valores[i];
            gui.setSlot(slots[i], new GuiElementBuilder(Items.DIAMOND, Math.min(v, 64))
                .setName(t(v + " diamante" + (v > 1 ? "s" : ""), ChatFormatting.AQUA))
                .addLoreLine(t("Clique para apostar", ChatFormatting.YELLOW))
                .setCallback((ix, ty, a, g) -> fazer(p, b, opcao, v, g)).build());
        }
        confirmarTudo(gui, p, b, opcao, false);
        gui.setSlot(18, botao(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((ix, ty, a, g) -> opcoes(p, b)).build());
        gui.open();
    }

    private static void confirmarTudo(SimpleGui gui, ServerPlayer p, Bolao b, String opcao, boolean pedindo) {
        int max = Math.min(Jogo.contar(p), Jogo.casa.config.apostaMax - b.apostadoPor(p.getUUID().toString()));
        GuiElementBuilder e = new GuiElementBuilder(pedindo ? Items.TNT : Items.DIAMOND_BLOCK)
            .setName(t(pedindo ? "Tem certeza? Clique de novo" : "All-in (" + Math.max(max, 0) + ")", pedindo ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
            .addLoreLine(t("Aposta tudo que o limite permite.", ChatFormatting.GRAY));
        e.setCallback((ix, ty, a, g) -> {
            if (!pedindo) {
                confirmarTudo(gui, p, b, opcao, true);
            } else {
                fazer(p, b, opcao, Math.max(max, 0), g);
            }
        });
        gui.setSlot(16, e.build());
    }

    private static void fazer(ServerPlayer p, Bolao b, String opcao, int v, eu.pb4.sgui.api.gui.SlotBasedGui g) {
        String erro = Jogo.apostar(p, b, opcao, v);
        if (erro != null) {
            p.sendSystemMessage(Jogo.prefixo(erro));
            return;
        }
        opcoes(p, b);
    }

    // ---------- histórico e ranking ----------

    private static void historico(ServerPlayer p) {
        Casa casa = Jogo.casa;
        String uuid = p.getUUID().toString();
        Casa.Jogador j = casa.jogadores.get(uuid);
        p.sendSystemMessage(t("== Meu histórico ==", ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (j == null || j.apostas == 0) {
            p.sendSystemMessage(t("Você ainda não apostou.", ChatFormatting.GRAY));
        } else {
            long lucro = j.ganho - j.gasto;
            p.sendSystemMessage(t(j.apostas + " aposta(s), " + j.acertos + " acerto(s). Gastou " + j.gasto + ", ganhou " + j.ganho
                + " (" + (lucro >= 0 ? "+" : "") + lucro + ").", ChatFormatting.WHITE));
        }
        for (Bolao b : casa.bolaos) {
            int v = b.apostadoPor(uuid);
            if (v <= 0) continue;
            String res = b.status == Bolao.Status.RESOLVIDO ? "ganhou " + b.vencedor : b.status.name().toLowerCase();
            p.sendSystemMessage(t("#" + b.id + " " + b.titulo + ": você pôs " + v + " (" + res + ")", ChatFormatting.GRAY));
        }
        for (Casa.Combinada c : casa.combinadasDe(uuid)) {
            p.sendSystemMessage(t("Combinada #" + c.id + " (" + c.pernas.size() + " bolões, " + c.valorPorPerna + " em cada): "
                + c.estado.toLowerCase() + (c.bonus > 0 ? ", bônus " + c.bonus : ""), ChatFormatting.LIGHT_PURPLE));
        }
        int n = casa.historico.size();
        if (n > 0) {
            p.sendSystemMessage(t("Últimos resultados da casa:", ChatFormatting.YELLOW));
            for (int i = Math.max(0, n - 5); i < n; i++) p.sendSystemMessage(t(casa.historico.get(i).texto, ChatFormatting.GRAY));
        }
    }

    public static void rankingApostadores(ServerPlayer p) {
        Casa casa = Jogo.casa;
        List<Casa.Jogador> r = casa.rankingApostadores();
        p.sendSystemMessage(t("== Ranking de apostadores (temporada " + casa.temporada + ") ==", ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (r.isEmpty()) {
            p.sendSystemMessage(t("Ninguém apostou ainda.", ChatFormatting.GRAY));
            return;
        }
        for (int i = 0; i < Math.min(10, r.size()); i++) {
            Casa.Jogador j = r.get(i);
            long lucro = j.ganho - j.gasto;
            p.sendSystemMessage(t((i + 1) + "º " + j.nome + " — " + (lucro >= 0 ? "+" : "") + lucro + ", " + j.acertos + "/" + j.apostas
                + " acertos [" + Casa.titulo(i + 1, j, r.size()) + "]", i == 0 ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        }
    }
}
