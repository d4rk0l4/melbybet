package br.melby.melbybet;

import br.melby.melbybet.core.Fmt;
import br.melby.melbybet.core.MarcadorDados;
import br.melby.melbybet.core.MarcadorDados.Area;
import br.melby.melbybet.core.MarcadorDados.Selecao;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /marcador: escolhe uma área (dois cantos) com comando ou com a varinha, mostra os limites com partículas só para
 * quem escolheu, e salva áreas com nome (que dá para compartilhar). Outros recursos de construção usam esta seleção.
 * Não precisa de nada instalado no cliente.
 */
public final class Marcadores {
    private Marcadores() {}

    private static MarcadorDados dados = new MarcadorDados();
    private static Path arq;
    private static boolean sujo;
    /** Quem desligou as partículas com /marcador ver. */
    private static final Set<UUID> OCULTOS = new HashSet<>();

    /** Quantos pontos de partícula no máximo por jogador a cada pulso, e a que distância ainda desenhamos. */
    private static final int MAX_PONTOS = 220;
    private static final double DIST_MAX = 48.0;

    private static final DustParticleOptions COR_AREA = new DustParticleOptions(0x55FF55, 1.0f);
    private static final DustParticleOptions COR_P1 = new DustParticleOptions(0xFF5555, 1.2f);
    private static final DustParticleOptions COR_P2 = new DustParticleOptions(0x5599FF, 1.2f);

    // ---------- estado ----------

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("marcadores.json");
        dados = MarcadorDados.ler(arq);
        sujo = false;
    }

    public static void salvarAgora() {
        if (arq == null || !sujo) return;
        try {
            dados.gravar(arq);
            sujo = false;
        } catch (Exception e) {
            Melbybet.LOGGER.error("Falha ao salvar {}", arq, e);
        }
    }

    /** A seleção atual do jogador (quem chama decide se ela está completa). Usada pelos outros recursos de construção. */
    public static Selecao selecaoDe(ServerPlayer p) {
        return dados.de(p.getUUID().toString()).atual;
    }

    // ---------- varinha ----------

    private static CompoundTag marca() {
        CompoundTag t = new CompoundTag();
        t.putString("melbycraft", "marcador");
        return t;
    }

    private static boolean ehVarinha(ItemStack st) {
        if (st.isEmpty() || !st.is(Items.STICK)) return false;
        CustomData cd = st.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.matchedBy(marca());
    }

    private static ItemStack novaVarinha() {
        ItemStack st = new ItemStack(Items.STICK);
        st.set(DataComponents.CUSTOM_DATA, CustomData.of(marca()));
        st.set(DataComponents.CUSTOM_NAME, Component.literal("Varinha de Marcação").withStyle(ChatFormatting.GREEN)
            .withStyle(s -> s.withItalic(false)));
        st.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return st;
    }

    // ---------- comandos ----------

    private static CompletableFuture<Suggestions> sugerirAreas(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        if (!(c.getSource().getEntity() instanceof ServerPlayer p)) return b.buildFuture();
        String eu = p.getUUID().toString();
        List<String> nomes = new ArrayList<>();
        for (Area a : dados.visiveis(eu)) nomes.add(a.dono.equals(eu) ? a.nome : a.donoNome + ":" + a.nome);
        return SharedSuggestionProvider.suggest(nomes, b);
    }

    private static CompletableFuture<Suggestions> sugerirMinhas(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        if (!(c.getSource().getEntity() instanceof ServerPlayer p)) return b.buildFuture();
        List<String> nomes = new ArrayList<>();
        for (Area a : dados.de(p.getUUID().toString()).salvas.values()) nomes.add(a.nome);
        return SharedSuggestionProvider.suggest(nomes, b);
    }

    private static CompletableFuture<Suggestions> sugerirJogadores(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(Jogo.casa.conhecidos.values(), b);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> canto(String nome, int canto) {
        return literal(nome)
            .then(argument("pos", BlockPosArgument.blockPos()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                BlockPos pos = BlockPosArgument.getBlockPos(c, "pos");
                return marcar(p, canto, pos, "");
            }))
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                HitResult h = p.pick(64.0, 1.0F, false);
                if (h instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) return marcar(p, canto, bh.getBlockPos(), "");
                return marcar(p, canto, p.blockPosition(), " (onde você está)");
            });
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("marcador")
            .then(canto("pos1", 1))
            .then(canto("pos2", 2))
            .then(literal("varinha").executes(c -> varinha(c.getSource().getPlayerOrException())))
            .then(literal("ver").executes(c -> alternarVer(c.getSource().getPlayerOrException())))
            .then(literal("limpar").executes(c -> limpar(c.getSource().getPlayerOrException())))
            .then(literal("info").executes(c -> info(c.getSource().getPlayerOrException())))
            .then(literal("altura").executes(c -> altura(c.getSource().getPlayerOrException())))
            .then(literal("expandir").then(argument("n", IntegerArgumentType.integer(1, 256))
                .executes(c -> ajustar(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n")))))
            .then(literal("contrair").then(argument("n", IntegerArgumentType.integer(1, 256))
                .executes(c -> ajustar(c.getSource().getPlayerOrException(), -IntegerArgumentType.getInteger(c, "n")))))
            .then(literal("salvar").then(argument("nome", StringArgumentType.word())
                .executes(c -> salvar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome")))))
            .then(literal("lista").executes(c -> lista(c.getSource().getPlayerOrException())))
            .then(literal("carregar").then(argument("area", StringArgumentType.word()).suggests(Marcadores::sugerirAreas)
                .executes(c -> carregarArea(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "area")))))
            .then(literal("apagar").then(argument("nome", StringArgumentType.word()).suggests(Marcadores::sugerirMinhas)
                .executes(c -> apagar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome")))))
            .then(literal("compartilhar").then(argument("nome", StringArgumentType.word()).suggests(Marcadores::sugerirMinhas)
                .then(argument("jogador", StringArgumentType.word()).suggests(Marcadores::sugerirJogadores)
                    .executes(c -> compartilhar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome"),
                        StringArgumentType.getString(c, "jogador"), true)))))
            .then(literal("descompartilhar").then(argument("nome", StringArgumentType.word()).suggests(Marcadores::sugerirMinhas)
                .then(argument("jogador", StringArgumentType.word()).suggests(Marcadores::sugerirJogadores)
                    .executes(c -> compartilhar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome"),
                        StringArgumentType.getString(c, "jogador"), false)))))
            .executes(c -> ajuda(c.getSource().getPlayerOrException())));
    }

    private static int ajuda(ServerPlayer p) {
        String[] l = {
            "/marcador varinha: pega a varinha (clique esquerdo = canto 1, direito = canto 2)",
            "/marcador pos1 | pos2 [x y z]: marca um canto (olhando um bloco, ou onde você está)",
            "/marcador info: tamanho da área | altura: do fundo ao topo do mundo",
            "/marcador expandir <n> | contrair <n>: cresce ou encolhe em todos os lados",
            "/marcador salvar <nome> | lista | carregar <nome> | apagar <nome>",
            "/marcador compartilhar <nome> <jogador> | descompartilhar <nome> <jogador>",
            "/marcador ver: liga ou desliga as partículas | limpar: descarta a seleção"};
        p.sendSystemMessage(Component.literal("Marcador de áreas").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String s : l) p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    // ---------- ações ----------

    private static String dimDe(ServerPlayer p) {
        return Corpos.dimDe((ServerLevel) p.level());
    }

    private static int marcar(ServerPlayer p, int canto, BlockPos pos, String extra) {
        Selecao s = selecaoDe(p);
        s.definir(canto, dimDe(p), pos.getX(), pos.getY(), pos.getZ());
        sujo = true;
        OCULTOS.remove(p.getUUID());
        String msg = "Canto " + canto + ": " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + extra;
        Area a = s.area();
        if (a != null) msg += "  |  " + a.larg() + " x " + a.alt() + " x " + a.comp() + " = " + Fmt.inteiro(a.volume()) + " blocos";
        aviso(p, msg, canto == 1 ? ChatFormatting.RED : ChatFormatting.AQUA);
        Jogo.som(p, "minecraft:block.note_block.hat", 1.4f);
        return 1;
    }

    private static int varinha(ServerPlayer p) {
        ItemStack st = novaVarinha();
        if (!p.getInventory().add(st)) p.drop(st, false, net.minecraft.util.Prediction.SERVER_ONLY);
        p.sendSystemMessage(Component.literal("Varinha de Marcação: clique esquerdo marca o canto 1, clique direito o canto 2.")
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int alternarVer(ServerPlayer p) {
        boolean ligou = !OCULTOS.remove(p.getUUID());
        if (ligou) OCULTOS.add(p.getUUID());
        aviso(p, ligou ? "Partículas do marcador desligadas." : "Partículas do marcador ligadas.", ChatFormatting.YELLOW);
        return 1;
    }

    private static int limpar(ServerPlayer p) {
        selecaoDe(p).limpar();
        sujo = true;
        aviso(p, "Seleção descartada.", ChatFormatting.YELLOW);
        return 1;
    }

    private static Area completa(ServerPlayer p) {
        Area a = selecaoDe(p).area();
        if (a == null) erro(p, "Marque os dois cantos primeiro (/marcador pos1 e pos2, ou a varinha).");
        return a;
    }

    private static int info(ServerPlayer p) {
        Area a = completa(p);
        if (a == null) return 0;
        p.sendSystemMessage(Component.literal("Área marcada").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        p.sendSystemMessage(Component.literal(a.resumo()).withStyle(ChatFormatting.WHITE), false);
        p.sendSystemMessage(Component.literal("Tamanho: " + a.larg() + " x " + a.alt() + " x " + a.comp() + " = " + Fmt.inteiro(a.volume()) + " blocos")
            .withStyle(ChatFormatting.GRAY), false);
        p.sendSystemMessage(Component.literal("Dimensão: " + a.dim).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int altura(ServerPlayer p) {
        Selecao s = selecaoDe(p);
        ServerLevel n = (ServerLevel) p.level();
        if (!s.altura(n.getMinY(), n.getMaxY())) return erro(p, "Marque os dois cantos primeiro.");
        sujo = true;
        aviso(p, "Área esticada do fundo ao topo do mundo.", ChatFormatting.GREEN);
        return 1;
    }

    private static int ajustar(ServerPlayer p, int n) {
        Selecao s = selecaoDe(p);
        if (!s.completa()) return erro(p, "Marque os dois cantos primeiro.");
        ServerLevel nivel = (ServerLevel) p.level();
        if (!s.ajustar(n, nivel.getMinY(), nivel.getMaxY())) return erro(p, "Não dá para encolher tanto: a área sumiria.");
        sujo = true;
        Area a = s.area();
        aviso(p, (n > 0 ? "Expandida" : "Contraída") + ": " + a.larg() + " x " + a.alt() + " x " + a.comp() + " blocos.", ChatFormatting.GREEN);
        return 1;
    }

    private static int salvar(ServerPlayer p, String nome) {
        Area a = selecaoDe(p).area();
        String err = dados.salvar(p.getUUID().toString(), p.getGameProfile().name(), nome, a, System.currentTimeMillis());
        if (err != null) return erro(p, err);
        sujo = true;
        aviso(p, "Área \"" + nome + "\" salva (" + Fmt.inteiro(a.volume()) + " blocos).", ChatFormatting.GREEN);
        return 1;
    }

    private static int lista(ServerPlayer p) {
        String eu = p.getUUID().toString();
        List<Area> todas = dados.visiveis(eu);
        if (todas.isEmpty()) {
            p.sendSystemMessage(Component.literal("Você não tem áreas salvas. Use /marcador salvar <nome>.").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        p.sendSystemMessage(Component.literal("Suas áreas").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (Area a : todas) {
            boolean minha = a.dono.equals(eu);
            String ref = minha ? a.nome : a.donoNome + ":" + a.nome;
            String extra = minha ? (a.partilha.isEmpty() ? "" : "  [compartilhada com " + a.partilha.size() + "]") : "  [de " + a.donoNome + "]";
            p.sendSystemMessage(Component.literal(ref).withStyle(ChatFormatting.AQUA)
                .append(Component.literal("  " + a.larg() + "x" + a.alt() + "x" + a.comp() + " (" + Fmt.inteiro(a.volume()) + ")" + extra).withStyle(ChatFormatting.GRAY)), false);
        }
        return todas.size();
    }

    private static int carregarArea(ServerPlayer p, String ref) {
        Area a = dados.buscar(p.getUUID().toString(), ref);
        if (a == null) return erro(p, "Não achei a área \"" + ref + "\" (se for de outra pessoa, use dono:nome).");
        selecaoDe(p).carregar(a);
        sujo = true;
        OCULTOS.remove(p.getUUID());
        aviso(p, "Área \"" + a.nome + "\" carregada: " + a.larg() + " x " + a.alt() + " x " + a.comp() + (a.dim.equals(dimDe(p)) ? "" : " (em outra dimensão: " + a.dim + ")"),
            ChatFormatting.GREEN);
        return 1;
    }

    private static int apagar(ServerPlayer p, String nome) {
        if (!dados.apagar(p.getUUID().toString(), nome)) return erro(p, "Você não tem uma área chamada \"" + nome + "\".");
        sujo = true;
        aviso(p, "Área \"" + nome + "\" apagada.", ChatFormatting.YELLOW);
        return 1;
    }

    private static String uuidDe(String nick) {
        for (var e : Jogo.casa.conhecidos.entrySet()) if (e.getValue().equalsIgnoreCase(nick)) return e.getKey();
        return null;
    }

    private static int compartilhar(ServerPlayer p, String nome, String nick, boolean dar) {
        String outro = uuidDe(nick);
        if (outro == null) return erro(p, "Não conheço o jogador \"" + nick + "\".");
        String err = dar ? dados.compartilhar(p.getUUID().toString(), nome, outro) : dados.descompartilhar(p.getUUID().toString(), nome, outro);
        if (err != null) return erro(p, err);
        sujo = true;
        aviso(p, dar ? "Área \"" + nome + "\" compartilhada com " + nick + "." : "Área \"" + nome + "\" não é mais compartilhada com " + nick + ".", ChatFormatting.GREEN);
        if (dar) {
            ServerPlayer alvo = Jogo.server.getPlayerList().getPlayer(UUID.fromString(outro));
            if (alvo != null) alvo.sendSystemMessage(Component.literal(p.getGameProfile().name() + " compartilhou a área \"" + nome + "\" com você. Use /marcador carregar "
                + p.getGameProfile().name() + ":" + nome).withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static void aviso(ServerPlayer p, String s, ChatFormatting cor) {
        p.sendSystemMessage(Component.literal(s).withStyle(cor), true);
    }

    private static int erro(ServerPlayer p, String s) {
        aviso(p, s, ChatFormatting.RED);
        return 0;
    }

    // ---------- eventos e partículas ----------

    public static void registrarEventos() {
        AttackBlockCallback.EVENT.register((p, n, mao, pos, dir) -> {
            if (!ehVarinha(p.getItemInHand(mao))) return InteractionResult.PASS;
            if (!n.isClientSide() && p instanceof ServerPlayer sp && mao == InteractionHand.MAIN_HAND) {
                Selecao s = selecaoDe(sp);
                int[] atual = s.p1;
                boolean igual = atual != null && s.dim.equals(dimDe(sp)) && atual[0] == pos.getX() && atual[1] == pos.getY() && atual[2] == pos.getZ();
                if (!igual) marcar(sp, 1, pos, "");
            }
            return InteractionResult.FAIL;
        });
        UseBlockCallback.EVENT.register((p, n, mao, hit) -> {
            if (!ehVarinha(p.getItemInHand(mao))) return InteractionResult.PASS;
            if (mao != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
            if (!n.isClientSide() && p instanceof ServerPlayer sp) marcar(sp, 2, hit.getBlockPos(), "");
            return InteractionResult.SUCCESS;
        });
    }

    /** A cada meio segundo: desenha os limites para quem tem uma seleção. */
    public static void aCadaTick(int tick) {
        if (tick % 10 == 0 && Jogo.server != null) {
            for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) desenhar(p);
        }
    }

    public static void aCadaSegundo() {
        if (sujo) salvarAgora();
    }

    private static void desenhar(ServerPlayer p) {
        if (OCULTOS.contains(p.getUUID())) return;
        var j = dados.jogadores.get(p.getUUID().toString());
        if (j == null) return;
        Selecao s = j.atual;
        if (s.p1 == null && s.p2 == null) return;
        ServerLevel n = (ServerLevel) p.level();
        if (!s.dim.equals(Corpos.dimDe(n))) return;
        if (s.completa()) {
            Area a = s.area();
            // não gasta partícula com uma área longe demais para o jogador ver
            if (distancia(p, a) > DIST_MAX) return;
            enviar(p, n, MarcadorDados.arestas(a, 900), COR_AREA, true);
        }
        if (s.p1 != null) enviar(p, n, MarcadorDados.arestas(unidade(s.p1), 64), COR_P1, false);
        if (s.p2 != null) enviar(p, n, MarcadorDados.arestas(unidade(s.p2), 64), COR_P2, false);
    }

    private static Area unidade(int[] c) {
        Area a = new Area();
        a.x0 = a.x1 = c[0];
        a.y0 = a.y1 = c[1];
        a.z0 = a.z1 = c[2];
        return a;
    }

    private static double distancia(ServerPlayer p, Area a) {
        double dx = Math.max(Math.max(a.x0 - p.getX(), 0), p.getX() - (a.x1 + 1));
        double dy = Math.max(Math.max(a.y0 - p.getY(), 0), p.getY() - (a.y1 + 1));
        double dz = Math.max(Math.max(a.z0 - p.getZ(), 0), p.getZ() - (a.z1 + 1));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void enviar(ServerPlayer p, ServerLevel n, List<double[]> pontos, DustParticleOptions cor, boolean limitar) {
        int enviados = 0;
        double max2 = DIST_MAX * DIST_MAX;
        for (double[] q : pontos) {
            if (limitar) {
                double dx = q[0] - p.getX(), dy = q[1] - p.getY(), dz = q[2] - p.getZ();
                if (dx * dx + dy * dy + dz * dz > max2) continue;
                if (enviados >= MAX_PONTOS) break;
            }
            n.sendParticles(p, cor, true, true, q[0], q[1], q[2], 1, 0.0, 0.0, 0.0, 0.0);
            enviados++;
        }
    }
}
