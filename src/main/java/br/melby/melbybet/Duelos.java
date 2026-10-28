package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /duelo: dois jogadores apostam e quem matar o outro primeiro leva tudo.
 * Em diamantes (/duelo nick 10) ou em itens (/duelo nick inv): cada um escolhe itens do inventário do outro,
 * e só no fim, depois do duelo, os itens do perdedor passam para o vencedor.
 */
public final class Duelos {
    private Duelos() {}

    private static final long CONVITE_MS = 60_000L;
    private static final long ESCOLHA_MS = 5 * 60_000L;
    private static final long DURACAO_MS = 15 * 60_000L;
    private static final long COMBATE_MS = 30_000L;
    static final int MAX_ITENS = 5;
    /** Quanto tempo ficam abertas as apostas no duelo dos outros, e o limite por apostador. */
    private static final long APOSTAS_MS = 60_000L;
    private static final int APOSTA_MAX_DUELO = 16;

    /** uuid -> instante do último golpe entre os dois duelistas. Só na memória. */
    private static final Map<UUID, Long> COMBATE = new HashMap<>();

    /** Dados de um duelo de itens (só na memória: se o servidor reiniciar, o duelo é cancelado e nada se perde). */
    static final class Itens {
        final Casa.Duelo d;
        List<ItemStack> daA = new ArrayList<>(); // o que A aposta (escolhido por B)
        List<ItemStack> daB = new ArrayList<>(); // o que B aposta (escolhido por A)
        boolean confA, confB, okA, okB;

        Itens(Casa.Duelo d) {
            this.d = d;
        }
    }

    private static final Map<Integer, Itens> ITENS = new HashMap<>();

    private static long combateRestante(UUID id) {
        Long t = COMBATE.get(id);
        if (t == null) return 0;
        return Math.max(0, COMBATE_MS - (System.currentTimeMillis() - t));
    }

    private static boolean emCombate(String uuid) {
        return combateRestante(UUID.fromString(uuid)) > 0;
    }

    /** Chamado quando alguém leva dano: se foi do outro duelista, os dois entram em combate por 30 s. */
    public static void aoDano(net.minecraft.world.entity.LivingEntity vitima, DamageSource fonte) {
        if (!(vitima instanceof ServerPlayer v) || !(fonte.getEntity() instanceof ServerPlayer atacante)) return;
        Casa.Duelo x = Jogo.casa.duelAtivoDe(v.getUUID().toString());
        if (x == null || x != Jogo.casa.duelAtivoDe(atacante.getUUID().toString())) return;
        long agora = System.currentTimeMillis();
        COMBATE.put(v.getUUID(), agora);
        COMBATE.put(atacante.getUUID(), agora);
    }

    private static MutableComponent botao(String rotulo, ChatFormatting cor, String comando, String dica) {
        return Component.literal("[" + rotulo + "]").withStyle(s -> s.withColor(cor).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand(comando))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(dica))));
    }

    private static ServerPlayer on(String uuid) {
        return Jogo.server.getPlayerList().getPlayer(UUID.fromString(uuid));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("duelo")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Casa.Duelo x = Jogo.casa.duelAtivoDe(p.getUUID().toString());
                if (x == null) {
                    p.sendSystemMessage(Jogo.prefixo("Use /duelo <nick> <valor> (diamantes) ou /duelo <nick> inv (itens do inventário do outro). Quem matar o outro primeiro leva tudo; /duelo desistir entrega a vitória."));
                } else {
                    p.sendSystemMessage(Jogo.prefixo("Duelo: " + x.nomeA + " x " + x.nomeB + (x.itens ? " (itens)" : ", " + x.valor + " diamante(s) cada") + ". Vence quem matar o outro."));
                }
                return 1;
            })
            .then(literal("aceitar").then(argument("id", integer(1)).executes(c -> {
                aceitar(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "id"));
                return 1;
            })))
            .then(literal("recusar").then(argument("id", integer(1)).executes(c -> {
                recusar(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "id"));
                return 1;
            })))
            .then(literal("termos").then(argument("id", integer(1)).executes(c -> {
                aceitarTermos(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "id"));
                return 1;
            })))
            .then(literal("placar")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    placar(p, p.getGameProfile().name());
                    return 1;
                })
                .then(argument("nick", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Jogo.casa.conhecidos.values(), b))
                    .executes(c -> {
                        placar(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nick"));
                        return 1;
                    })))
            .then(literal("desistir").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Casa.Duelo x = Jogo.casa.duelAtivoDe(p.getUUID().toString());
                if (x == null) {
                    p.sendSystemMessage(Jogo.prefixo("Você não está em um duelo."));
                    return 0;
                }
                long resta = combateRestante(p.getUUID());
                if (resta > 0) {
                    p.sendSystemMessage(Jogo.prefixo("Você está em combate. Espere " + (resta / 1000 + 1) + "s para desistir."));
                    return 0;
                }
                String outro = x.uuidA.equals(p.getUUID().toString()) ? x.uuidB : x.uuidA;
                vencer(x, outro, p.getGameProfile().name() + " desistiu.");
                return 1;
            }))
            .then(argument("alvo", EntityArgument.player())
                .then(literal("inv").executes(c -> {
                    proporItens(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "alvo"));
                    return 1;
                }))
                .then(argument("valor", integer(1)).executes(c -> {
                    propor(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "alvo"), IntegerArgumentType.getInteger(c, "valor"));
                    return 1;
                }))));
    }

    // ---------- apostas dos espectadores ----------

    /** Quando o duelo começa, abre um bolão curto com os dois lutadores. Eles mesmos não podem apostar. */
    private static void abrirApostas(Casa.Duelo x) {
        Casa casa = Jogo.casa;
        long agora = System.currentTimeMillis();
        var b = casa.criar("Duelo: " + x.nomeA + " x " + x.nomeB, br.melby.melbybet.core.Bolao.Gatilho.MANUAL, List.of(x.nomeA, x.nomeB), agora);
        b.dueloId = x.id;
        b.excluidos.add(x.uuidA);
        b.excluidos.add(x.uuidB);
        b.maxAposta = APOSTA_MAX_DUELO;
        b.expiraEm = agora + APOSTAS_MS;
        casa.abrir(b);
        Jogo.avisar("Apostas abertas no duelo " + x.nomeA + " x " + x.nomeB + " por " + (APOSTAS_MS / 1000) + "s (até " + APOSTA_MAX_DUELO + " diamantes). Use /bet!");
    }

    private static br.melby.melbybet.core.Bolao bolaoDoDuelo(Casa.Duelo x) {
        for (var b : Jogo.casa.bolaos) if (b.dueloId == x.id && b.emAndamento()) return b;
        return null;
    }

    /** Fecha o bolão do duelo: paga quem acertou, ou devolve tudo se o duelo foi cancelado. */
    private static void encerrarApostas(Casa.Duelo x, String vencedorNome) {
        var b = bolaoDoDuelo(x);
        if (b == null) return;
        if (b.apostas.isEmpty()) {
            b.status = br.melby.melbybet.core.Bolao.Status.CANCELADO;
            return;
        }
        if (vencedorNome != null) {
            Jogo.resolver(b, vencedorNome);
        } else {
            Jogo.casa.cancelar(b, System.currentTimeMillis());
            Jogo.avisar("Apostas do duelo " + x.nomeA + " x " + x.nomeB + " devolvidas.");
        }
    }

    // ---------- placar ----------

    private static void placar(ServerPlayer p, String nick) {
        Casa casa = Jogo.casa;
        String nome = null;
        for (String n : casa.conhecidos.values()) if (n.equalsIgnoreCase(nick)) nome = n;
        if (nome == null) {
            p.sendSystemMessage(Jogo.prefixo("Não conheço ninguém chamado " + nick + "."));
            return;
        }
        final String alvo = nome;
        List<Casa.DueloReg> lista = casa.duelosDe(alvo, 300);
        p.sendSystemMessage(Component.literal("== Duelos de " + alvo + " ==").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (lista.isEmpty()) {
            p.sendSystemMessage(Component.literal("Nenhum duelo ainda.").withStyle(ChatFormatting.GRAY));
            return;
        }
        int v = 0;
        int dd = 0;
        java.util.Set<String> rivais = new java.util.LinkedHashSet<>();
        for (Casa.DueloReg r : lista) {
            boolean ganhou = r.nomeVenc.equalsIgnoreCase(alvo);
            if (ganhou) v++;
            else dd++;
            rivais.add(ganhou ? r.nomePerd : r.nomeVenc);
        }
        p.sendSystemMessage(Component.literal(v + " vitória(s) e " + dd + " derrota(s).").withStyle(ChatFormatting.WHITE));
        List<String> ordem = new ArrayList<>(rivais);
        ordem.sort((x, y) -> {
            int[] a = casa.confronto(alvo, x);
            int[] b = casa.confronto(alvo, y);
            return Integer.compare(b[0] + b[1], a[0] + a[1]);
        });
        for (int i = 0; i < Math.min(5, ordem.size()); i++) {

            int[] c = casa.confronto(alvo, ordem.get(i));
            p.sendSystemMessage(Component.literal("contra " + ordem.get(i) + ": " + alvo + " " + c[0] + " x " + c[1] + " " + ordem.get(i)).withStyle(ChatFormatting.AQUA));
        }
        p.sendSystemMessage(Component.literal("Últimos duelos:").withStyle(ChatFormatting.YELLOW));
        long agora = System.currentTimeMillis();
        for (int i = 0; i < Math.min(5, lista.size()); i++) {
            Casa.DueloReg r = lista.get(i);
            p.sendSystemMessage(Component.literal(r.nomeVenc + " venceu " + r.nomePerd + (r.resumo.isEmpty() ? "" : " (" + r.resumo + ")")
                + ", há " + br.melby.melbybet.core.Fmt.duracao(agora - r.ts)).withStyle(ChatFormatting.GRAY));
        }
    }

    // ---------- duelo em diamantes ----------

    private static void propor(ServerPlayer a, ServerPlayer b, int valor) {
        String erro = Jogo.casa.validarDuelo(a.getUUID().toString(), b.getUUID().toString(), valor);
        if (erro != null) {
            a.sendSystemMessage(Jogo.prefixo(erro));
            return;
        }
        if (Jogo.contar(a) < valor) {
            a.sendSystemMessage(Jogo.prefixo("Você só tem " + Jogo.contar(a) + " diamante(s)."));
            return;
        }
        Casa.Duelo x = Jogo.casa.proporDuelo(a.getUUID().toString(), a.getGameProfile().name(), b.getUUID().toString(), b.getGameProfile().name(),
            valor, System.currentTimeMillis());
        Jogo.salvar();
        a.sendSystemMessage(Jogo.prefixo("Convite enviado para " + b.getGameProfile().name() + " (vale 60 segundos)."));
        b.sendSystemMessage(Jogo.prefixo(a.getGameProfile().name() + " te chamou para um duelo de " + valor + " diamante(s) cada. Quem matar o outro primeiro leva tudo. ")
            .copy().append(botao("Aceitar", ChatFormatting.GREEN, "/duelo aceitar " + x.id, "Põe " + valor + " diamante(s) e começa"))
            .append(Component.literal(" "))
            .append(botao("Recusar", ChatFormatting.RED, "/duelo recusar " + x.id, "Não duelar")));
    }

    // ---------- duelo em itens ----------

    private static void proporItens(ServerPlayer a, ServerPlayer b) {
        String erro = Jogo.casa.validarDuelo(a.getUUID().toString(), b.getUUID().toString(), 1);
        if (erro != null) {
            a.sendSystemMessage(Jogo.prefixo(erro));
            return;
        }
        Casa.Duelo x = Jogo.casa.proporDuelo(a.getUUID().toString(), a.getGameProfile().name(), b.getUUID().toString(), b.getGameProfile().name(),
            0, System.currentTimeMillis());
        x.itens = true;
        Itens st = new Itens(x);
        ITENS.put(x.id, st);
        Jogo.salvar();
        b.sendSystemMessage(Jogo.prefixo(a.getGameProfile().name() + " te chamou para um duelo de ITENS: cada um escolhe até " + MAX_ITENS
            + " itens do inventário do outro, e só o perdedor entrega os seus, depois do duelo. ")
            .copy().append(botao("Aceitar", ChatFormatting.GREEN, "/duelo aceitar " + x.id, "Abre o inventário do " + a.getGameProfile().name()))
            .append(Component.literal(" "))
            .append(botao("Recusar", ChatFormatting.RED, "/duelo recusar " + x.id, "Não duelar")));
        a.sendSystemMessage(Jogo.prefixo("Escolha os itens de " + b.getGameProfile().name() + " que você quer ganhar. O convite vale 60 segundos."));
        MenuDuelo.abrir(a, b, st, true);
    }

    /** O jogador terminou de escolher (confirmou) os itens do outro. */
    static void confirmarEscolha(ServerPlayer quem, Itens st, boolean souA, List<ItemStack> escolhidos) {
        if (souA) {
            st.daB = escolhidos;
            st.confA = true;
        } else {
            st.daA = escolhidos;
            st.confB = true;
        }
        quem.sendSystemMessage(Jogo.prefixo("Escolha confirmada. Aguardando o outro jogador."));
        if (st.confA && st.confB && st.d.estado.equals("ESCOLHENDO")) mostrarTermos(st);
    }

    static Itens itensDe(int id) {
        return ITENS.get(id);
    }

    private static Component lista(List<ItemStack> l) {
        MutableComponent m = Component.empty();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) m.append(Component.literal(", "));
            m.append(Component.literal(l.get(i).getCount() + "x ")).append(l.get(i).getDisplayName());
        }
        return m;
    }

    private static void mostrarTermos(Itens st) {
        Casa.Duelo x = st.d;
        ServerPlayer a = on(x.uuidA);
        ServerPlayer b = on(x.uuidB);
        if (a == null || b == null) {
            cancelar(x, "alguém saiu antes de fechar os termos.");
            return;
        }
        enviarTermos(a, x, "Você aposta", st.daA, "Você leva, se vencer", st.daB);
        enviarTermos(b, x, "Você aposta", st.daB, "Você leva, se vencer", st.daA);
    }

    private static void enviarTermos(ServerPlayer p, Casa.Duelo x, String t1, List<ItemStack> l1, String t2, List<ItemStack> l2) {
        p.sendSystemMessage(Component.literal("== Termos do duelo ==").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal(t1 + ": ").withStyle(ChatFormatting.RED).append(lista(l1)));
        p.sendSystemMessage(Component.literal(t2 + ": ").withStyle(ChatFormatting.GREEN).append(lista(l2)));
        p.sendSystemMessage(Component.literal("Nada sai agora: só o perdedor entrega, depois do duelo. ").withStyle(ChatFormatting.GRAY)
            .append(botao("Aceitar termos", ChatFormatting.GREEN, "/duelo termos " + x.id, "Começa o duelo quando os dois aceitarem"))
            .append(Component.literal(" "))
            .append(botao("Recusar", ChatFormatting.RED, "/duelo recusar " + x.id, "Cancela o duelo")));
    }

    private static void aceitarTermos(ServerPlayer p, int id) {
        Itens st = ITENS.get(id);
        if (st == null || !st.d.estado.equals("ESCOLHENDO") || !(st.confA && st.confB)) {
            p.sendSystemMessage(Jogo.prefixo("Não há termos para aceitar."));
            return;
        }
        String u = p.getUUID().toString();
        if (u.equals(st.d.uuidA)) st.okA = true;
        else if (u.equals(st.d.uuidB)) st.okB = true;
        else return;
        p.sendSystemMessage(Jogo.prefixo("Termos aceitos. Aguardando o outro."));
        if (!(st.okA && st.okB)) return;
        ServerPlayer a = on(st.d.uuidA);
        ServerPlayer b = on(st.d.uuidB);
        if (a == null || b == null) {
            cancelar(st.d, "alguém saiu.");
            return;
        }
        if (faltando(a, st.daA)) {
            cancelar(st.d, st.d.nomeA + " não tem mais todos os itens da aposta.");
            return;
        }
        if (faltando(b, st.daB)) {
            cancelar(st.d, st.d.nomeB + " não tem mais todos os itens da aposta.");
            return;
        }
        Jogo.casa.ativarDuelo(st.d, System.currentTimeMillis());
        abrirApostas(st.d);
        Jogo.salvar();
        Jogo.avisar("Duelo de itens valendo! " + st.d.nomeA + " x " + st.d.nomeB + ". Quem matar o outro primeiro leva os itens. Não guarde nem jogue fora os itens da aposta!");
    }

    // ---------- itens: conferir e transferir ----------

    /**
     * Mesmo item para efeito da aposta: mesmo tipo, mesmos encantamentos e mesmo nome.
     * A durabilidade (e o custo de reparo) é ignorada: a espada gasta no duelo, e o Remendo a repara.
     */
    static boolean mesmoItem(ItemStack a, ItemStack b) {
        return a.is(b.getItem())
            && java.util.Objects.equals(a.get(DataComponents.ENCHANTMENTS), b.get(DataComponents.ENCHANTMENTS))
            && java.util.Objects.equals(a.get(DataComponents.STORED_ENCHANTMENTS), b.get(DataComponents.STORED_ENCHANTMENTS))
            && java.util.Objects.equals(a.get(DataComponents.CUSTOM_NAME), b.get(DataComponents.CUSTOM_NAME));
    }

    private static int contarIgual(Inventory inv, ItemStack modelo) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && mesmoItem(s, modelo)) n += s.getCount();
        }
        return n;
    }

    /** true se falta algum item da aposta no inventário do dono. */
    private static boolean faltando(ServerPlayer p, List<ItemStack> apostados) {
        for (ItemStack m : apostados) {
            int precisa = 0;
            for (ItemStack o : apostados) if (mesmoItem(o, m)) precisa += o.getCount();
            if (contarIgual(p.getInventory(), m) < precisa) return true;
        }
        return false;
    }

    /** Tira do perdedor os itens apostados e dá ao vencedor. Devolve o que foi de fato transferido. */
    private static List<ItemStack> transferir(ServerPlayer perdedor, ServerPlayer vencedor, List<ItemStack> apostados) {
        List<ItemStack> passou = new ArrayList<>();
        Inventory inv = perdedor.getInventory();
        for (ItemStack m : apostados) {
            int falta = m.getCount();
            for (int i = 0; i < inv.getContainerSize() && falta > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty() || !mesmoItem(s, m)) continue;
                int x = Math.min(falta, s.getCount());
                ItemStack parte = s.copy();
                parte.setCount(x);
                s.shrink(x);
                falta -= x;
                passou.add(parte);
            }
        }
        inv.setChanged();
        perdedor.containerMenu.broadcastChanges();
        if (vencedor != null) {
            for (ItemStack it : passou) {
                ItemStack resto = it.copy();
                vencedor.getInventory().add(resto);
                if (!resto.isEmpty()) vencedor.drop(resto, false);
            }
            vencedor.containerMenu.broadcastChanges();
        }
        return passou;
    }

    // ---------- aceitar, recusar, resolver ----------

    private static void aceitar(ServerPlayer b, int id) {
        Casa.Duelo x = Jogo.casa.achaDuelo(id);
        if (x == null || !x.estado.equals("PROPOSTO") || !x.uuidB.equals(b.getUUID().toString())) {
            b.sendSystemMessage(Jogo.prefixo("Esse convite não existe mais."));
            return;
        }
        ServerPlayer a = on(x.uuidA);
        if (a == null) {
            x.estado = "CANCELADO";
            ITENS.remove(x.id);
            b.sendSystemMessage(Jogo.prefixo(x.nomeA + " não está mais online."));
            return;
        }
        if (x.itens) {
            Itens st = ITENS.get(x.id);
            String erro = (Jogo.casa.ocupado(x.uuidA) || Jogo.casa.ocupado(x.uuidB)) ? "Alguém já está em outro duelo." : null;
            if (st == null || erro != null) {
                b.sendSystemMessage(Jogo.prefixo(erro != null ? erro : "Esse convite não existe mais."));
                return;
            }
            x.estado = "ESCOLHENDO";
            x.criadoEm = System.currentTimeMillis();
            a.sendSystemMessage(Jogo.prefixo(x.nomeB + " aceitou. Ele está escolhendo os seus itens."));
            b.sendSystemMessage(Jogo.prefixo("Escolha os itens de " + x.nomeA + " que você quer ganhar (até " + MAX_ITENS + ")."));
            MenuDuelo.abrir(b, a, st, false);
            return;
        }
        String erro = Jogo.casa.validarDuelo(x.uuidA, x.uuidB, x.valor);
        // o convite do próprio jogador conta como pendente; ignora esse aviso específico
        if (erro != null && !erro.equals("Você já tem um convite pendente.")) {
            b.sendSystemMessage(Jogo.prefixo(erro));
            return;
        }
        if (Jogo.contar(a) < x.valor) {
            b.sendSystemMessage(Jogo.prefixo(x.nomeA + " não tem " + x.valor + " diamante(s) agora."));
            return;
        }
        if (Jogo.contar(b) < x.valor) {
            b.sendSystemMessage(Jogo.prefixo("Você só tem " + Jogo.contar(b) + " diamante(s)."));
            return;
        }
        Jogo.tirar(a, x.valor);
        Jogo.tirar(b, x.valor);
        Jogo.casa.ativarDuelo(x, System.currentTimeMillis());
        abrirApostas(x);
        Jogo.salvar();
        Jogo.avisar("Duelo valendo! " + x.nomeA + " x " + x.nomeB + ", " + x.valor + " diamante(s) cada. Quem matar o outro primeiro leva o pote.");
    }

    private static void recusar(ServerPlayer p, int id) {
        Casa.Duelo x = Jogo.casa.achaDuelo(id);
        if (x == null) return;
        String u = p.getUUID().toString();
        boolean participante = x.uuidA.equals(u) || x.uuidB.equals(u);
        boolean aberto = x.estado.equals("PROPOSTO") || (x.itens && x.estado.equals("ESCOLHENDO"));
        if (!participante || !aberto) return;
        if (x.estado.equals("PROPOSTO") && !x.itens && !x.uuidB.equals(u)) return;
        x.estado = "CANCELADO";
        ITENS.remove(x.id);
        Jogo.salvar();
        p.sendSystemMessage(Jogo.prefixo("Duelo cancelado."));
        ServerPlayer outro = on(x.uuidA.equals(u) ? x.uuidB : x.uuidA);
        if (outro != null) outro.sendSystemMessage(Jogo.prefixo(p.getGameProfile().name() + " cancelou o duelo."));
    }

    /** O jogador fechou a janela de escolha ou desistiu antes de começar. */
    static void cancelarEscolha(ServerPlayer p, Itens st) {
        recusar(p, st.d.id);
    }

    private static String descricao(List<ItemStack> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(l.get(i).getCount()).append("x ").append(l.get(i).getHoverName().getString());
        }
        return sb.toString();
    }

    private static void vencer(Casa.Duelo x, String uuidVencedor, String motivo) {
        String nome = uuidVencedor.equals(x.uuidA) ? x.nomeA : x.nomeB;
        String perdedorNome = uuidVencedor.equals(x.uuidA) ? x.nomeB : x.nomeA;
        if (x.itens) {
            Itens st = ITENS.remove(x.id);
            ServerPlayer venc = on(uuidVencedor);
            String perdUuid = uuidVencedor.equals(x.uuidA) ? x.uuidB : x.uuidA;
            ServerPlayer perd = on(perdUuid);
            List<ItemStack> apostados = st == null ? new ArrayList<>() : (perdUuid.equals(x.uuidA) ? st.daA : st.daB);
            List<ItemStack> passou = perd == null ? new ArrayList<>() : transferir(perd, venc, apostados);
            Jogo.casa.resolverDueloItens(x, uuidVencedor, System.currentTimeMillis(), passou.isEmpty() ? "nada" : descricao(passou));
            Jogo.avisar("Fim do duelo! " + motivo + " " + nome + " leva: " + (passou.isEmpty() ? "nada (os itens não estavam mais lá)" : descricao(passou)) + ".");
            Jogo.webhook("⚔️ **Duelo de itens** — " + nome + " venceu " + perdedorNome + (passou.isEmpty() ? "" : " e levou " + descricao(passou)));
            encerrarApostas(x, nome);
            Jogo.salvar();
            return;
        }
        int premio = Jogo.casa.resolverDuelo(x, uuidVencedor, System.currentTimeMillis());
        Jogo.avisar("Fim do duelo! " + motivo + " " + nome + " leva " + premio + " diamante(s).");
        Jogo.webhook("⚔️ **Duelo** — " + nome + " venceu " + perdedorNome + " e levou **" + premio + "** 💎");
        encerrarApostas(x, nome);
        Jogo.entregarTodos();
        Jogo.salvar();
    }

    private static void cancelar(Casa.Duelo x, String motivo) {
        ITENS.remove(x.id);
        Jogo.casa.cancelarDuelo(x, System.currentTimeMillis());
        encerrarApostas(x, null);
        Jogo.avisar("Duelo " + x.nomeA + " x " + x.nomeB + " cancelado: " + motivo + (x.itens ? "" : " Os diamantes foram devolvidos."));
        Jogo.entregarTodos();
        Jogo.salvar();
    }

    /** Antes de a morte acontecer (os itens ainda estão no inventário): se foi o outro duelista, já resolve. */
    public static void antesDeMorrer(ServerPlayer morto, DamageSource fonte) {
        Casa.Duelo x = Jogo.casa.duelAtivoDe(morto.getUUID().toString());
        if (x == null) return;
        String outro = x.uuidA.equals(morto.getUUID().toString()) ? x.uuidB : x.uuidA;
        if (fonte.getEntity() instanceof ServerPlayer matador && matador.getUUID().toString().equals(outro)) {
            vencer(x, outro, morto.getGameProfile().name() + " caiu.");
        }
    }

    /** Depois da morte: se ainda sobrou duelo ativo, foi por outro motivo. */
    public static void aoMorrer(ServerPlayer morto, DamageSource fonte) {
        Casa.Duelo x = Jogo.casa.duelAtivoDe(morto.getUUID().toString());
        if (x == null) return;
        String outro = x.uuidA.equals(morto.getUUID().toString()) ? x.uuidB : x.uuidA;
        if (fonte.getEntity() instanceof ServerPlayer matador && matador.getUUID().toString().equals(outro)) {
            vencer(x, outro, morto.getGameProfile().name() + " caiu.");
        } else {
            cancelar(x, morto.getGameProfile().name() + " morreu por outro motivo.");
        }
    }

    /** Ao sair do jogo (o inventário ainda existe): perde o duelo, e em duelo de itens já entrega os itens. */
    public static void aoSair(ServerPlayer p) {
        if (Jogo.server == null) return;
        Casa.Duelo x = Jogo.casa.duelAtivoDe(p.getUUID().toString());
        if (x == null) return;
        String u = p.getUUID().toString();
        String outro = x.uuidA.equals(u) ? x.uuidB : x.uuidA;
        vencer(x, outro, p.getGameProfile().name() + " saiu do jogo" + (emCombate(u) ? " em combate." : "."));
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        if (Jogo.server == null || Jogo.casa.duelos.isEmpty()) return;
        long agora = System.currentTimeMillis();
        boolean limpar = false;
        for (Casa.Duelo x : new ArrayList<>(Jogo.casa.duelos)) {
            if (x.estado.equals("PROPOSTO")) {
                if (agora - x.criadoEm > CONVITE_MS) {
                    x.estado = "CANCELADO";
                    ITENS.remove(x.id);
                    limpar = true;
                }
            } else if (x.estado.equals("ESCOLHENDO")) {
                if (agora - x.criadoEm > ESCOLHA_MS || on(x.uuidA) == null || on(x.uuidB) == null) {
                    x.estado = "CANCELADO";
                    ITENS.remove(x.id);
                    limpar = true;
                }
            } else if (x.estado.equals("ATIVO")) {
                ServerPlayer pa = on(x.uuidA);
                ServerPlayer pb = on(x.uuidB);
                if (pa == null && pb == null) cancelar(x, "os dois saíram.");
                else if (pa == null) vencer(x, x.uuidB, x.nomeA + " saiu do jogo" + (emCombate(x.uuidA) ? " em combate." : "."));
                else if (pb == null) vencer(x, x.uuidA, x.nomeB + " saiu do jogo" + (emCombate(x.uuidB) ? " em combate." : "."));
                else if (agora - x.inicio > DURACAO_MS) cancelar(x, "passou de 15 minutos sem vencedor.");
                else {
                    Itens st = x.itens ? ITENS.get(x.id) : null;
                    if (x.itens && st == null) {
                        cancelar(x, "os dados do duelo se perderam.");
                    } else if (st != null && faltando(pa, st.daA)) {
                        vencer(x, x.uuidB, x.nomeA + " tirou um item da aposta do inventário.");
                    } else if (st != null && faltando(pb, st.daB)) {
                        vencer(x, x.uuidA, x.nomeB + " tirou um item da aposta do inventário.");
                    } else {
                        for (String u : new String[] {x.uuidA, x.uuidB}) {
                            long resta = combateRestante(UUID.fromString(u));
                            ServerPlayer jp = on(u);
                            if (resta > 0 && jp != null) jp.sendSystemMessage(Component.literal("Em combate: " + (resta / 1000 + 1) + "s").withStyle(ChatFormatting.RED), true);
                        }
                    }
                }
            }
        }
        if (limpar) {
            Jogo.casa.duelos.removeIf(x -> x.estado.equals("CANCELADO") && x.criadoEm < agora - CONVITE_MS);
            Jogo.salvar();
        }
    }

    /** Ao ligar o servidor: convites e duelos de itens que estavam no ar somem (nada foi tirado de ninguém). */
    public static void aoLigar() {
        Jogo.casa.duelos.removeIf(x -> x.estado.equals("PROPOSTO") || x.estado.equals("ESCOLHENDO") || (x.itens && x.estado.equals("ATIVO")));
        // bolões de duelos que não existem mais: devolve as apostas
        for (var b : new ArrayList<>(Jogo.casa.bolaos)) {
            if (b.dueloId <= 0 || !b.emAndamento()) continue;
            Casa.Duelo x = Jogo.casa.achaDuelo(b.dueloId);
            if (x == null || !x.estado.equals("ATIVO")) {
                if (b.apostas.isEmpty()) b.status = br.melby.melbybet.core.Bolao.Status.CANCELADO;
                else Jogo.casa.cancelar(b, System.currentTimeMillis());
            }
        }
    }
}
