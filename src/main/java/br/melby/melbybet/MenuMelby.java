package br.melby.melbybet;

import br.melby.melbybet.core.Bolao;
import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Gradiente;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** /melby: o menu central, separado por categorias. Textos digitados no chat não vão para o chat geral. */
public final class MenuMelby {
    private MenuMelby() {}

    // ---------- peças ----------

    private static Component t(String s, ChatFormatting... f) {
        return Component.literal(s).withStyle(f);
    }

    private static GuiElementBuilder btn(Item item, String nome, ChatFormatting cor, String... lore) {
        GuiElementBuilder b = new GuiElementBuilder(item).setName(t(nome, cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        for (String l : lore) b.addLoreLine(t(l, ChatFormatting.GRAY));
        return b;
    }

    private static SimpleGui tela(ServerPlayer p, int linhas, String titulo) {
        MenuType<?> tipo = switch (linhas) {
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        SimpleGui g = new SimpleGui(tipo, p, false);
        g.setTitle(t(titulo, ChatFormatting.GOLD));
        return g;
    }

    private static void voltar(SimpleGui g, int linhas, Runnable acao) {
        g.setSlot(linhas * 9 - 9, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> acao.run()).build());
    }

    private static void info(ServerPlayer p, String s) {
        p.sendSystemMessage(Jogo.prefixo(s));
    }

    private static void perguntaNumero(ServerPlayer p, String pergunta, int min, int max, Consumer<Integer> aoDigitar, Runnable depois) {
        Chat.perguntar(p, pergunta, txt -> {
            try {
                int v = Integer.parseInt(txt.trim());
                if (v < min || v > max) {
                    info(p, "Use um número de " + min + " a " + max + ".");
                } else {
                    aoDigitar.accept(v);
                    Jogo.salvar();
                }
            } catch (NumberFormatException e) {
                info(p, "Isso não é um número.");
            }
            depois.run();
        });
    }

    // ---------- principal ----------

    public static void principal(ServerPlayer p) {
        SimpleGui g = tela(p, 3, "Melby");
        g.setSlot(10, btn(Items.GOLD_INGOT, "Apostas", ChatFormatting.GOLD, "Bolões, ranking e histórico")
            .setCallback((i, ty, a, gg) -> MenuBet.abrir(p)).build());
        g.setSlot(11, btn(Items.COMPASS, "Ranking de stats", ChatFormatting.AQUA, "Quem andou, minerou e morreu mais")
            .setCallback((i, ty, a, gg) -> MenuTop.abrir(p)).build());
        g.setSlot(12, new GuiElementBuilder(Items.PLAYER_HEAD).setProfile(p.getGameProfile().name())
            .setName(t("Meu perfil", ChatFormatting.GREEN, ChatFormatting.BOLD))
            .addLoreLine(t("Cor do nick, chat e AFK", ChatFormatting.GRAY))
            .setCallback((i, ty, a, gg) -> perfil(p)).build());
        g.setSlot(13, btn(Items.BOOK, "Ajuda", ChatFormatting.YELLOW, "Livro de boas-vindas")
            .setCallback((i, ty, a, gg) -> Tutorial.ajuda(p)).build());
        if (Comandos.staff(p)) {
            g.setSlot(15, btn(Items.SHIELD, "Moderação", ChatFormatting.RED, "Jogadores e inventários")
                .setCallback((i, ty, a, gg) -> jogadores(p)).build());
        }
        if (Comandos.admin(p)) {
            g.setSlot(16, btn(Items.COMMAND_BLOCK, "Administração", ChatFormatting.LIGHT_PURPLE, "Bolões, mensagens e configurações")
                .setCallback((i, ty, a, gg) -> admin(p)).build());
        }
        g.setSlot(22, btn(Items.BARRIER, "Fechar", ChatFormatting.RED).setCallback((i, ty, a, gg) -> gg.close()).build());
        g.open();
    }

    // ---------- perfil (todos) ----------

    private static void perfil(ServerPlayer p) {
        SimpleGui g = tela(p, 3, "Meu perfil");
        g.setSlot(10, btn(Items.BLAZE_POWDER, "Cor do nick", ChatFormatting.GOLD, "Degradê no chat")
            .setCallback((i, ty, a, gg) -> cores(p)).build());
        boolean global = Chat.estaNoGlobal(p);
        g.setSlot(12, btn(global ? Items.ENDER_EYE : Items.ENDER_PEARL, global ? "Chat: global" : "Chat: local", ChatFormatting.AQUA,
            "Clique para trocar para " + (global ? "local" : "global"))
            .setCallback((i, ty, a, gg) -> {
                if (global) Chat.travarLocal(p);
                else Chat.alternarGlobal(p);
                perfil(p);
            }).build());
        g.setSlot(16, btn(Items.TOTEM_OF_UNDYING, "Gestos", ChatFormatting.YELLOW, "Girar, sentar, acenar, apontar e rastejar")
            .setCallback((i, ty, a, gg) -> Gestos.abrirMenu(p)).build());
        g.setSlot(14, btn(Items.CLOCK, "Ficar AFK", ChatFormatting.GRAY, "Mostra [AFK] até você se mexer")
            .setCallback((i, ty, a, gg) -> {
                gg.close();
                Extras.alternarAfk(p);
            }).build());
        voltar(g, 3, () -> principal(p));
        g.open();
    }

    private static final Item[] ICONES_PRESET = {Items.BLAZE_POWDER, Items.HEART_OF_THE_SEA, Items.SLIME_BALL, Items.EMERALD,
        Items.PORKCHOP, Items.GOLD_INGOT, Items.SNOWBALL, Items.LAVA_BUCKET};

    private static void cores(ServerPlayer p) {
        SimpleGui g = tela(p, 3, "Cor do nick");
        String[] nomes = Gradiente.NOMES_PRESETS;
        for (int i = 0; i < nomes.length; i++) {
            String nome = nomes[i];
            int[] c = Gradiente.preset(nome);
            g.setSlot(9 + i, new GuiElementBuilder(ICONES_PRESET[i]).hideDefaultTooltip()
                .setName(Prefixos.degrade(nome, c[0], c[1]))
                .addLoreLine(t("Clique para usar", ChatFormatting.GRAY))
                .setCallback((ix, ty, a, gg) -> {
                    Comandos.aplicarCor(p.getGameProfile().name(), nome, null);
                    info(p, "Cor aplicada.");
                    cores(p);
                }).build());
        }
        g.setSlot(18, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> perfil(p)).build());
        g.setSlot(21, btn(Items.NAME_TAG, "Personalizada", ChatFormatting.LIGHT_PURPLE, "Digite no chat: #cor1 #cor2", "Ex.: #ff0000 #0000ff")
            .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Digite uma ou duas cores, como #ff8a24 #ffd23f.", txt -> {
                String[] t = txt.trim().split("\\s+");
                String erro = Comandos.aplicarCor(p.getGameProfile().name(), t[0], t.length > 1 ? t[1] : null);
                info(p, erro == null ? "Cor aplicada." : erro);
                cores(p);
            })).build());
        g.setSlot(23, btn(Items.BARRIER, "Tirar a cor", ChatFormatting.RED)
            .setCallback((i, ty, a, gg) -> {
                Jogo.casa.config.cores.remove(p.getGameProfile().name().toLowerCase());
                Jogo.salvar();
                cores(p);
            }).build());
        g.open();
    }

    // ---------- moderação: jogadores ----------

    private static void jogadores(ServerPlayer p) {
        SimpleGui g = tela(p, 6, "Jogadores");
        int slot = 0;
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) {
            if (slot >= 45) break;
            String nick = o.getGameProfile().name();
            g.setSlot(slot++, new GuiElementBuilder(Items.PLAYER_HEAD).setProfile(nick)
                .setName(t(nick, ChatFormatting.WHITE, ChatFormatting.BOLD))
                .addLoreLine(t("Online. Clique para opções", ChatFormatting.GRAY))
                .setCallback((i, ty, a, gg) -> jogador(p, nick)).build());
        }
        g.setSlot(49, btn(Items.NAME_TAG, "Outro jogador (offline)", ChatFormatting.YELLOW, "Digite o nick no chat")
            .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Digite o nick do jogador.", txt -> jogador(p, txt.trim()))).build());
        voltar(g, 6, () -> principal(p));
        g.open();
    }

    private static void jogador(ServerPlayer p, String nick) {
        boolean admin = Comandos.admin(p);
        ServerPlayer on = Jogo.server.getPlayerList().getPlayerByName(nick);
        SimpleGui g = tela(p, 4, nick + (on != null ? " (online)" : " (offline)"));
        g.setSlot(10, btn(Items.CHEST, "Ver inventário", ChatFormatting.AQUA)
            .setCallback((i, ty, a, gg) -> {
                String e = Comandos.verPorNome(p, nick, false);
                if (e != null) info(p, e);
            }).build());
        g.setSlot(11, btn(Items.ENDER_CHEST, "Ver ender chest", ChatFormatting.DARK_PURPLE)
            .setCallback((i, ty, a, gg) -> {
                String e = Comandos.verPorNome(p, nick, true);
                if (e != null) info(p, e);
            }).build());
        g.setSlot(13, btn(Items.PAPER, "Mensagens privadas", ChatFormatting.LIGHT_PURPLE, "As últimas 15, no chat")
            .setCallback((i, ty, a, gg) -> {
                var l = Chat.historico(nick);
                if (l.isEmpty()) info(p, "Nenhuma mensagem privada de " + nick + ".");
                for (Casa.Tell x : l) p.sendSystemMessage(t(x.de + " -> " + x.para + ": " + x.texto, ChatFormatting.GRAY));
            }).build());
        g.setSlot(14, btn(Items.SPYGLASS, "Visto por último", ChatFormatting.GRAY)
            .setCallback((i, ty, a, gg) -> {
                Casa.Visto v = Jogo.casa.vistos.get(nick.toLowerCase());
                if (v == null) info(p, "Nunca vi " + nick + ".");
                else if (v.online) info(p, nick + " está online há " + br.melby.melbybet.core.Fmt.duracao(System.currentTimeMillis() - v.entrou) + ".");
                else info(p, nick + " saiu há " + br.melby.melbybet.core.Fmt.duracao(System.currentTimeMillis() - v.saiu)
                    + ". Posição: " + v.dimensao + " " + v.x + " " + v.y + " " + v.z);
            }).build());
        if (admin) {
            g.setSlot(19, btn(Items.NAME_TAG, "Dar título", ChatFormatting.GOLD, "Digite: [#cor1 #cor2] texto")
                .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Digite o título (até 20 letras). Pode começar com duas cores: #ff8a24 #ffd23f Lenda.", txt -> {
                    String e = Comandos.aplicarTitulo(nick, txt);
                    info(p, e == null ? "Título aplicado." : e);
                    jogador(p, nick);
                })).build());
            g.setSlot(20, btn(Items.BARRIER, "Tirar título", ChatFormatting.RED)
                .setCallback((i, ty, a, gg) -> {
                    Jogo.casa.config.titulos.remove(nick.toLowerCase());
                    Jogo.salvar();
                    Prefixos.esquecer();
                    info(p, "Título removido.");
                }).build());
            g.setSlot(21, btn(Items.BLAZE_POWDER, "Cor do nick", ChatFormatting.GOLD, "Digite: #cor1 #cor2 ou um nome pronto")
                .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Digite as cores (#ff0000 #0000ff) ou um nome: " + String.join(", ", Gradiente.NOMES_PRESETS) + ".", txt -> {
                    String[] x = txt.trim().split("\\s+");
                    String e = Comandos.aplicarCor(nick, x[0], x.length > 1 ? x[1] : null);
                    info(p, e == null ? "Cor aplicada." : e);
                    jogador(p, nick);
                })).build());
            boolean ehMod = Jogo.casa.config.moderadores.stream().anyMatch(n -> n.equalsIgnoreCase(nick));
            g.setSlot(22, btn(Items.SHIELD, ehMod ? "Tirar de moderador" : "Tornar moderador", ChatFormatting.RED)
                .setCallback((i, ty, a, gg) -> {
                    if (ehMod) Jogo.casa.config.moderadores.removeIf(n -> n.equalsIgnoreCase(nick));
                    else Jogo.casa.config.moderadores.add(nick);
                    Jogo.salvar();
                    jogador(p, nick);
                }).build());
            g.setSlot(25, btn(Items.LEATHER_BOOTS, "Expulsar (kick)", ChatFormatting.GOLD, "Digite o motivo no chat")
                .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Motivo do kick de " + nick + " (ou escreva - para sem motivo).", txt -> {
                    comandoDoServidor("kick " + nick + (txt.equals("-") ? "" : " " + txt));
                    info(p, nick + " expulso.");
                })).build());
            g.setSlot(26, btn(Items.TNT, "Banir", ChatFormatting.DARK_RED, "Digite o motivo no chat", "Pede confirmação")
                .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Motivo do BAN de " + nick + ". Escreva o motivo para banir ou cancelar.", motivo ->
                    Chat.perguntar(p, "Confirmar ban de " + nick + " por \"" + motivo + "\"? Escreva sim para confirmar.", conf -> {
                        if (conf.equalsIgnoreCase("sim")) {
                            comandoDoServidor("ban " + nick + " " + motivo);
                            info(p, nick + " banido.");
                        } else {
                            info(p, "Ban cancelado.");
                        }
                    }))).build());
            g.setSlot(34, btn(Items.TOTEM_OF_UNDYING, "Desbanir", ChatFormatting.GREEN)
                .setCallback((i, ty, a, gg) -> {
                    comandoDoServidor("pardon " + nick);
                    info(p, nick + " desbanido (se estava banido).");
                }).build());
        }
        voltar(g, 4, () -> jogadores(p));
        g.open();
    }

    private static void comandoDoServidor(String comando) {
        Jogo.server.getCommands().performPrefixedCommand(Jogo.server.createCommandSourceStack(), comando);
    }

    // ---------- administração ----------

    private static void admin(ServerPlayer p) {
        SimpleGui g = tela(p, 3, "Administração");
        g.setSlot(10, btn(Items.WRITABLE_BOOK, "Bolões", ChatFormatting.GOLD, "Criar, abrir, fechar e resolver")
            .setCallback((i, ty, a, gg) -> boloes(p)).build());
        g.setSlot(12, btn(Items.PAPER, "Mensagens de entrar e sair", ChatFormatting.GREEN, "Frases sorteadas")
            .setCallback((i, ty, a, gg) -> mensagens(p)).build());
        g.setSlot(14, btn(Items.REDSTONE, "Configurações", ChatFormatting.AQUA, "Chat, AFK, apostas e Discord")
            .setCallback((i, ty, a, gg) -> config(p)).build());
        g.setSlot(16, btn(Items.SHIELD, "Jogadores", ChatFormatting.RED, "Inventário, título, kick e ban")
            .setCallback((i, ty, a, gg) -> jogadores(p)).build());
        g.setSlot(22, btn(Items.CLOCK, "Reinício do servidor", ChatFormatting.RED, "Horários e avisos automáticos")
            .setCallback((i, ty, a, gg) -> reinicio(p)).build());
        voltar(g, 3, () -> principal(p));
        g.open();
    }

    private static void reinicio(ServerPlayer p) {
        Casa.Config c = Jogo.casa.config;
        SimpleGui g = tela(p, 4, "Reinício do servidor");
        for (int i = 0; i < c.reinicios.size() && i < 18; i++) {
            String h = c.reinicios.get(i);
            g.setSlot(i, btn(Items.CLOCK, h, ChatFormatting.WHITE, "Clique para remover", "Fuso: " + c.fuso)
                .setCallback((ix, ty, a, gg) -> {
                    c.reinicios.remove(h);
                    Jogo.salvar();
                    reinicio(p);
                }).build());
        }
        g.setSlot(29, btn(Items.EMERALD, "Adicionar horário", ChatFormatting.GREEN, "Digite no formato HH:mm")
            .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Digite o horário do reinício, como 04:30.", txt -> {
                String h = br.melby.melbybet.core.Reinicio.normalizar(txt);
                if (h == null) info(p, "Use o formato HH:mm, como 04:30.");
                else {
                    if (!c.reinicios.contains(h)) c.reinicios.add(h);
                    Jogo.salvar();
                    info(p, "Reinício às " + h + " adicionado. O servidor avisa 30, 15, 10, 5, 3, 2 e 1 minuto antes.");
                }
                reinicio(p);
            })).build());
        g.setSlot(31, btn(Items.TNT, Reinicios.manualAtivo() ? "Cancelar contagem" : "Reiniciar agora (contagem)", ChatFormatting.RED,
            Reinicios.manualAtivo() ? "Cancela o reinício marcado" : "Digite em quantos minutos; o servidor salva e desliga")
            .setCallback((i, ty, a, gg) -> {
                if (Reinicios.manualAtivo()) {
                    Reinicios.cancelarManual();
                    reinicio(p);
                } else {
                    perguntaNumero(p, "Reiniciar em quantos minutos? (1 a 120)", 1, 120, Reinicios::iniciarManual, () -> reinicio(p));
                }
            }).build());
        voltar(g, 4, () -> admin(p));
        g.open();
    }

    // ---------- administração: bolões ----------

    private static void boloes(ServerPlayer p) {
        SimpleGui g = tela(p, 6, "Bolões");
        int slot = 0;
        for (int i = Jogo.casa.bolaos.size() - 1; i >= 0 && slot < 45; i--) {
            Bolao b = Jogo.casa.bolaos.get(i);
            g.setSlot(slot++, btn(b.emAndamento() ? Items.WRITABLE_BOOK : Items.BOOK, "#" + b.id + " " + b.titulo, ChatFormatting.GOLD,
                "Estado: " + b.status, "Resolve: " + b.gatilho.rotulo, "Pote: " + b.pote())
                .setCallback((ix, ty, a, gg) -> bolao(p, b)).build());
        }
        g.setSlot(49, btn(Items.EMERALD, "Criar bolão", ChatFormatting.GREEN, "Escolha o tipo e digite o título")
            .setCallback((i, ty, a, gg) -> escolherGatilho(p)).build());
        voltar(g, 6, () -> admin(p));
        g.open();
    }

    private static void escolherGatilho(ServerPlayer p) {
        SimpleGui g = tela(p, 3, "Como o bolão se resolve?");
        int slot = 9;
        for (Bolao.Gatilho gat : Bolao.Gatilho.values()) {
            g.setSlot(slot++, btn(gat == Bolao.Gatilho.MANUAL ? Items.WRITABLE_BOOK : Items.NETHER_STAR, gat.rotulo, ChatFormatting.YELLOW,
                gat == Bolao.Gatilho.MANUAL ? "Você declara o vencedor" : "O servidor resolve sozinho")
                .setCallback((i, ty, a, gg) -> criar(p, gat)).build());
        }
        voltar(g, 3, () -> boloes(p));
        g.open();
    }

    private static void criar(ServerPlayer p, Bolao.Gatilho gat) {
        Chat.perguntar(p, "Digite o título do bolão.", titulo -> {
            if (gat == Bolao.Gatilho.MANUAL) {
                Chat.perguntar(p, "Digite as opções separadas por ponto e vírgula. Ex.: Davi;Kaue;Edu", txt -> {
                    List<String> ops = new ArrayList<>();
                    for (String o : txt.split(";")) if (!o.isBlank()) ops.add(o.trim());
                    if (ops.size() < 2) {
                        info(p, "Precisa de pelo menos 2 opções.");
                    } else {
                        Bolao b = Jogo.casa.criar(titulo, gat, ops, System.currentTimeMillis());
                        Jogo.salvar();
                        info(p, "Bolão #" + b.id + " criado (fechado).");
                    }
                    boloes(p);
                });
            } else {
                Bolao b = Jogo.casa.criar(titulo, gat, new ArrayList<>(Jogo.casa.config.roster), System.currentTimeMillis());
                Jogo.salvar();
                info(p, "Bolão #" + b.id + " criado (fechado), com os nicks do roster.");
                boloes(p);
            }
        });
    }

    private static void bolao(ServerPlayer p, Bolao b) {
        SimpleGui g = tela(p, 3, "#" + b.id + " " + b.titulo);
        g.setSlot(4, btn(Items.BOOK, "Estado: " + b.status, ChatFormatting.WHITE, "Pote: " + b.pote(), "Resolve: " + b.gatilho.rotulo).build());
        if (b.status == Bolao.Status.FECHADO) {
            g.setSlot(10, btn(Items.EMERALD, "Abrir apostas", ChatFormatting.GREEN)
                .setCallback((i, ty, a, gg) -> {
                    String e = Jogo.casa.abrir(b);
                    if (e != null) {
                        info(p, e);
                    } else {
                        Jogo.avisar("Novo bolão aberto: \"" + b.titulo + "\". Use /bet para apostar!");
                        Jogo.webhook("🎲 **Novo bolão aberto!** #" + b.id + " — " + b.titulo + "\nOpções: " + String.join(", ", b.opcoes));
                        Jogo.salvar();
                    }
                    bolao(p, b);
                }).build());
        }
        if (b.status == Bolao.Status.ABERTO) {
            g.setSlot(10, btn(Items.REDSTONE, "Fechar apostas", ChatFormatting.RED)
                .setCallback((i, ty, a, gg) -> {
                    Jogo.casa.fechar(b);
                    Jogo.avisar("Bolão \"" + b.titulo + "\" fechado para apostas.");
                    Jogo.salvar();
                    bolao(p, b);
                }).build());
            g.setSlot(12, btn(Items.CLOCK, "Prazo para fechar", ChatFormatting.AQUA, "Digite os minutos no chat")
                .setCallback((i, ty, a, gg) -> perguntaNumero(p, "Fechar sozinho em quantos minutos? (1 a 1440)", 1, 1440,
                    min -> b.expiraEm = System.currentTimeMillis() + min * 60_000L, () -> bolao(p, b))).build());
        }
        if (b.emAndamento()) {
            g.setSlot(14, btn(Items.NETHER_STAR, "Declarar vencedor", ChatFormatting.GOLD, "Escolhe a opção e paga")
                .setCallback((i, ty, a, gg) -> vencedor(p, b)).build());
            g.setSlot(16, btn(Items.TNT, "Cancelar", ChatFormatting.DARK_RED, "Devolve as apostas", "Pede confirmação")
                .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Cancelar o bolão #" + b.id + "? Escreva sim para confirmar.", conf -> {
                    if (conf.equalsIgnoreCase("sim")) {
                        Jogo.casa.cancelar(b, System.currentTimeMillis());
                        Jogo.entregarTodos();
                        Jogo.avisar("Bolão \"" + b.titulo + "\" cancelado. Apostas devolvidas.");
                        Jogo.salvar();
                    }
                    bolao(p, b);
                })).build());
        }
        voltar(g, 3, () -> boloes(p));
        g.open();
    }

    private static void vencedor(ServerPlayer p, Bolao b) {
        SimpleGui g = tela(p, 4, "Quem ganhou? #" + b.id);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < b.opcoes.size() && i < slots.length; i++) {
            String op = b.opcoes.get(i);
            boolean cabeca = Jogo.casa.config.roster.stream().anyMatch(r -> r.equalsIgnoreCase(op));
            GuiElementBuilder e = btn(cabeca ? Items.PLAYER_HEAD : Items.PAPER, op, ChatFormatting.WHITE, "Clique para declarar vencedor");
            if (cabeca) e.setProfile(op);
            e.setCallback((ix, ty, a, gg) -> Chat.perguntar(p, "Declarar \"" + op + "\" vencedor do bolão #" + b.id + "? Escreva sim para confirmar.", conf -> {
                if (conf.equalsIgnoreCase("sim") && b.emAndamento()) Jogo.resolver(b, op);
                bolao(p, b);
            }));
            g.setSlot(slots[i], e.build());
        }
        voltar(g, 4, () -> bolao(p, b));
        g.open();
    }

    // ---------- administração: mensagens ----------

    private static void mensagens(ServerPlayer p) {
        SimpleGui g = tela(p, 3, "Mensagens de entrar e sair");
        g.setSlot(11, btn(Items.EMERALD, "Mensagens de entrar", ChatFormatting.GREEN, Jogo.casa.config.msgEntrar.size() + " frases")
            .setCallback((i, ty, a, gg) -> listaFrases(p, true)).build());
        g.setSlot(15, btn(Items.REDSTONE, "Mensagens de sair", ChatFormatting.RED, Jogo.casa.config.msgSair.size() + " frases")
            .setCallback((i, ty, a, gg) -> listaFrases(p, false)).build());
        voltar(g, 3, () -> admin(p));
        g.open();
    }

    private static void listaFrases(ServerPlayer p, boolean entrar) {
        List<String> lista = entrar ? Jogo.casa.config.msgEntrar : Jogo.casa.config.msgSair;
        SimpleGui g = tela(p, 6, entrar ? "Frases de entrar" : "Frases de sair");
        for (int i = 0; i < lista.size() && i < 45; i++) {
            String frase = lista.get(i);
            g.setSlot(i, btn(Items.PAPER, frase, ChatFormatting.WHITE, "Clique para remover")
                .setCallback((ix, ty, a, gg) -> {
                    lista.remove(frase);
                    Jogo.salvar();
                    listaFrases(p, entrar);
                }).build());
        }
        g.setSlot(49, btn(Items.EMERALD, "Adicionar frase", ChatFormatting.GREEN, "Use {nick} onde vai o nome")
            .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Digite a frase. Use {nick} onde vai o nome. Ex.: {nick} chegou!", txt -> {
                if (!txt.contains("{nick}")) info(p, "Faltou o {nick}.");
                else {
                    lista.add(txt);
                    Jogo.salvar();
                }
                listaFrases(p, entrar);
            })).build());
        voltar(g, 6, () -> mensagens(p));
        g.open();
    }

    // ---------- administração: configurações ----------

    private static void config(ServerPlayer p) {
        Casa.Config c = Jogo.casa.config;
        SimpleGui g = tela(p, 4, "Configurações");
        g.setSlot(10, btn(Items.COMPASS, "Alcance do chat local", ChatFormatting.AQUA, "Agora: " + (c.chatLocalRaio == 0 ? "sem limite" : c.chatLocalRaio + " blocos"))
            .setCallback((i, ty, a, gg) -> perguntaNumero(p, "Alcance do chat local em blocos (0 = sem limite).", 0, 10000, v -> c.chatLocalRaio = v, () -> config(p))).build());
        g.setSlot(11, btn(Items.CLOCK, "AFK automático", ChatFormatting.GRAY, "Agora: " + (c.afkAutoMinutos == 0 ? "desligado" : c.afkAutoMinutos + " min"))
            .setCallback((i, ty, a, gg) -> perguntaNumero(p, "Minutos parado até ficar AFK (0 = desligado).", 0, 120, v -> c.afkAutoMinutos = v, () -> config(p))).build());
        g.setSlot(12, btn(Items.GOLD_INGOT, "Parte da casa", ChatFormatting.GOLD, "Agora: " + c.rakePct + "%")
            .setCallback((i, ty, a, gg) -> perguntaNumero(p, "Porcentagem do pote que a casa fica (0 a 50).", 0, 50, v -> c.rakePct = v, () -> config(p))).build());
        g.setSlot(13, btn(Items.DIAMOND, "Limite por bolão", ChatFormatting.AQUA, "Agora: " + c.apostaMax + " diamantes")
            .setCallback((i, ty, a, gg) -> perguntaNumero(p, "Máximo de diamantes por jogador em cada bolão.", 1, 2000, v -> c.apostaMax = v, () -> config(p))).build());
        g.setSlot(14, btn(Items.BELL, "Aviso de aposta grande", ChatFormatting.YELLOW, "Agora: a partir de " + c.avisoGrande)
            .setCallback((i, ty, a, gg) -> perguntaNumero(p, "A partir de quantos diamantes a aposta vira aviso no chat.", 1, 2000, v -> c.avisoGrande = v, () -> config(p))).build());
        g.setSlot(15, btn(Items.REDSTONE, "Taxa de desistir", ChatFormatting.RED, "Agora: " + c.cashoutTaxaPct + "%")
            .setCallback((i, ty, a, gg) -> perguntaNumero(p, "Taxa de quem desiste da aposta (0 a 100).", 0, 100, v -> c.cashoutTaxaPct = v, () -> config(p))).build());
        boolean temHook = c.discordWebhook != null && !c.discordWebhook.isBlank();
        g.setSlot(19, btn(Items.NAME_TAG, "Webhook do Discord", ChatFormatting.LIGHT_PURPLE, temHook ? "Ligado" : "Desligado", "Clique para colar a URL")
            .setCallback((i, ty, a, gg) -> Chat.perguntar(p, "Cole a URL do webhook (começa com https://).", txt -> {
                if (!txt.startsWith("https://")) info(p, "A URL precisa começar com https://");
                else {
                    c.discordWebhook = txt;
                    Jogo.salvar();
                    info(p, "Webhook salvo.");
                }
                config(p);
            })).build());
        if (temHook) {
            g.setSlot(20, btn(Items.PAPER, "Testar webhook", ChatFormatting.GREEN)
                .setCallback((i, ty, a, gg) -> {
                    Jogo.webhook("✅ MelbyBet conectado ao Discord!");
                    info(p, "Mensagem de teste enviada.");
                }).build());
            g.setSlot(21, btn(Items.BARRIER, "Desligar webhook", ChatFormatting.RED)
                .setCallback((i, ty, a, gg) -> {
                    c.discordWebhook = "";
                    Jogo.salvar();
                    config(p);
                }).build());
        }
        voltar(g, 4, () -> admin(p));
        g.open();
    }
}
