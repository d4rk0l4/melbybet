package br.melby.melbybet;

import br.melby.melbybet.core.FotoDados;
import br.melby.melbybet.core.FotoDados.Grupo;
import br.melby.melbybet.core.ImagemMapa;
import br.melby.melbybet.core.UrlSegura;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /fotomoldura &lt;LxA&gt; &lt;endereço&gt; [modo]: baixa uma imagem (só HTTPS), converte para as cores dos mapas, divide em mapas
 * de 128x128 e os coloca em molduras invisíveis na parede para a qual o jogador olha. Tudo com mapas e molduras do jogo, então
 * quem entra no servidor vê sem instalar nada. O download e a conversão rodam fora da thread principal.
 */
public final class FotoMoldura {
    private FotoMoldura() {}

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    /** Longe de qualquer jogador: um mapa com esse centro nunca é "repintado" com o terreno por quem o carrega. */
    private static final int CENTRO_LONGE = 29_000_000;

    private static FotoDados dados = new FotoDados();
    private static Path arq;
    private static boolean sujo;
    private static final Set<UUID> OCUPADOS = new HashSet<>();

    private static final HttpClient CLIENTE = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "melbycraft-fotomoldura");
        t.setDaemon(true);
        return t;
    });

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("fotomolduras.json");
        dados = FotoDados.ler(arq);
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
        UPLOADS.remove(p.getUUID());
    }

    // ---------- comandos ----------

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("fotomoldura")
            .then(literal("lista").executes(c -> lista(c.getSource().getPlayerOrException())))
            .then(literal("info").executes(c -> info(c.getSource().getPlayerOrException())))
            .then(literal("reparar").executes(c -> reparar(c.getSource().getPlayerOrException())))
            .then(literal("desfazer").executes(c -> desfazer(c.getSource().getPlayerOrException())))
            .then(literal("apagar").executes(c -> apagar(c.getSource().getPlayerOrException(), -1))
                .then(argument("id", IntegerArgumentType.integer(1)).executes(c -> apagar(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "id")))))
            .then(argument("tamanho", StringArgumentType.word()).then(argument("endereco", StringArgumentType.greedyString())
                .executes(c -> criar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "tamanho"), StringArgumentType.getString(c, "endereco")))))
            .executes(c -> {
                ajuda(c.getSource().getPlayerOrException());
                return 1;
            }));
    }

    private static void ajuda(ServerPlayer p) {
        String[] l = {
            "/fotomoldura <LxA> <endereço> [modo]: põe uma foto na parede para a qual você olha. LxA é o tamanho em molduras (até 6x6), por exemplo 3x2",
            "O endereço precisa ser https:// direto para a imagem (PNG, JPG, GIF ou BMP; WebP não funciona).",
            "Modo: cortar (padrão, preenche e corta as bordas), ajustar (mostra tudo, com faixas pretas) ou esticar.",
            "Olhe para o bloco que será o canto inferior esquerdo da foto: ela cresce para a direita e para cima, na frente da parede.",
            "/fotomoldura lista | info | reparar | desfazer | apagar [id]  (info, reparar e apagar valem para o mural para o qual você olha)"};
        p.sendSystemMessage(Component.literal("Fotos em molduras").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String s : l) p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.GRAY), false);
    }

    // ---------- criar ----------

    private record Pedido(UUID jogador, String nomeJogador, boolean op, String dim, BlockPos base, Direction normal, int larg, int alt, String url, String modo) {}

    private static int criar(ServerPlayer p, String tamanho, String resto) {
        int[] t = FotoDados.tamanho(tamanho);
        if (t == null) return erro(p, "Tamanho inválido. Use LxA com 1 a " + FotoDados.MAX_LADO + ", por exemplo 3x2.");
        String[] partes = resto.trim().split("\\s+");
        String url = partes[0];
        String modo = ImagemMapa.CORTAR;
        if (partes.length > 2) return erro(p, "Use: /fotomoldura <LxA> <endereço> [cortar|ajustar|esticar]");
        if (partes.length == 2) {
            modo = partes[1].toLowerCase();
            if (!modo.equals(ImagemMapa.CORTAR) && !modo.equals(ImagemMapa.AJUSTAR) && !modo.equals(ImagemMapa.ESTICAR)) return erro(p, "Modo desconhecido. Use cortar, ajustar ou esticar.");
        }
        String err = UrlSegura.validar(url);
        if (err != null) return erro(p, err);
        boolean op = Comandos.admin(p);
        err = dados.podeCriar(p.getUUID().toString(), op, t[0] * t[1]);
        if (err != null) return erro(p, err);
        if (OCUPADOS.contains(p.getUUID())) return erro(p, "Ainda estou preparando a sua foto anterior. Espere terminar.");
        HitResult h = p.pick(6.0, 1.0F, false);
        if (!(h instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK || !bh.getDirection().getAxis().isHorizontal()) {
            return erro(p, "Olhe para o lado de um bloco da parede (a até 6 blocos): ele será o canto inferior esquerdo da foto.");
        }
        BlockPos base = bh.getBlockPos().relative(bh.getDirection());
        Pedido ped = new Pedido(p.getUUID(), p.getGameProfile().name(), op, Corpos.dimDe((ServerLevel) p.level()), base, bh.getDirection(), t[0], t[1], url, modo);
        OCUPADOS.add(p.getUUID());
        p.sendSystemMessage(Component.literal("Baixando e preparando a foto (" + t[0] + "x" + t[1] + ")... pode levar alguns segundos.").withStyle(ChatFormatting.YELLOW), false);
        EXEC.submit(() -> {
            try {
                byte[] bytes = baixar(ped.url);
                BufferedImage img = ImagemMapa.ler(bytes);
                byte[][] mapas = ImagemMapa.preparar(img, ped.larg, ped.alt, ped.modo, true);
                Jogo.server.execute(() -> colocar(ped, mapas));
            } catch (Exception e) {
                String msg = e instanceof IllegalArgumentException || e instanceof IOException ? e.getMessage() : "Não consegui baixar a imagem (" + e.getClass().getSimpleName() + ").";
                Melbybet.LOGGER.info("fotomoldura de {} falhou: {}", ped.nomeJogador, e.toString());
                Jogo.server.execute(() -> terminar(ped, msg));
            }
        });
        return 1;
    }


    // ---------- imagem enviada pelo cliente (arquivo do PC) ----------

    private static final class Upload {
        int sessao, total, larg, alt, bytes;
        String modo;
        byte[][] partes;
        int recebidas;
        long inicio = System.currentTimeMillis();
    }

    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
    private static final int MAX_UPLOAD = 2 * 1024 * 1024;

    /** Recebe um pedaço da imagem; quando chegam todos, faz o mesmo que o comando com endereço. */
    public static void receberParte(ServerPlayer p, ConstrucaoRede.FotoPartePayload v) {
        if (v.total() < 1 || v.total() > 80 || v.indice() < 0 || v.indice() >= v.total()) return;
        Upload u = UPLOADS.get(p.getUUID());
        if (u == null || u.sessao != v.sessao() || System.currentTimeMillis() - u.inicio > 60_000) {
            if (v.indice() != 0 && u != null && u.sessao == v.sessao()) return;
            u = new Upload();
            u.sessao = v.sessao();
            u.total = v.total();
            u.larg = v.larg();
            u.alt = v.alt();
            u.modo = v.modo();
            u.partes = new byte[v.total()][];
            UPLOADS.put(p.getUUID(), u);
        }
        if (v.total() != u.total || u.partes[v.indice()] != null) return;
        u.bytes += v.dados().length;
        if (u.bytes > MAX_UPLOAD) {
            UPLOADS.remove(p.getUUID());
            erro(p, "Imagem grande demais.");
            return;
        }
        u.partes[v.indice()] = v.dados();
        if (++u.recebidas < u.total) return;
        UPLOADS.remove(p.getUUID());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] b : u.partes) out.write(b, 0, b.length);
        criarDeBytes(p, u.larg, u.alt, u.modo, out.toByteArray());
    }

    private static void criarDeBytes(ServerPlayer p, int larg, int alt, String modo, byte[] bytes) {
        if (larg < 1 || alt < 1 || larg > FotoDados.MAX_LADO || alt > FotoDados.MAX_LADO) {
            erro(p, "Tamanho inválido.");
            return;
        }
        if (!modo.equals(ImagemMapa.CORTAR) && !modo.equals(ImagemMapa.AJUSTAR) && !modo.equals(ImagemMapa.ESTICAR)) modo = ImagemMapa.CORTAR;
        boolean op = Comandos.admin(p);
        String err = dados.podeCriar(p.getUUID().toString(), op, larg * alt);
        if (err != null) {
            erro(p, err);
            return;
        }
        if (OCUPADOS.contains(p.getUUID())) {
            erro(p, "Ainda estou preparando a sua foto anterior. Espere terminar.");
            return;
        }
        HitResult h = p.pick(6.0, 1.0F, false);
        if (!(h instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK || !bh.getDirection().getAxis().isHorizontal()) {
            erro(p, "Olhe para o lado de um bloco da parede (a até 6 blocos): ele será o canto inferior esquerdo da foto.");
            return;
        }
        BlockPos base = bh.getBlockPos().relative(bh.getDirection());
        Pedido ped = new Pedido(p.getUUID(), p.getGameProfile().name(), op, Corpos.dimDe((ServerLevel) p.level()), base, bh.getDirection(), larg, alt, "arquivo:pc", modo);
        OCUPADOS.add(p.getUUID());
        p.sendSystemMessage(Component.literal("Preparando a foto (" + larg + "x" + alt + ")...").withStyle(ChatFormatting.YELLOW), false);
        EXEC.submit(() -> {
            try {
                BufferedImage img = ImagemMapa.ler(bytes);
                byte[][] mapas = ImagemMapa.preparar(img, ped.larg, ped.alt, ped.modo, true);
                Jogo.server.execute(() -> colocar(ped, mapas));
            } catch (Exception e) {
                String msg = e instanceof IllegalArgumentException || e instanceof IOException ? e.getMessage() : "Não consegui ler a imagem (" + e.getClass().getSimpleName() + ").";
                Jogo.server.execute(() -> terminar(ped, msg));
            }
        });
    }

    private static void terminar(Pedido ped, String erro) {
        OCUPADOS.remove(ped.jogador);
        ServerPlayer p = Jogo.server.getPlayerList().getPlayer(ped.jogador);
        if (p != null && erro != null) p.sendSystemMessage(Component.literal(erro).withStyle(ChatFormatting.RED), false);
    }

    private static byte[] baixar(String url) throws Exception {
        String atual = url;
        long limite = System.nanoTime() + Duration.ofSeconds(25).toNanos();
        for (int salto = 0; salto <= 3; salto++) {
            String err = UrlSegura.validar(atual);
            if (err != null) throw new IOException(err);
            URI u = URI.create(atual);
            for (InetAddress a : InetAddress.getAllByName(u.getHost())) {
                if (!UrlSegura.ipPermitido(a)) throw new IOException("Esse endereço aponta para a rede interna.");
            }
            HttpRequest req = HttpRequest.newBuilder(u).timeout(Duration.ofSeconds(15)).header("User-Agent", "MelbyCraft-fotomoldura/1.0").GET().build();
            HttpResponse<InputStream> r = CLIENTE.send(req, HttpResponse.BodyHandlers.ofInputStream());
            int sc = r.statusCode();
            if (sc >= 300 && sc < 400) {
                r.body().close();
                String loc = r.headers().firstValue("location").orElse(null);
                String novo = loc == null ? null : UrlSegura.redirecionar(atual, loc);
                if (novo == null) throw new IOException("A imagem redireciona para um endereço que não é permitido.");
                atual = novo;
                continue;
            }
            if (sc != 200) {
                r.body().close();
                throw new IOException("O site respondeu " + sc + ". Confira se o link abre a imagem direto.");
            }
            if (r.headers().firstValueAsLong("content-length").orElse(-1) > MAX_BYTES) {
                r.body().close();
                throw new IOException("Arquivo grande demais (máximo 8 MB).");
            }
            String tipo = r.headers().firstValue("content-type").orElse("").toLowerCase();
            if (tipo.startsWith("text/")) {
                r.body().close();
                throw new IOException("Esse endereço é uma página, não uma imagem. Use o link direto do arquivo da imagem.");
            }
            try (InputStream in = r.body()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) throw new IOException("Arquivo grande demais (máximo 8 MB).");
                    if (System.nanoTime() > limite) throw new IOException("O download demorou demais.");
                }
                return out.toByteArray();
            }
        }
        throw new IOException("Redirecionamentos demais.");
    }

    /** Na thread principal: confere a parede de novo, cria os mapas e as molduras. */
    private static void colocar(Pedido ped, byte[][] mapas) {
        ServerPlayer p = Jogo.server.getPlayerList().getPlayer(ped.jogador);
        try {
            if (p == null) return;
            ServerLevel nivel = (ServerLevel) p.level();
            if (!Corpos.dimDe(nivel).equals(ped.dim)) throw new IOException("Você mudou de dimensão. Tente de novo.");
            String err = dados.podeCriar(ped.jogador.toString(), ped.op, ped.larg * ped.alt);
            if (err != null) throw new IOException(err);
            List<ItemFrame> molduras = new ArrayList<>();
            for (int lin = 0; lin < ped.alt; lin++) {
                for (int col = 0; col < ped.larg; col++) {
                    int[] c = FotoDados.celula(ped.base.getX(), ped.base.getY(), ped.base.getZ(), ped.normal.getStepX(), ped.normal.getStepZ(), ped.alt, col, lin);
                    BlockPos pos = new BlockPos(c[0], c[1], c[2]);
                    if (!nivel.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) throw new IOException("A parede está em chunks descarregados. Chegue mais perto.");
                    ItemFrame f = new ItemFrame(nivel, pos, ped.normal);
                    if (!f.survives()) {
                        throw new IOException("Não cabe: em " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                            + " falta parede atrás ou o espaço está ocupado. A foto ocupa " + ped.larg + " de largura (para a direita) e " + ped.alt + " de altura (para cima) a partir do bloco que você olhou.");
                    }
                    molduras.add(f);
                }
            }
            Grupo g = new Grupo();
            g.dono = ped.jogador.toString();
            g.donoNome = ped.nomeJogador;
            g.dim = ped.dim;
            g.direcao = ped.normal.name();
            g.x = ped.base.getX();
            g.y = ped.base.getY();
            g.z = ped.base.getZ();
            g.larg = ped.larg;
            g.alt = ped.alt;
            g.site = ped.url.startsWith("arquivo:") ? "arquivo do jogador" : URI.create(ped.url).getHost();
            for (int i = 0; i < molduras.size(); i++) {
                ItemStack mapa = MapItem.create(nivel, CENTRO_LONGE, CENTRO_LONGE, (byte) 0, false, false);
                MapItemSavedData md = MapItem.getSavedData(mapa, nivel);
                if (md == null) throw new IOException("O jogo não criou o mapa. Avise a staff.");
                System.arraycopy(mapas[i], 0, md.colors, 0, mapas[i].length);
                md.setDirty();
                g.mapas.add(idDoMapa(mapa));
                ItemFrame f = molduras.get(i);
                f.setItem(mapa, false);
                f.setInvisible(true);
                f.setPermanentlyInvulnerable(true);
                nivel.addFreshEntity(f);
                g.molduras.add(f.getUUID().toString());
            }
            dados.adicionar(g, System.currentTimeMillis());
            sujo = true;
            p.sendSystemMessage(Component.literal("Foto #" + g.id + " colocada: " + ped.larg + "x" + ped.alt + " molduras (" + g.mapas.size() + " mapas). /fotomoldura apagar remove.")
                .withStyle(ChatFormatting.GREEN), false);
            terminar(ped, null);
        } catch (Exception e) {
            terminar(ped, e instanceof IOException ? e.getMessage() : "Não consegui colocar a foto (" + e.getClass().getSimpleName() + ").");
            if (!(e instanceof IOException)) Melbybet.LOGGER.error("Falha ao colocar fotomoldura", e);
        }
    }

    private static int idDoMapa(ItemStack mapa) {
        var id = mapa.get(net.minecraft.core.component.DataComponents.MAP_ID);
        return id == null ? -1 : id.id();
    }

    // ---------- achar o mural ----------

    private static ItemFrame moldura(ServerPlayer p) {
        Vec3 de = p.getEyePosition();
        Vec3 ate = de.add(p.getLookAngle().scale(8.0));
        ItemFrame melhor = null;
        double dist = Double.MAX_VALUE;
        for (ItemFrame f : p.level().getEntitiesOfClass(ItemFrame.class, new AABB(de, ate).inflate(1.0))) {
            Optional<Vec3> hit = f.getBoundingBox().inflate(0.1).clip(de, ate);
            if (hit.isEmpty()) continue;
            double d = de.distanceToSqr(hit.get());
            if (d < dist) {
                dist = d;
                melhor = f;
            }
        }
        return melhor;
    }

    private static Grupo olhado(ServerPlayer p) {
        ItemFrame f = moldura(p);
        return f == null ? null : dados.acharPorMoldura(f.getUUID().toString());
    }

    // ---------- outros comandos ----------

    private static int lista(ServerPlayer p) {
        boolean op = Comandos.admin(p);
        List<Grupo> l = new ArrayList<>();
        for (Grupo g : dados.grupos) if (op || g.dono.equals(p.getUUID().toString())) l.add(g);
        if (l.isEmpty()) return erro(p, "Nenhuma foto sua. /fotomoldura <LxA> <endereço>");
        p.sendSystemMessage(Component.literal(op ? "Fotos do servidor (" + l.size() + ")" : "Suas fotos (" + l.size() + "/" + FotoDados.MAX_POR_JOGADOR + ")")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (Grupo g : l) {
            p.sendSystemMessage(Component.literal("#" + g.id + " ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(g.larg + "x" + g.alt + " em " + g.x + " " + g.y + " " + g.z + " (" + g.dim + ")" + (op ? " de " + g.donoNome : "") + ", site " + g.site)
                    .withStyle(ChatFormatting.GRAY)), false);
        }
        return l.size();
    }

    private static int info(ServerPlayer p) {
        Grupo g = olhado(p);
        if (g == null) return erro(p, "Olhe para uma moldura de foto (a até 8 blocos).");
        p.sendSystemMessage(Component.literal("Foto #" + g.id + ": " + g.larg + "x" + g.alt + ", de " + g.donoNome + ", site " + g.site + ", mapas "
            + g.mapas.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("") + ".").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int reparar(ServerPlayer p) {
        Grupo g = olhado(p);
        if (g == null) return erro(p, "Olhe para uma moldura de foto (a até 8 blocos).");
        String err = FotoDados.pode(g, p.getUUID().toString(), Comandos.admin(p));
        if (err != null) return erro(p, err);
        ServerLevel n = (ServerLevel) p.level();
        int n1 = 0;
        for (String u : g.molduras) {
            Entity e = n.getEntity(UUID.fromString(u));
            if (e instanceof ItemFrame f && f.getRotation() != 0) {
                f.setRotation(0);
                n1++;
            }
        }
        p.sendSystemMessage(Component.literal(n1 == 0 ? "Nenhuma moldura estava girada." : n1 + " moldura(s) desgirada(s)."
            ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** Apaga a última foto que você colocou, sem precisar olhar para ela. */
    private static int desfazer(ServerPlayer p) {
        int ultimo = -1;
        for (Grupo g : dados.grupos) if (g.dono.equals(p.getUUID().toString()) && g.id > ultimo) ultimo = g.id;
        if (ultimo < 0) return erro(p, "Você não tem foto para desfazer.");
        return apagar(p, ultimo);
    }

    private static int apagar(ServerPlayer p, int id) {
        Grupo g = id > 0 ? dados.achar(id) : olhado(p);
        if (g == null) return erro(p, id > 0 ? "Não existe a foto #" + id + "." : "Olhe para uma moldura de foto (a até 8 blocos) ou use /fotomoldura apagar <id>.");
        String err = FotoDados.pode(g, p.getUUID().toString(), Comandos.admin(p));
        if (err != null) return erro(p, err);
        if (!g.dim.equals(Corpos.dimDe((ServerLevel) p.level()))) return erro(p, "Essa foto está em outra dimensão (" + g.dim + ").");
        ServerLevel n = (ServerLevel) p.level();
        int removidas = 0;
        for (int lin = 0; lin < g.alt; lin++) {
            for (int col = 0; col < g.larg; col++) {
                var nz = Direction.valueOf(g.direcao);
                int[] c = FotoDados.celula(g.x, g.y, g.z, nz.getStepX(), nz.getStepZ(), g.alt, col, lin);
                if (!n.hasChunk(c[0] >> 4, c[2] >> 4)) return erro(p, "Parte da foto está em chunks descarregados. Chegue perto e tente de novo.");
            }
        }
        for (String u : g.molduras) {
            Entity e = n.getEntity(UUID.fromString(u));
            if (e instanceof ItemFrame f) {
                f.discard();
                removidas++;
            }
        }
        dados.remover(g.id);
        sujo = true;
        p.sendSystemMessage(Component.literal("Foto #" + g.id + " apagada (" + removidas + " moldura(s)). Os arquivos dos mapas ficam no mundo.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int erro(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED), false);
        return 0;
    }
}
