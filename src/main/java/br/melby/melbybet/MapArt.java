package br.melby.melbybet;

import br.melby.melbybet.core.ContagemBlocos;
import br.melby.melbybet.core.Fmt;
import br.melby.melbybet.core.MapArtGeo;
import br.melby.melbybet.core.MapArtGeo.Conta;
import br.melby.melbybet.core.MapArtGeo.Projeto;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.nio.file.Path;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /mapart: ajuda a planejar mapas-arte. Mostra a grade de mapas do jogo (onde cada mapa começa e termina), calcula quantos
 * mapas e blocos um mural pede, marca a área com o /marcador e guarda projetos. Não precisa de nada no cliente.
 */
public final class MapArt {
    private MapArt() {}

    private static MapArtGeo dados = new MapArtGeo();
    private static Path arq;
    private static boolean sujo;

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("mapart.json");
        dados = MapArtGeo.ler(arq);
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

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Integer> escala() {
        return argument("escala", IntegerArgumentType.integer(0, MapArtGeo.MAX_ESCALA));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("mapart")
            .then(literal("calc").then(argument("largura", IntegerArgumentType.integer(1)).then(argument("altura", IntegerArgumentType.integer(1))
                .then(escala().executes(c -> calc(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "largura"),
                    IntegerArgumentType.getInteger(c, "altura"), IntegerArgumentType.getInteger(c, "escala"))))
                .executes(c -> calc(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "largura"),
                    IntegerArgumentType.getInteger(c, "altura"), 0)))))
            .then(literal("grade")
                .then(escala().executes(c -> grade(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "escala"))))
                .executes(c -> grade(c.getSource().getPlayerOrException(), 0)))
            .then(literal("marcar")
                .then(escala().executes(c -> marcar(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "escala"))))
                .executes(c -> marcar(c.getSource().getPlayerOrException(), 0)))
            .then(literal("info").executes(c -> info(c.getSource().getPlayerOrException())))
            .then(literal("projeto").then(argument("nome", StringArgumentType.word()).then(argument("largura", IntegerArgumentType.integer(1))
                .then(argument("altura", IntegerArgumentType.integer(1))
                    .then(escala().executes(c -> projeto(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome"),
                        IntegerArgumentType.getInteger(c, "largura"), IntegerArgumentType.getInteger(c, "altura"), IntegerArgumentType.getInteger(c, "escala"))))
                    .executes(c -> projeto(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome"),
                        IntegerArgumentType.getInteger(c, "largura"), IntegerArgumentType.getInteger(c, "altura"), 0))))))
            .then(literal("esconder").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                ConstrucaoServidor.enviar(p, new ConstrucaoRede.MapArtPayload(false, "overworld", 0, 0, 0, 0, 0, 0));
                p.sendSystemMessage(Component.literal("Grade de mapas escondida.").withStyle(ChatFormatting.YELLOW), true);
                return 1;
            }))
            .then(literal("lista").executes(c -> lista(c.getSource().getPlayerOrException())))
            .then(literal("ver").then(argument("nome", StringArgumentType.word()).suggests((c, b) -> sugerir(c.getSource(), b))
                .executes(c -> ver(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome")))))
            .then(literal("apagar").then(argument("nome", StringArgumentType.word()).suggests((c, b) -> sugerir(c.getSource(), b))
                .executes(c -> apagar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "nome")))))
            .executes(c -> {
                ajuda(c.getSource().getPlayerOrException());
                return 1;
            }));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> sugerir(CommandSourceStack s,
            com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        if (!(s.getEntity() instanceof ServerPlayer p)) return b.buildFuture();
        return SharedSuggestionProvider.suggest(dados.lista(p.getUUID().toString()).stream().map(x -> x.nome).toList(), b);
    }

    private static void ajuda(ServerPlayer p) {
        String[] l = {
            "/mapart calc <largura> <altura> [escala]: quantos mapas e blocos um mural pede (tamanho em mapas)",
            "/mapart grade [escala]: onde começa e termina o mapa em que você está",
            "/mapart marcar [escala]: marca com o /marcador o mapa em que você está",
            "/mapart info: lê o mapa que você segura (escala, centro e área coberta)",
            "/mapart projeto <nome> <largura> <altura> [escala]: guarda um mural a partir do mapa em que você está (vai para leste e sul) e marca a área",
            "/mapart lista | ver <nome> | apagar <nome>"};
        p.sendSystemMessage(Component.literal("Mapas-arte").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String s : l) p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.GRAY), false);
    }

    private static void linha(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.WHITE), false);
    }

    private static int calc(ServerPlayer p, int l, int a, int escala) {
        String err = MapArtGeo.validar(l, a, escala);
        if (err != null) return erro(p, err);
        Conta c = MapArtGeo.calcular(l, a, escala);
        p.sendSystemMessage(Component.literal("Mural de " + l + " x " + a + " mapas (escala " + escala + ")").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        linha(p, "Mapas: " + c.mapas + "  (precisa de " + c.mapas + " mapas cheios e " + c.mapas + " molduras)");
        linha(p, "Cobre: " + Fmt.inteiro(c.largBlocos) + " x " + Fmt.inteiro(c.altBlocos) + " blocos (" + Fmt.inteiro(c.areaBlocos) + " blocos de área)");
        linha(p, "Pixels: " + Fmt.inteiro(c.pixels) + " (128 x 128 por mapa)");
        linha(p, "Blocos, mural plano (uma cor só): " + Fmt.inteiro(c.blocosPlano) + "  = " + ContagemBlocos.pilhas(c.blocosPlano, 64));
        linha(p, "Blocos com relevo (3 tons): " + Fmt.inteiro(c.blocosRelevo) + "  = " + ContagemBlocos.pilhas(c.blocosRelevo, 64) + "  (inclui 1 linha extra ao norte)");
        if (escala > 0) {
            p.sendSystemMessage(Component.literal("Em escala " + escala + " cada pixel resume um quadrado de " + (1 << escala) + " x " + (1 << escala)
                + " blocos. Quem faz mapa-arte costuma usar escala 0.").withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int[] celulaAtual(ServerPlayer p, int escala) {
        return new int[] {MapArtGeo.celula(p.getX(), escala), MapArtGeo.celula(p.getZ(), escala)};
    }

    private static int grade(ServerPlayer p, int escala) {
        int[] c = celulaAtual(p, escala);
        p.sendSystemMessage(Component.literal("Mapa em que você está (escala " + escala + ")").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        linha(p, "x: " + MapArtGeo.inicio(c[0], escala) + " até " + MapArtGeo.fim(c[0], escala));
        linha(p, "z: " + MapArtGeo.inicio(c[1], escala) + " até " + MapArtGeo.fim(c[1], escala));
        linha(p, "Centro: " + MapArtGeo.centro(c[0], escala) + ", " + MapArtGeo.centro(c[1], escala) + "  (o mapa criado aí vai ficar alinhado)");
        p.sendSystemMessage(Component.literal("Um mapa criado em qualquer ponto dessa área cobre exatamente ela. /mapart marcar mostra os limites.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int marcar(ServerPlayer p, int escala) {
        int[] c = celulaAtual(p, escala);
        return aplicarMarcador(p, c[0], c[1], 1, 1, escala, "Mapa marcado");
    }

    private static int aplicarMarcador(ServerPlayer p, int gx, int gz, int l, int a, int escala, String titulo) {
        int y = p.blockPosition().getY();
        int x0 = MapArtGeo.inicio(gx, escala), z0 = MapArtGeo.inicio(gz, escala);
        int x1 = MapArtGeo.fim(gx + l - 1, escala), z1 = MapArtGeo.fim(gz + a - 1, escala);
        Marcadores.definirArea(p, x0, y, z0, x1, y, z1);
        ConstrucaoServidor.enviar(p, new ConstrucaoRede.MapArtPayload(true, Corpos.dimDe((ServerLevel) p.level()), escala, x0, z0, l, a, y));
        p.sendSystemMessage(Component.literal(titulo + ": x " + x0 + ".." + x1 + ", z " + z0 + ".." + z1 + ". Os limites aparecem em partículas; /marcador altura estica para cima e para baixo.")
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static ItemStack mapaNaMao(ServerPlayer p) {
        ItemStack a = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (a.is(Items.FILLED_MAP)) return a;
        ItemStack b = p.getItemInHand(InteractionHand.OFF_HAND);
        return b.is(Items.FILLED_MAP) ? b : null;
    }

    private static int info(ServerPlayer p) {
        ItemStack st = mapaNaMao(p);
        if (st == null) return erro(p, "Segure um mapa (já preenchido) na mão.");
        MapItemSavedData m = MapItem.getSavedData(st, p.level());
        if (m == null) return erro(p, "Não achei os dados desse mapa.");
        int escala = m.scale;
        int lado = MapArtGeo.lado(escala);
        int x0 = m.centerX - lado / 2, z0 = m.centerZ - lado / 2;
        p.sendSystemMessage(Component.literal("Mapa na mão").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        linha(p, "Escala " + escala + ": cobre " + lado + " x " + lado + " blocos (cada pixel = " + (1 << escala) + " x " + (1 << escala) + " blocos)");
        linha(p, "Centro: " + m.centerX + ", " + m.centerZ);
        linha(p, "Área: x " + x0 + ".." + (x0 + lado - 1) + ", z " + z0 + ".." + (z0 + lado - 1));
        linha(p, "Dimensão: " + (m.dimension == p.level().dimension() ? "a mesma em que você está" : "outra (" + m.dimension.toString() + ")")
            + (m.locked ? "  |  travado (trancado com vidro)" : "  |  destravado (ainda muda quando alguém passa)"));
        return 1;
    }

    private static int projeto(ServerPlayer p, String nome, int l, int a, int escala) {
        int[] c = celulaAtual(p, escala);
        Projeto pr = new Projeto();
        pr.nome = nome;
        pr.dim = Corpos.dimDe((ServerLevel) p.level());
        pr.escala = escala;
        pr.gx = c[0];
        pr.gz = c[1];
        pr.largM = l;
        pr.altM = a;
        String err = dados.salvar(p.getUUID().toString(), pr, System.currentTimeMillis());
        if (err != null) return erro(p, err);
        sujo = true;
        mostrar(p, pr);
        return aplicarMarcador(p, pr.gx, pr.gz, l, a, escala, "Projeto \"" + nome + "\" salvo e marcado");
    }

    private static void mostrar(ServerPlayer p, Projeto pr) {
        Conta c = MapArtGeo.calcular(pr.largM, pr.altM, pr.escala);
        p.sendSystemMessage(Component.literal("Projeto " + pr.nome).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal("  " + pr.largM + " x " + pr.altM + " mapas, escala " + pr.escala + ", " + pr.dim).withStyle(ChatFormatting.GRAY)), false);
        linha(p, "Área: x " + pr.x0() + ".." + pr.x1() + ", z " + pr.z0() + ".." + pr.z1() + "  (" + Fmt.inteiro(c.largBlocos) + " x " + Fmt.inteiro(c.altBlocos) + " blocos)");
        linha(p, c.mapas + " mapas  |  plano " + ContagemBlocos.pilhas(c.blocosPlano, 64) + "  |  com relevo " + ContagemBlocos.pilhas(c.blocosRelevo, 64));
        if (pr.largM * pr.altM <= 12) {
            StringBuilder sb = new StringBuilder("Centros dos mapas (leste e sul): ");
            for (int j = 0; j < pr.altM; j++) {
                for (int i = 0; i < pr.largM; i++) sb.append("(").append(MapArtGeo.centro(pr.gx + i, pr.escala)).append(",").append(MapArtGeo.centro(pr.gz + j, pr.escala)).append(") ");
            }
            p.sendSystemMessage(Component.literal(sb.toString().trim()).withStyle(ChatFormatting.DARK_GRAY), false);
        }
    }

    private static int lista(ServerPlayer p) {
        var l = dados.lista(p.getUUID().toString());
        if (l.isEmpty()) return erro(p, "Você não tem projetos. /mapart projeto <nome> <largura> <altura>");
        p.sendSystemMessage(Component.literal("Seus projetos de mapa-arte").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (Projeto x : l) {
            p.sendSystemMessage(Component.literal(x.nome).withStyle(ChatFormatting.AQUA).append(Component.literal("  " + x.largM + "x" + x.altM + " mapas, escala " + x.escala
                + ", x " + x.x0() + " z " + x.z0()).withStyle(ChatFormatting.GRAY)), false);
        }
        return l.size();
    }

    private static int ver(ServerPlayer p, String nome) {
        Projeto pr = dados.obter(p.getUUID().toString(), nome);
        if (pr == null) return erro(p, "Não tenho um projeto chamado \"" + nome + "\".");
        mostrar(p, pr);
        if (pr.dim.equals(Corpos.dimDe((ServerLevel) p.level()))) return aplicarMarcador(p, pr.gx, pr.gz, pr.largM, pr.altM, pr.escala, "Área marcada");
        p.sendSystemMessage(Component.literal("O projeto é de outra dimensão (" + pr.dim + "), então não marquei a área.").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int apagar(ServerPlayer p, String nome) {
        if (!dados.apagar(p.getUUID().toString(), nome)) return erro(p, "Não tenho um projeto chamado \"" + nome + "\".");
        sujo = true;
        p.sendSystemMessage(Component.literal("Projeto \"" + nome + "\" apagado.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int erro(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED), true);
        return 0;
    }
}
