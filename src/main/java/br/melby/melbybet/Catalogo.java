package br.melby.melbybet;

import br.melby.melbybet.core.ContagemBlocos;
import br.melby.melbybet.core.ContagemBlocos.Contagem;
import br.melby.melbybet.core.ContagemBlocos.Projeto;
import br.melby.melbybet.core.Fmt;
import br.melby.melbybet.core.MarcadorDados.Area;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /catalogo: conta os blocos da área marcada com /marcador, tipo por tipo, com porcentagem e pilhas. Lê aos poucos
 * (algumas dezenas de milhares de blocos por tick) para não travar o servidor, e só os chunks carregados.
 */
public final class Catalogo {
    private Catalogo() {}

    /** Maior área aceita, em blocos. */
    public static final long MAX_VOLUME = 2_000_000L;
    private static final int POR_TICK = 25_000;

    private static ContagemBlocos dados = new ContagemBlocos();
    private static Path arq;
    private static boolean sujo;

    private static final class Varredura {
        final UUID dono;
        final ServerLevel nivel;
        final Area area;
        final boolean tudo;
        final Contagem res = new Contagem();
        long i;
        final long volume;

        Varredura(UUID dono, ServerLevel nivel, Area area, boolean tudo) {
            this.dono = dono;
            this.nivel = nivel;
            this.area = area;
            this.tudo = tudo;
            this.volume = area.volume();
        }
    }

    private static final Map<UUID, Varredura> ATIVAS = new HashMap<>();
    /** O último resultado de cada jogador, para as páginas e para salvar. */
    private static final Map<UUID, Projeto> ULTIMO = new HashMap<>();

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("catalogo.json");
        dados = ContagemBlocos.ler(arq);
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

    public static void aCadaSegundo() {
        if (sujo) salvarAgora();
    }

    public static void aoSair(ServerPlayer p) {
        ATIVAS.remove(p.getUUID());
        ULTIMO.remove(p.getUUID());
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("catalogo")
            .then(literal("tudo").executes(c -> iniciar(c.getSource().getPlayerOrException(), true)))
            .then(literal("pagina").then(argument("n", IntegerArgumentType.integer(1))
                .executes(c -> pagina(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n")))))
            .then(literal("cancelar").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                if (ATIVAS.remove(p.getUUID()) == null) return erro(p, "Não há contagem em andamento.");
                aviso(p, "Contagem cancelada.", ChatFormatting.YELLOW);
                return 1;
            }))
            .then(literal("salvar").then(argument("nome", StringArgumentType.word())
                .executes(c -> salvar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome")))))
            .then(literal("lista").executes(c -> lista(c.getSource().getPlayerOrException())))
            .then(literal("ver").then(argument("nome", StringArgumentType.word()).suggests((c, b) -> sugerir(c.getSource(), b))
                .executes(c -> ver(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome")))))
            .then(literal("apagar").then(argument("nome", StringArgumentType.word()).suggests((c, b) -> sugerir(c.getSource(), b))
                .executes(c -> apagar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome")))))
            .executes(c -> iniciar(c.getSource().getPlayerOrException(), false)));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> sugerir(CommandSourceStack s,
            com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        if (!(s.getEntity() instanceof ServerPlayer p)) return b.buildFuture();
        return SharedSuggestionProvider.suggest(dados.lista(p.getUUID().toString()).stream().map(x -> x.nome).toList(), b);
    }

    private static int iniciar(ServerPlayer p, boolean tudo) {
        Area a = Marcadores.selecaoDe(p).area();
        if (a == null) return erro(p, "Marque a área primeiro com /marcador (varinha ou pos1 e pos2).");
        ServerLevel n = (ServerLevel) p.level();
        if (!a.dim.equals(Corpos.dimDe(n))) return erro(p, "A área marcada está em outra dimensão (" + a.dim + ").");
        if (a.volume() > MAX_VOLUME) return erro(p, "Área grande demais: " + Fmt.inteiro(a.volume()) + " blocos (máximo " + Fmt.inteiro(MAX_VOLUME) + ").");
        if (ATIVAS.containsKey(p.getUUID())) return erro(p, "Já estou contando para você. /catalogo cancelar para parar.");
        ATIVAS.put(p.getUUID(), new Varredura(p.getUUID(), n, a, tudo));
        aviso(p, "Contando " + Fmt.inteiro(a.volume()) + " blocos...", ChatFormatting.YELLOW);
        return 1;
    }

    public static void aCadaTick() {
        if (ATIVAS.isEmpty()) return;
        var it = ATIVAS.values().iterator();
        while (it.hasNext()) {
            Varredura v = it.next();
            ServerPlayer p = Jogo.server.getPlayerList().getPlayer(v.dono);
            if (p == null) {
                it.remove();
                continue;
            }
            if (avancar(v)) {
                it.remove();
                terminar(p, v);
            }
        }
    }

    /** Lê até POR_TICK posições. Devolve true quando acabou. */
    private static boolean avancar(Varredura v) {
        Area a = v.area;
        long lx = a.larg(), lz = a.comp();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int feitos = 0;
        long camada = lx * lz;
        int ultimoCx = Integer.MIN_VALUE, ultimoCz = Integer.MIN_VALUE;
        boolean carregado = false;
        while (v.i < v.volume && feitos < POR_TICK) {
            long k = v.i++;
            int y = a.y0 + (int) (k / camada);
            long r = k % camada;
            int z = a.z0 + (int) (r / lx);
            int x = a.x0 + (int) (r % lx);
            feitos++;
            int cx = x >> 4, cz = z >> 4;
            if (cx != ultimoCx || cz != ultimoCz) {
                ultimoCx = cx;
                ultimoCz = cz;
                carregado = v.nivel.hasChunk(cx, cz);
            }
            if (!carregado) {
                v.res.puladas++;
                continue;
            }
            mp.set(x, y, z);
            BlockState st = v.nivel.getBlockState(mp);
            contar(v, st);
        }
        return v.i >= v.volume;
    }

    private static void contar(Varredura v, BlockState st) {
        if (st.isAir() && !v.tudo) return;
        var bloco = st.getBlock();
        if (!v.tudo && bloco instanceof LiquidBlock) return;
        // portas e plantas altas ocupam dois blocos mas são um item; a cama também
        if (st.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && st.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return;
        if (bloco instanceof BedBlock && st.getValue(BedBlock.PART) == BedPart.HEAD) return;
        long n = 1;
        if (bloco instanceof SlabBlock && st.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) n = 2;
        String id = BuiltInRegistries.BLOCK.getKey(bloco).getPath();
        v.res.somar(id, n);
    }

    private static void terminar(ServerPlayer p, Varredura v) {
        Projeto pr = new Projeto();
        pr.dim = v.area.dim;
        pr.area = v.area.resumo();
        pr.larg = v.area.larg();
        pr.alt = v.area.alt();
        pr.comp = v.area.comp();
        pr.contagem = v.res;
        ULTIMO.put(p.getUUID(), pr);
        mostrar(p, pr, 1);
    }

    private static int pagina(ServerPlayer p, int n) {
        Projeto pr = ULTIMO.get(p.getUUID());
        if (pr == null) return erro(p, "Faça uma contagem primeiro (/catalogo).");
        if (n > pr.contagem.paginas()) return erro(p, "Só há " + pr.contagem.paginas() + " página(s).");
        mostrar(p, pr, n);
        return 1;
    }

    private static Component nomeBloco(String id) {
        return Component.translatable("block.minecraft." + id);
    }

    private static void mostrar(ServerPlayer p, Projeto pr, int pag) {
        Contagem c = pr.contagem;
        p.sendSystemMessage(Component.literal("Catálogo de blocos").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal("  " + pr.larg + " x " + pr.alt + " x " + pr.comp + (pr.nome.isEmpty() ? "" : "  (" + pr.nome + ")")).withStyle(ChatFormatting.GRAY)), false);
        if (c.blocos.isEmpty()) {
            p.sendSystemMessage(Component.literal("Nenhum bloco encontrado" + (c.puladas > 0 ? " (chunks não carregados)." : ".")).withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        for (var e : c.pagina(pag)) {
            int tam = tamanhoPilha(e.getKey());
            p.sendSystemMessage(Component.literal(Fmt.inteiro(e.getValue()) + " ").withStyle(ChatFormatting.AQUA)
                .append(nomeBloco(e.getKey()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  " + ContagemBlocos.percentual(e.getValue(), c.total) + "  (" + ContagemBlocos.pilhas(e.getValue(), tam) + ")").withStyle(ChatFormatting.GRAY)), false);
        }
        p.sendSystemMessage(Component.literal("Total: " + Fmt.inteiro(c.total) + " blocos em " + c.blocos.size() + " tipos. Página " + pag + "/" + c.paginas()
            + (c.paginas() > 1 ? " (/catalogo pagina <n>)" : "") + ". /catalogo salvar <nome> guarda.").withStyle(ChatFormatting.GRAY), false);
        if (c.puladas > 0) {
            p.sendSystemMessage(Component.literal("Atenção: " + Fmt.inteiro(c.puladas) + " posições estavam em chunks não carregados e ficaram de fora. Chegue perto e repita.")
                .withStyle(ChatFormatting.RED), false);
        }
    }

    /** Tamanho da pilha do item daquele bloco (a maioria é 64; placas, baldes etc. diferem). */
    private static int tamanhoPilha(String id) {
        try {
            var bloco = BuiltInRegistries.BLOCK.stream().filter(b -> BuiltInRegistries.BLOCK.getKey(b).getPath().equals(id)).findFirst().orElse(null);
            if (bloco == null) return 64;
            int t = new net.minecraft.world.item.ItemStack(bloco.asItem()).getMaxStackSize();
            return t <= 0 ? 64 : t;
        } catch (Exception e) {
            return 64;
        }
    }

    private static int salvar(ServerPlayer p, String nome) {
        Projeto pr = ULTIMO.get(p.getUUID());
        if (pr == null) return erro(p, "Faça uma contagem primeiro (/catalogo).");
        Projeto copia = new Projeto();
        copia.dim = pr.dim;
        copia.area = pr.area;
        copia.larg = pr.larg;
        copia.alt = pr.alt;
        copia.comp = pr.comp;
        copia.contagem = pr.contagem;
        String err = dados.salvar(p.getUUID().toString(), nome, copia, System.currentTimeMillis());
        if (err != null) return erro(p, err);
        sujo = true;
        pr.nome = nome;
        aviso(p, "Projeto \"" + nome + "\" salvo.", ChatFormatting.GREEN);
        return 1;
    }

    private static int lista(ServerPlayer p) {
        var l = dados.lista(p.getUUID().toString());
        if (l.isEmpty()) return erro(p, "Você não tem projetos salvos. /catalogo salvar <nome> depois de contar.");
        p.sendSystemMessage(Component.literal("Seus projetos").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (Projeto x : l) {
            p.sendSystemMessage(Component.literal(x.nome).withStyle(ChatFormatting.AQUA).append(Component.literal("  " + x.larg + "x" + x.alt + "x" + x.comp
                + ", " + Fmt.inteiro(x.contagem.total) + " blocos, " + x.contagem.blocos.size() + " tipos").withStyle(ChatFormatting.GRAY)), false);
        }
        return l.size();
    }

    private static int ver(ServerPlayer p, String nome) {
        Projeto pr = dados.obter(p.getUUID().toString(), nome);
        if (pr == null) return erro(p, "Não tenho um projeto chamado \"" + nome + "\".");
        ULTIMO.put(p.getUUID(), pr);
        mostrar(p, pr, 1);
        return 1;
    }

    private static int apagar(ServerPlayer p, String nome) {
        if (!dados.apagar(p.getUUID().toString(), nome)) return erro(p, "Não tenho um projeto chamado \"" + nome + "\".");
        sujo = true;
        aviso(p, "Projeto \"" + nome + "\" apagado.", ChatFormatting.YELLOW);
        return 1;
    }

    private static void aviso(ServerPlayer p, String s, ChatFormatting cor) {
        p.sendSystemMessage(Component.literal(s).withStyle(cor), true);
    }

    private static int erro(ServerPlayer p, String s) {
        aviso(p, s, ChatFormatting.RED);
        return 0;
    }
}
