package br.melby.melbybet;

import br.melby.melbybet.core.Bolao;
import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Gradiente;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class Comandos {
    private Comandos() {}

    private static final PermissionNode<Boolean> ADMIN = PermissionNode.of("melbybet", "admin");

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("bet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem abrir o menu.");
            Tutorial.primeiraVez(p);
            MenuBet.abrir(p);
            return 1;
        }).then(literal("ajuda").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem ler o livro.");
            Tutorial.ajuda(p);
            return 1;
        })));
        d.register(literal("afk").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem usar.");
            Extras.alternarAfk(p);
            return 1;
        }));
        d.register(literal("seen").then(argument("nick", word()).executes(c -> {
            String nick = StringArgumentType.getString(c, "nick");
            Casa.Visto v = Jogo.casa.vistos.get(nick.toLowerCase());
            if (v == null) return erro(c, "Nunca vi " + nick + " por aqui.");
            long agora = System.currentTimeMillis();
            if (v.online) return ok(c, v.nome + " está online há " + br.melby.melbybet.core.Fmt.duracao(agora - v.entrou) + ".");
            String msg = v.nome + " foi visto há " + br.melby.melbybet.core.Fmt.duracao(agora - v.saiu) + ".";
            if (adminOuMod("seen").test(c.getSource())) msg += " Última posição: " + v.dimensao + " " + v.x + " " + v.y + " " + v.z + ".";
            return ok(c, msg);
        })));
        d.register(literal("topbet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem usar.");
            MenuBet.rankingApostadores(p);
            return 1;
        }));
        d.register(literal("top").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem abrir o menu.");
            MenuTop.abrir(p);
            return 1;
        }));

        d.register(literal("melby").executes(c -> {
            ServerPlayer p = c.getSource().getPlayer();
            if (p == null) return erro(c, "Só jogadores podem abrir o menu.");
            Chat.cancelarPergunta(p);
            MenuMelby.principal(p);
            return 1;
        }));
        extras(d);
        Coords.registrar(d);
        Capsulas.registrar(d);
        Gestos.registrar(d);
        Duelos.registrar(d);
        Perfil.registrar(d);
        Limpeza.registrar(d);
        chatEMais(d);

        LiteralArgumentBuilder<CommandSourceStack> adm = literal("melbybet")
            .requires(PermissionPredicates.require(ADMIN, PermissionLevel.ADMINS));

        adm.then(literal("criar")
            .then(argument("gatilho", word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Bolao.Gatilho.values()).map(Enum::name).toList(), b))
                .then(argument("titulo", greedyString()).executes(Comandos::criar))));

        adm.then(literal("abrir").then(argument("id", integer(1)).executes(c -> mudar(c, "abrir"))));
        adm.then(literal("fechar").then(argument("id", integer(1)).executes(c -> mudar(c, "fechar"))));
        adm.then(literal("cancelar").then(argument("id", integer(1)).executes(c -> mudar(c, "cancelar"))));
        adm.then(literal("resolver").then(argument("id", integer(1))
            .then(argument("vencedor", greedyString()).executes(Comandos::resolver))));
        adm.then(literal("prazo").then(argument("id", integer(1))
            .then(argument("minutos", integer(1, 1440)).executes(Comandos::prazo))));
        adm.then(literal("webhook")
            .then(literal("off").executes(c -> {
                Jogo.casa.config.discordWebhook = "";
                Jogo.salvar();
                return ok(c, "Webhook do Discord desligado.");
            }))
            .then(literal("teste").executes(c -> {
                if (Jogo.casa.config.discordWebhook.isBlank()) return erro(c, "Nenhum webhook configurado.");
                Jogo.webhook("✅ MelbyBet conectado ao Discord!");
                return ok(c, "Mensagem de teste enviada.");
            }))
            .then(argument("url", greedyString()).executes(c -> {
                String url = StringArgumentType.getString(c, "url").trim();
                if (!url.startsWith("https://")) return erro(c, "A URL do webhook começa com https://");
                Jogo.casa.config.discordWebhook = url;
                Jogo.salvar();
                return ok(c, "Webhook salvo. Teste com /melbybet webhook teste");
            })));
        adm.then(literal("mod")
            .then(literal("add").then(argument("nick", word()).executes(c -> {
                String nick = StringArgumentType.getString(c, "nick");
                List<String> l = Jogo.casa.config.moderadores;
                if (l.stream().anyMatch(n -> n.equalsIgnoreCase(nick))) return erro(c, nick + " já é moderador.");
                l.add(nick);
                Jogo.salvar();
                return ok(c, nick + " agora é moderador (/inv, /ec, /tells e /seen).");
            })))
            .then(literal("remove").then(argument("nick", word()).executes(c -> {
                String nick = StringArgumentType.getString(c, "nick");
                boolean tirou = Jogo.casa.config.moderadores.removeIf(n -> n.equalsIgnoreCase(nick));
                Jogo.salvar();
                return tirou ? ok(c, nick + " não é mais moderador.") : erro(c, nick + " não está na lista.");
            })))
            .then(literal("lista").executes(c -> ok(c, "Moderadores: " + (Jogo.casa.config.moderadores.isEmpty() ? "nenhum" : String.join(", ", Jogo.casa.config.moderadores))))));
        adm.then(literal("afk").then(argument("minutos", integer(0, 120)).executes(c -> {
            int m = IntegerArgumentType.getInteger(c, "minutos");
            Jogo.casa.config.afkAutoMinutos = m;
            Jogo.salvar();
            return ok(c, m == 0 ? "AFK automático desligado (só pelo /afk)." : "Fica AFK sozinho depois de " + m + " min parado.");
        })));
        adm.then(literal("titulo").then(argument("nick", word())
            .then(literal("off").executes(c -> {
                String nick = StringArgumentType.getString(c, "nick");
                Jogo.casa.config.titulos.remove(nick.toLowerCase());
                Jogo.salvar();
                Prefixos.esquecer();
                return ok(c, "Título de " + nick + " removido.");
            }))
            .then(argument("texto", greedyString()).executes(c -> {
                String nick = StringArgumentType.getString(c, "nick");
                String erro = aplicarTitulo(nick, StringArgumentType.getString(c, "texto"));
                return erro == null ? ok(c, "Título de " + nick + " atualizado.") : erro(c, erro);
            }))));
        listaMsgs(adm, "entrar", true);
        listaMsgs(adm, "sair", false);
        adm.then(literal("cor").then(argument("nick", word()).then(argument("cor1", word())
            .executes(c -> corDeOutro(c, null))
            .then(argument("cor2", word()).executes(c -> corDeOutro(c, StringArgumentType.getString(c, "cor2")))))));
        adm.then(literal("raio").then(argument("blocos", integer(0, 10000)).executes(c -> {
            int r = IntegerArgumentType.getInteger(c, "blocos");
            Jogo.casa.config.chatLocalRaio = r;
            Jogo.salvar();
            return ok(c, r == 0 ? "Chat local sem limite de distância." : "Chat local alcança " + r + " blocos.");
        })));
        adm.then(literal("sit").then(argument("altura", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.0, 3.0)).executes(c -> {
            double h = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "altura");
            Jogo.casa.config.bancoAltura = h;
            Jogo.salvar();
            return ok(c, "Altura do /sit: " + h + " (maior senta mais baixo). Levante e sente de novo para ver.");
        })));
        adm.then(literal("reinicio")
            .then(literal("add").then(argument("hora", word()).executes(c -> {
                String h = br.melby.melbybet.core.Reinicio.normalizar(StringArgumentType.getString(c, "hora"));
                if (h == null) return erro(c, "Use o formato HH:mm, por exemplo 04:30.");
                if (!Jogo.casa.config.reinicios.contains(h)) Jogo.casa.config.reinicios.add(h);
                Jogo.salvar();
                return ok(c, "Reinício às " + h + " (fuso " + Jogo.casa.config.fuso + "). Avisos automáticos ligados.");
            })))
            .then(literal("remover").then(argument("hora", word()).executes(c -> {
                String h = br.melby.melbybet.core.Reinicio.normalizar(StringArgumentType.getString(c, "hora"));
                if (h == null || !Jogo.casa.config.reinicios.remove(h)) return erro(c, "Esse horário não está na lista.");
                Jogo.salvar();
                return ok(c, "Horário " + h + " removido.");
            })))
            .then(literal("lista").executes(c -> ok(c, Jogo.casa.config.reinicios.isEmpty()
                ? "Nenhum horário de reinício. Use /melbybet reinicio add 04:30."
                : "Reinícios: " + String.join(", ", Jogo.casa.config.reinicios) + " (fuso " + Jogo.casa.config.fuso + ")"))));
        adm.then(literal("reiniciar")
            .then(literal("cancelar").executes(c -> {
                if (!Reinicios.manualAtivo()) return erro(c, "Não há reinício em contagem.");
                Reinicios.cancelarManual();
                return 1;
            }))
            .then(argument("minutos", integer(1, 120)).executes(c -> {
                Reinicios.iniciarManual(IntegerArgumentType.getInteger(c, "minutos"));
                return ok(c, "Contagem iniciada. O servidor salva e desliga no fim; /melbybet reiniciar cancelar desiste.");
            })));
        adm.then(literal("lista").executes(Comandos::lista));
        adm.then(literal("cofre").executes(c -> {
            Casa casa = Jogo.casa;
            return ok(c, "Cofre da Casa: " + casa.cofre + " | Jackpot: " + casa.jackpot + " | Temporada " + casa.temporada);
        }));
        adm.then(literal("roster").then(argument("nicks", greedyString()).executes(c -> {
            List<String> n = new ArrayList<>();
            for (String s : StringArgumentType.getString(c, "nicks").split("[;,\\s]+")) if (!s.isBlank()) n.add(s.trim());
            if (n.isEmpty()) return erro(c, "Informe os nicks separados por espaço, vírgula ou ponto e vírgula.");
            Jogo.casa.config.roster = n;
            Jogo.salvar();
            return ok(c, "Roster: " + String.join(", ", n));
        })));
        adm.then(literal("temporada").then(literal("nova").executes(c -> {
            Jogo.casa.novaTemporada(System.currentTimeMillis());
            Jogo.salvar();
            Jogo.avisar("Começou a temporada " + Jogo.casa.temporada + "! O ranking de apostadores foi zerado.");
            return 1;
        })));
        d.register(adm);
    }

    private static boolean ehMod(CommandSourceStack s) {
        ServerPlayer p = s.getPlayer();
        if (p == null) return false;
        String nome = p.getGameProfile().name();
        return Jogo.casa.config.moderadores.stream().anyMatch(n -> n.equalsIgnoreCase(nome));
    }

    /** Admin (OP) ou moderador da lista do mod. */
    private static Predicate<CommandSourceStack> adminOuMod(String no) {
        Predicate<CommandSourceStack> admin = PermissionPredicates.require(PermissionNode.of("melbybet", no), PermissionLevel.ADMINS);
        return s -> admin.test(s) || ehMod(s);
    }

    // ---------- inventário, ender chest, vanish ----------

    private static void extras(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("inv")
            .requires(adminOuMod("inv"))
            .then(argument("alvo", word()).suggests(Comandos::nomes).executes(c -> ver(c, false))));
        d.register(literal("ec")
            .requires(adminOuMod("ec"))
            .then(argument("alvo", word()).suggests(Comandos::nomes).executes(c -> ver(c, true))));
        // aberto a todos: é o clique em [Inventário de fulano] no chat
        d.register(literal("mbver").then(argument("id", integer(1)).executes(c -> {
            ServerPlayer quem = c.getSource().getPlayerOrException();
            if (!Extras.verFoto(quem, IntegerArgumentType.getInteger(c, "id"))) return erro(c, "Essa foto de inventário expirou.");
            return 1;
        })));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> nomes(
            CommandContext<CommandSourceStack> c, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(Jogo.casa.conhecidos.values(), b);
    }

    private static String uuidDe(String nick) {
        for (var e : Jogo.casa.conhecidos.entrySet()) if (e.getValue().equalsIgnoreCase(nick)) return e.getKey();
        return null;
    }

    /** /inv e /ec: jogador online (foto na hora) ou offline (lê o arquivo dele). */
    private static int ver(CommandContext<CommandSourceStack> c, boolean ender) throws CommandSyntaxException {
        ServerPlayer quem = c.getSource().getPlayerOrException();
        String erro = verPorNome(quem, StringArgumentType.getString(c, "alvo"), ender);
        return erro == null ? 1 : erro(c, erro);
    }

    /** Abre inventário ou ender chest de um jogador. Devolve o motivo do erro, ou null. */
    public static String verPorNome(ServerPlayer quem, String nick, boolean ender) {
        ServerPlayer on = Jogo.server.getPlayerList().getPlayerByName(nick);
        if (on != null) {
            if (ender) Vista.enderAoVivo(quem, on);
            else Vista.inventarioAoVivo(quem, on);
            return null;
        }
        String uuid = uuidDe(nick);
        if (uuid == null) return "Não conheço " + nick + " (ele precisa já ter entrado no servidor com o mod).";
        List<ItemStack> itens = ender ? Offline.ender(uuid) : Offline.inventario(uuid);
        if (itens == null) return "Não achei o arquivo de " + nick + ".";
        Vista.foto(quem, Jogo.casa.conhecidos.get(uuid), ender, itens);
        quem.sendSystemMessage(Jogo.prefixo(nick + " está offline: isto é o que estava com ele ao sair."));
        return null;
    }

    public static boolean admin(ServerPlayer p) {
        Predicate<CommandSourceStack> pr = PermissionPredicates.require(ADMIN, PermissionLevel.ADMINS);
        return pr.test(p.createCommandSourceStack());
    }

    /** Admin (OP) ou moderador. */
    public static boolean staff(ServerPlayer p) {
        return admin(p) || ehMod(p.createCommandSourceStack());
    }

    /** Dá um título: "[#cor1 #cor2] texto". Devolve o motivo do erro, ou null. */
    public static String aplicarTitulo(String nick, String bruto) {
        String[] t = bruto.trim().split("\\s+", 3);
        Casa.Titulo ti = new Casa.Titulo();
        int c1 = t.length >= 3 ? Gradiente.hex(t[0]) : -1;
        int c2 = t.length >= 3 ? Gradiente.hex(t[1]) : -1;
        if (c1 >= 0 && c2 >= 0) {
            ti.cor1 = c1;
            ti.cor2 = c2;
            ti.texto = t[2];
        } else {
            ti.texto = bruto.trim();
        }
        if (ti.texto.isBlank()) return "Título vazio.";
        if (ti.texto.length() > 20) return "O título pode ter até 20 letras.";
        Jogo.casa.config.titulos.put(nick.toLowerCase(), ti);
        Jogo.salvar();
        Prefixos.esquecer();
        return null;
    }

    /** Aplica uma cor de nick. Devolve o motivo do erro, ou null se deu certo. */
    public static String aplicarCor(String nick, String a, String b) {
        int[] preset = Gradiente.preset(a);
        int c1, c2;
        if (preset != null) {
            c1 = preset[0];
            c2 = preset[1];
        } else {
            c1 = Gradiente.hex(a);
            c2 = b == null ? c1 : Gradiente.hex(b);
            if (c1 < 0 || c2 < 0) return "Use cores como #ff8a24, uma ou duas, ou um nome pronto: " + String.join(", ", Gradiente.NOMES_PRESETS);
        }
        Casa.CorNick cn = new Casa.CorNick();
        cn.cor1 = c1;
        cn.cor2 = c2;
        Jogo.casa.config.cores.put(nick.toLowerCase(), cn);
        Jogo.salvar();
        return null;
    }

    private static void chatEMais(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("g")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                boolean on = Chat.alternarGlobal(p);
                p.sendSystemMessage(Jogo.prefixo(on ? "Chat travado no global. Use /g de novo ou /l para voltar ao local." : "Você voltou ao chat local."));
                return 1;
            })
            .then(argument("msg", greedyString()).executes(c -> {
                Chat.global(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "msg"));
                return 1;
            })));
        d.register(literal("l")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Chat.travarLocal(p);
                p.sendSystemMessage(Jogo.prefixo("Você está no chat local."));
                return 1;
            })
            .then(argument("msg", greedyString()).executes(c -> {
                Chat.local(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "msg"));
                return 1;
            })));
        // Mescla com o /msg do jogo (/tell e /w apontam para ele): sem texto trava a conversa, com texto envia.
        d.register(literal("msg").then(argument("targets", EntityArgument.players())
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                var alvos = EntityArgument.getPlayers(c, "targets");
                ServerPlayer alvo = alvos.stream().filter(x -> x != p).findFirst().orElse(null);
                if (alvo == null) return erro(c, "Escolha outro jogador.");
                Chat.travarPrivado(p, alvo);
                p.sendSystemMessage(Jogo.prefixo("Chat travado em conversa com " + alvo.getGameProfile().name() + ". Use /g ou /l para sair."));
                return 1;
            })
            .then(argument("message", MessageArgument.message()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String texto = MessageArgument.getMessage(c, "message").getString();
                int n = 0;
                for (ServerPlayer alvo : EntityArgument.getPlayers(c, "targets")) {
                    if (alvo == p) continue;
                    Chat.privado(p, alvo, texto);
                    n++;
                }
                return n > 0 ? 1 : erro(c, "Escolha outro jogador.");
            }))));
        d.register(literal("r")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                ServerPlayer alvo = alvoDoR(p);
                if (alvo == null) return erro(c, "Ninguém para responder (ou saiu do jogo).");
                Chat.travarPrivado(p, alvo);
                p.sendSystemMessage(Jogo.prefixo("Chat travado em conversa com " + alvo.getGameProfile().name() + ". Use /g ou /l para sair."));
                return 1;
            })
            .then(argument("msg", greedyString()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                ServerPlayer alvo = alvoDoR(p);
                if (alvo == null) return erro(c, "Ninguém para responder (ou saiu do jogo).");
                Chat.privado(p, alvo, StringArgumentType.getString(c, "msg"));
                return 1;
            })));
        d.register(literal("tells").requires(adminOuMod("tells"))
            .then(argument("nick", word()).suggests(Comandos::nomes).executes(c -> {
                String nick = StringArgumentType.getString(c, "nick");
                List<Casa.Tell> l = Chat.historico(nick);
                if (l.isEmpty()) return ok(c, "Nenhuma mensagem privada de " + nick + " guardada.");
                var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");
                var zona = java.time.ZoneId.of("America/Sao_Paulo");
                for (Casa.Tell t : l) {
                    ok(c, java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(t.ts), zona).format(fmt) + " " + t.de + " -> " + t.para + ": " + t.texto);
                }
                return l.size();
            })));
        d.register(literal("cor")
            .executes(c -> ok(c, "Mude a cor do seu nick no chat: /cor <#cor1> [#cor2] ou um nome pronto (" + String.join(", ", Gradiente.NOMES_PRESETS) + "). /cor off tira."))
            .then(literal("off").executes(c -> {
                Jogo.casa.config.cores.remove(c.getSource().getPlayerOrException().getGameProfile().name().toLowerCase());
                Jogo.salvar();
                return ok(c, "Cor do nick removida.");
            }))
            .then(argument("cor1", word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Gradiente.NOMES_PRESETS, b))
                .executes(c -> corDoJogador(c, null))
                .then(argument("cor2", word()).executes(c -> corDoJogador(c, StringArgumentType.getString(c, "cor2"))))));
    }

    private static int corDeOutro(CommandContext<CommandSourceStack> c, String cor2) {
        String nick = StringArgumentType.getString(c, "nick");
        String a = StringArgumentType.getString(c, "cor1");
        if (a.equalsIgnoreCase("off")) {
            Jogo.casa.config.cores.remove(nick.toLowerCase());
            Jogo.salvar();
            return ok(c, "Cor de " + nick + " removida.");
        }
        String erro = aplicarCor(nick, a, cor2);
        return erro == null ? ok(c, "Cor de " + nick + " atualizada.") : erro(c, erro);
    }

    private static ServerPlayer alvoDoR(ServerPlayer p) {
        String nome = Chat.ultimoDe(p);
        return nome == null ? null : Jogo.server.getPlayerList().getPlayerByName(nome);
    }

    private static int corDoJogador(CommandContext<CommandSourceStack> c, String cor2) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String erro = aplicarCor(p.getGameProfile().name(), StringArgumentType.getString(c, "cor1"), cor2);
        if (erro != null) return erro(c, erro);
        p.sendSystemMessage(Component.literal("Seu nick agora fica assim no chat: ").append(Prefixos.nomeColorido(p)));
        return 1;
    }

    private static void listaMsgs(LiteralArgumentBuilder<CommandSourceStack> pai, String nome, boolean entrar) {
        java.util.function.Supplier<List<String>> lista = () -> entrar ? Jogo.casa.config.msgEntrar : Jogo.casa.config.msgSair;
        pai.then(literal(nome)
            .then(literal("add").then(argument("texto", greedyString()).executes(c -> {
                String t = StringArgumentType.getString(c, "texto").trim();
                if (!t.contains("{nick}")) return erro(c, "Use {nick} onde vai o nome. Ex.: {nick} chegou!");
                lista.get().add(t);
                Jogo.salvar();
                return ok(c, "Adicionada (" + lista.get().size() + " no total).");
            })))
            .then(literal("remover").then(argument("n", integer(1)).executes(c -> {
                int n = IntegerArgumentType.getInteger(c, "n");
                if (n > lista.get().size()) return erro(c, "Só existem " + lista.get().size() + ".");
                lista.get().remove(n - 1);
                Jogo.salvar();
                return ok(c, "Removida.");
            })))
            .then(literal("lista").executes(c -> {
                List<String> l = lista.get();
                if (l.isEmpty()) return ok(c, "Nenhuma frase: vale a mensagem padrão do jogo.");
                for (int i = 0; i < l.size(); i++) ok(c, (i + 1) + ". " + l.get(i));
                return l.size();
            })));
    }

    // ---------- admin ----------

    /** /melbybet criar VOID Quem cai no Void primeiro?   |   para MANUAL: titulo | opcao1;opcao2;opcao3 */
    private static int criar(CommandContext<CommandSourceStack> c) {
        Bolao.Gatilho g = Bolao.Gatilho.de(StringArgumentType.getString(c, "gatilho"));
        if (g == null) return erro(c, "Gatilho inválido. Use: " + String.join(", ", Arrays.stream(Bolao.Gatilho.values()).map(Enum::name).toList()));
        String bruto = StringArgumentType.getString(c, "titulo");
        String titulo = bruto;
        List<String> opcoes = new ArrayList<>(Jogo.casa.config.roster);
        int barra = bruto.indexOf('|');
        if (barra >= 0) {
            titulo = bruto.substring(0, barra).trim();
            opcoes.clear();
            for (String o : bruto.substring(barra + 1).split(";")) if (!o.isBlank()) opcoes.add(o.trim());
        } else if (g == Bolao.Gatilho.MANUAL) {
            return erro(c, "Bolão manual precisa das opções: /melbybet criar MANUAL Título | opção1;opção2");
        }
        if (titulo.isBlank()) return erro(c, "Falta o título.");
        if (opcoes.size() < 2) return erro(c, "Precisa de pelo menos 2 opções.");
        Bolao b = Jogo.casa.criar(titulo, g, opcoes, System.currentTimeMillis());
        Jogo.salvar();
        return ok(c, "Bolão #" + b.id + " criado (fechado). Abra com /melbybet abrir " + b.id);
    }

    private static int mudar(CommandContext<CommandSourceStack> c, String acao) {
        Bolao b = Jogo.casa.achar(IntegerArgumentType.getInteger(c, "id"));
        if (b == null) return erro(c, "Bolão não encontrado.");
        String erro = null;
        switch (acao) {
            case "abrir" -> {
                erro = Jogo.casa.abrir(b);
                if (erro == null) {
                    Jogo.avisar("Novo bolão aberto: \"" + b.titulo + "\". Use /bet para apostar!");
                    Jogo.webhook("🎲 **Novo bolão aberto!** #" + b.id + " — " + b.titulo + "\nOpções: " + String.join(", ", b.opcoes));
                }
            }
            case "fechar" -> {
                erro = Jogo.casa.fechar(b);
                if (erro == null) Jogo.avisar("Bolão \"" + b.titulo + "\" fechado para apostas.");
            }
            default -> {
                if (!b.emAndamento()) erro = "Esse bolão já terminou.";
                else {
                    Jogo.casa.cancelar(b, System.currentTimeMillis());
                    Jogo.entregarTodos();
                    Jogo.avisar("Bolão \"" + b.titulo + "\" cancelado. Apostas devolvidas.");
                }
            }
        }
        if (erro != null) return erro(c, erro);
        Jogo.salvar();
        return 1;
    }

    private static int resolver(CommandContext<CommandSourceStack> c) {
        Bolao b = Jogo.casa.achar(IntegerArgumentType.getInteger(c, "id"));
        if (b == null) return erro(c, "Bolão não encontrado.");
        if (!b.emAndamento()) return erro(c, "Esse bolão já terminou.");
        String venc = b.opcaoExata(StringArgumentType.getString(c, "vencedor"));
        if (venc == null) return erro(c, "Opção inexistente. Opções: " + String.join(", ", b.opcoes));
        Jogo.resolver(b, venc);
        return 1;
    }

    private static int prazo(CommandContext<CommandSourceStack> c) {
        Bolao b = Jogo.casa.achar(IntegerArgumentType.getInteger(c, "id"));
        if (b == null) return erro(c, "Bolão não encontrado.");
        int min = IntegerArgumentType.getInteger(c, "minutos");
        b.expiraEm = System.currentTimeMillis() + min * 60_000L;
        Jogo.salvar();
        return ok(c, "O bolão #" + b.id + " fecha sozinho em " + min + " min (só vale se estiver aberto).");
    }

    private static int lista(CommandContext<CommandSourceStack> c) {
        List<Bolao> l = Jogo.casa.bolaos;
        if (l.isEmpty()) return ok(c, "Nenhum bolão criado.");
        for (Bolao b : l) {
            ok(c, "#" + b.id + " [" + b.status + "] " + b.titulo + " (" + b.gatilho + ", pote " + b.pote() + ")"
                + (b.status == Bolao.Status.AGUARDANDO ? " candidatos: " + String.join(", ", b.candidatos) : ""));
        }
        return l.size();
    }

    // ---------- respostas ----------

    private static int ok(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static int erro(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendFailure(Component.literal(s));
        return 0;
    }
}
