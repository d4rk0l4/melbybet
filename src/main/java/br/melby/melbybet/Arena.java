package br.melby.melbybet;

import br.melby.melbybet.core.ArenaGeo;
import br.melby.melbybet.core.ArenaRegistro;
import br.melby.melbybet.core.Casa;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Arena de vidro dos duelos: uma caixa fechada no céu do Overworld, construída só em lugar 100% vazio (ar), com contagem
 * regressiva, proteção do vidro e limpeza total no fim. O estado fica em disco (arenas.json) para recuperar depois de
 * um reinício. Duelos usam isto automaticamente, sem comando.
 */
public final class Arena {
    private Arena() {}

    private static final int CAMADAS_POR_TICK = 3;
    private static final int FLAGS = 2 | 16; // avisa o cliente, sem atualizar vizinhos
    private static final int ACIMA = 32;
    private static final int TENTATIVAS_LUGAR = 25;
    private static final int PASSO_LUGAR = 48;

    private static ArenaRegistro reg = new ArenaRegistro();
    private static Path arq;
    private static long agoraTick;

    private static final class Sessao {
        Casa.Duelo x;
        ArenaRegistro.Arena a;
        ServerLevel nivel;
        ArenaGeo.Caixa caixa;
        /** CONSTRUINDO, CONTAGEM, LUTA ou LIMPANDO. */
        String fase = "CONSTRUINDO";
        int camada;
        int contagem;
        BooleanSupplier ativar;
        boolean teleportou;
        int foraA, foraB;
        long limpezaEm;
        int limpezaCamada;
        boolean retornou;
        int falhasLimpeza;

        boolean duelista(UUID u) {
            String s = u.toString();
            return s.equals(a.uuidA) || s.equals(a.uuidB);
        }
    }

    private static final Map<Integer, Sessao> SESSOES = new HashMap<>();
    /** Origem dos duelistas depois do fim, até o corpo ser criado (só na memória, some em 2 minutos). */
    private record PosFim(ArenaRegistro.Origem o, long ate) {}
    private static final Map<UUID, PosFim> POS_FIM = new HashMap<>();
    /** Quem entrou e precisa ser levado de volta (do reg.devolver); processado no próximo tick. */
    private static final List<UUID> VOLTAR = new ArrayList<>();

    // ---------- configuração ----------

    public static boolean ativa() {
        return Jogo.casa.config.arenaAtiva;
    }

    // ---------- registro e ciclo de vida ----------

    private static void salvar() {
        if (arq == null) return;
        try {
            reg.gravar(arq);
        } catch (Exception e) {
            Melbybet.LOGGER.error("Não consegui gravar as arenas em {}", arq, e);
        }
    }

    /** Ao ligar o servidor: desfaz toda arena que ficou para trás e devolve a lista de duelos que dependiam dela. */
    public static List<Integer> aoLigar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("arenas.json");
        reg = ArenaRegistro.ler(arq);
        List<Integer> duelos = new ArrayList<>();
        for (ArenaRegistro.Arena a : new ArrayList<>(reg.arenas)) {
            duelos.add(a.duelo);
            ServerLevel n = Corpos.nivelDe(a.dim);
            boolean jogadoresLa = a.estado.equals("CONTAGEM") || a.estado.equals("LUTA") || a.estado.equals("LIMPANDO");
            if (jogadoresLa) {
                if (!a.uuidA.isEmpty()) reg.devolver.put(a.uuidA, a.origemA);
                if (!a.uuidB.isEmpty()) reg.devolver.put(a.uuidB, a.origemB);
            }
            if (n == null) {
                Melbybet.LOGGER.warn("[arena] dimensão {} não existe; arena do duelo {} fica registrada", a.dim, a.duelo);
                continue;
            }
            try {
                limparTudo(n, a.caixa().expandida(1));
                reg.liberar(a.duelo);
                Melbybet.LOGGER.info("[arena] arena pendente do duelo {} removida ao ligar", a.duelo);
            } catch (Exception e) {
                Melbybet.LOGGER.error("[arena] não consegui limpar a arena do duelo {}", a.duelo, e);
            }
        }
        salvar();
        return duelos;
    }

    /** Joga fora tudo que não for ar na caixa (só roda em arena nossa; o lugar foi escolhido vazio). */
    private static void limparTudo(ServerLevel n, ArenaGeo.Caixa c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState ar = Blocks.AIR.defaultBlockState();
        for (int y = c.y0(); y <= c.y1(); y++) limparCamada(n, c, y, p, ar);
        descartarEntidades(n, c, true);
    }

    private static void limparCamada(ServerLevel n, ArenaGeo.Caixa c, int y, BlockPos.MutableBlockPos p, BlockState ar) {
        for (int x = c.x0(); x <= c.x1(); x++) {
            for (int z = c.z0(); z <= c.z1(); z++) {
                p.set(x, y, z);
                if (!n.getBlockState(p).isAir()) n.setBlock(p, ar, FLAGS);
            }
        }
    }

    private static AABB caixaAabb(ArenaGeo.Caixa c) {
        return new AABB(c.x0(), c.y0(), c.z0(), c.x1() + 1, c.y1() + 1, c.z1() + 1);
    }

    /** Remove itens, orbes, flechas e bichos que sobraram (nunca jogadores). */
    private static void descartarEntidades(ServerLevel n, ArenaGeo.Caixa c, boolean tudo) {
        for (Entity e : n.getEntities((Entity) null, caixaAabb(c), en -> !(en instanceof Player) && (tudo || en instanceof Mob))) {
            e.discard();
        }
    }

    // ---------- achar lugar e reservar ----------

    private static ServerPlayer on(String uuid) {
        return Jogo.server.getPlayerList().getPlayer(UUID.fromString(uuid));
    }

    private static ArenaRegistro.Origem origemDe(ServerPlayer p) {
        return new ArenaRegistro.Origem(Corpos.dimDe((ServerLevel) p.level()), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
    }

    /**
     * Procura e reserva um lugar. Devolve null se deu certo (a construção continua nos próximos ticks e chama
     * {@code ativar} quando estiver pronta), ou o texto do problema.
     */
    public static String iniciar(ServerPlayer a, ServerPlayer b, Casa.Duelo x, BooleanSupplier ativar) {
        Casa.Config cfg = Jogo.casa.config;
        ServerLevel ow = Jogo.server.overworld();
        int larg = Math.max(7, cfg.arenaLarg), comp = Math.max(7, cfg.arenaComp), alt = Math.max(5, cfg.arenaAlt);

        int cx, cz;
        if (b.level() == ow) {
            cx = b.blockPosition().getX();
            cz = b.blockPosition().getZ();
        } else if (a.level() == ow) {
            cx = a.blockPosition().getX();
            cz = a.blockPosition().getZ();
        } else if (!ow.players().isEmpty()) {
            cx = ow.players().get(0).blockPosition().getX();
            cz = ow.players().get(0).blockPosition().getZ();
        } else {
            return "Não achei um lugar para a arena: ninguém está no Overworld para eu usar o céu de referência.";
        }

        ArenaGeo.Caixa escolhida = null;
        String dim = Corpos.dimDe(ow);
        int tentativas = 0;
        for (int[] c : ArenaGeo.candidatos(cx, cz, PASSO_LUGAR, 3)) {
            if (tentativas++ >= TENTATIVAS_LUGAR) break;
            escolhida = tentar(ow, c[0], c[1], larg, comp, alt, dim, cfg);
            if (escolhida != null) break;
        }
        if (escolhida == null) return "Não achei um lugar livre no céu para a arena. Tente de novo em instantes.";

        ArenaRegistro.Arena reservada = reg.reservar(x.id, dim, escolhida, cfg.arenaMaterial, System.currentTimeMillis());
        reservada.uuidA = x.uuidA;
        reservada.uuidB = x.uuidB;
        reservada.origemA = origemDe(a);
        reservada.origemB = origemDe(b);
        reservada.estado = "CONSTRUINDO";
        salvar();

        Sessao s = new Sessao();
        s.x = x;
        s.a = reservada;
        s.nivel = ow;
        s.caixa = escolhida;
        s.ativar = ativar;
        SESSOES.put(x.id, s);
        x.estado = "PREPARANDO";
        Jogo.salvar();
        Melbybet.LOGGER.info("[arena] duelo {}: arena reservada em {} {} {}", x.id, escolhida.x0(), escolhida.y0(), escolhida.z0());
        return null;
    }

    /** Testa um centro (cx,cz): chunks carregados, altura acima do terreno, sem outra arena e TODO o espaço vazio. */
    private static ArenaGeo.Caixa tentar(ServerLevel n, int cx, int cz, int larg, int comp, int alt, String dim, Casa.Config cfg) {
        int x0 = cx - larg / 2, z0 = cz - comp / 2;
        if (!n.hasChunksAt(x0 - 1, z0 - 1, x0 + larg, z0 + comp)) return null;
        int terreno = n.getMinY();
        for (int x = x0 - 1; x <= x0 + larg; x += 4) {
            for (int z = z0 - 1; z <= z0 + comp; z += 4) terreno = Math.max(terreno, n.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        }
        terreno = Math.max(terreno, n.getHeight(Heightmap.Types.WORLD_SURFACE, x0 + larg, z0 + comp));
        int y0 = ArenaGeo.alturaBase(terreno, n.getMinY(), n.getMaxY(), alt, ACIMA);
        if (y0 < 0) return null;
        ArenaGeo.Caixa caixa = new ArenaGeo.Caixa(x0, y0, z0, larg, comp, alt);
        if (!reg.livre(dim, caixa, Math.max(1, cfg.arenaDistMin))) return null;
        return vazio(n, caixa.expandida(1)) ? caixa : null;
    }

    private static boolean vazio(ServerLevel n, ArenaGeo.Caixa c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = c.y0(); y <= c.y1(); y++) {
            for (int x = c.x0(); x <= c.x1(); x++) {
                for (int z = c.z0(); z <= c.z1(); z++) {
                    p.set(x, y, z);
                    if (!n.getBlockState(p).isAir()) return false;
                }
            }
        }
        return true;
    }

    private static BlockState vidro(String material) {
        Identifier id = Identifier.tryParse(material);
        if (id != null) {
            var b = BuiltInRegistries.BLOCK.getOptional(id);
            if (b.isPresent() && b.get() != Blocks.AIR) return b.get().defaultBlockState();
        }
        return Blocks.GLASS.defaultBlockState();
    }

    // ---------- tick ----------

    public static void aCadaTick() {
        agoraTick++;
        if (Jogo.server == null) return;
        processarVoltas();
        if (!POS_FIM.isEmpty() && agoraTick % 100 == 0) {
            long agora = System.currentTimeMillis();
            POS_FIM.values().removeIf(p -> p.ate() < agora);
        }
        if (SESSOES.isEmpty()) return;
        for (Sessao s : new ArrayList<>(SESSOES.values())) {
            try {
                switch (s.fase) {
                    case "CONSTRUINDO" -> construir(s);
                    case "CONTAGEM" -> contar(s);
                    case "LUTA" -> lutar(s);
                    case "LIMPANDO" -> limpar(s);
                    default -> {
                    }
                }
            } catch (Exception e) {
                Melbybet.LOGGER.error("[arena] erro na arena do duelo {} (fase {})", s.x.id, s.fase, e);
                if (!s.fase.equals("LIMPANDO")) falhar(s, "erro interno na arena.");
                else if (++s.falhasLimpeza > 40) abandonar(s);
            }
        }
    }

    private static void construir(Sessao s) {
        BlockState v = vidro(s.a.material);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < CAMADAS_POR_TICK && s.camada < s.caixa.alt(); i++, s.camada++) {
            int y = s.caixa.y0() + s.camada;
            for (int x = s.caixa.x0(); x <= s.caixa.x1(); x++) {
                for (int z = s.caixa.z0(); z <= s.caixa.z1(); z++) {
                    if (s.caixa.casca(x, y, z)) {
                        p.set(x, y, z);
                        s.nivel.setBlock(p, v, FLAGS);
                    }
                }
            }
        }
        if (s.camada < s.caixa.alt()) return;
        // pronta: deixa o duelo conferir saldo/itens e começar, e leva os dois para dentro
        if (!s.ativar.getAsBoolean()) {
            iniciarLimpeza(s, 0);
            return;
        }
        ServerPlayer pa = on(s.a.uuidA), pb = on(s.a.uuidB);
        if (pa == null || pb == null) {
            falhar(s, "alguém saiu antes de entrar na arena.");
            return;
        }
        double[] a = s.caixa.posA(), b = s.caixa.posB();
        boolean okA = entrar(pa, s, a);
        boolean okB = okA && entrar(pb, s, b);
        s.teleportou = true;
        if (!okA || !okB) {
            falhar(s, "não consegui teleportar os dois para a arena.");
            return;
        }
        s.fase = "CONTAGEM";
        s.a.estado = "CONTAGEM";
        s.contagem = Math.max(1, Jogo.casa.config.arenaContagemSeg) * 20;
        salvar();
        Melbybet.LOGGER.info("[arena] duelo {}: jogadores dentro, contagem iniciada", s.x.id);
    }

    private static boolean entrar(ServerPlayer p, Sessao s, double[] pos) {
        p.stopRiding();
        return p.teleportTo(s.nivel, pos[0], pos[1], pos[2], Set.of(), (float) pos[3], 0f, true);
    }

    private static void contar(Sessao s) {
        manterCasca(s);
        if (s.contagem % 20 == 0) {
            int seg = s.contagem / 20;
            for (String u : new String[] {s.a.uuidA, s.a.uuidB}) {
                ServerPlayer p = on(u);
                if (p == null) continue;
                if (seg > 0) titulo(p, String.valueOf(seg), "Prepare-se!", ChatFormatting.YELLOW);
                else titulo(p, "VALENDO!", "Vence quem matar o outro", ChatFormatting.RED);
                Jogo.som(p, seg > 0 ? "minecraft:block.note_block.hat" : "minecraft:entity.ender_dragon.growl", seg > 0 ? 1.0f : 1.2f);
            }
        }
        if (s.contagem-- > 0) return;
        s.fase = "LUTA";
        s.a.estado = "LUTA";
        s.x.inicio = System.currentTimeMillis();
        salvar();
        Jogo.salvar();
    }

    private static void lutar(Sessao s) {
        if (agoraTick % 10 == 0) manterCasca(s);
        if (agoraTick % 20 != 0) return;
        descartarEntidades(s.nivel, s.caixa, false);
        ServerPlayer pa = on(s.a.uuidA), pb = on(s.a.uuidB);
        if (pa != null && !pa.isDeadOrDying()) s.foraA = fora(pa, s) ? s.foraA + 1 : 0;
        if (pb != null && !pb.isDeadOrDying()) s.foraB = fora(pb, s) ? s.foraB + 1 : 0;
        if (s.foraA >= 2) Duelos.perderPorFuga(s.x, s.a.uuidA);
        else if (s.foraB >= 2) Duelos.perderPorFuga(s.x, s.a.uuidB);
    }

    private static boolean fora(ServerPlayer p, Sessao s) {
        return p.level() != s.nivel || !s.caixa.dentroLivre(p.getX(), p.getY(), p.getZ());
    }

    /** Repõe qualquer peça do vidro que tenha sumido (explosão, por exemplo). */
    private static void manterCasca(Sessao s) {
        BlockState v = vidro(s.a.material);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        ArenaGeo.Caixa c = s.caixa;
        for (int y = c.y0(); y <= c.y1(); y++) {
            boolean cheia = y == c.y0() || y == c.y1();
            for (int x = c.x0(); x <= c.x1(); x++) {
                for (int z = c.z0(); z <= c.z1(); z++) {
                    if (!cheia && x != c.x0() && x != c.x1() && z != c.z0() && z != c.z1()) continue;
                    p.set(x, y, z);
                    if (s.nivel.getBlockState(p).getBlock() != v.getBlock()) s.nivel.setBlock(p, v, FLAGS);
                }
            }
        }
    }

    // ---------- fim ----------

    /** O duelo terminou (vitória ou cancelamento): daqui a pouco os dois voltam e a arena some. */
    public static void fim(Casa.Duelo x) {
        Sessao s = SESSOES.get(x.id);
        if (s == null || s.fase.equals("LIMPANDO")) return;
        long ate = System.currentTimeMillis() + 120_000L;
        if (s.teleportou) {
            POS_FIM.put(UUID.fromString(s.a.uuidA), new PosFim(s.a.origemA, ate));
            POS_FIM.put(UUID.fromString(s.a.uuidB), new PosFim(s.a.origemB, ate));
        }
        iniciarLimpeza(s, s.teleportou ? Math.max(0, Jogo.casa.config.arenaLimpezaSeg) * 20 : 0);
    }

    private static void iniciarLimpeza(Sessao s, int ticksAte) {
        s.fase = "LIMPANDO";
        s.a.estado = "LIMPANDO";
        s.limpezaEm = agoraTick + ticksAte;
        s.limpezaCamada = s.caixa.y0() - 1;
        salvar();
    }

    /** Quando não dá para seguir (erro, teleporte, saldo): desfaz o duelo sem cobrar nada ou devolvendo o que foi cobrado. */
    private static void falhar(Sessao s, String motivo) {
        Melbybet.LOGGER.warn("[arena] duelo {} falhou: {}", s.x.id, motivo);
        if (s.x.estado.equals("ATIVO")) Duelos.cancelarPorArena(s.x, motivo);
        else {
            Jogo.casa.cancelarDuelo(s.x, System.currentTimeMillis());
            Jogo.avisar("Duelo " + s.x.nomeA + " x " + s.x.nomeB + " cancelado: " + motivo);
            Jogo.salvar();
        }
        fim(s.x);
        if (SESSOES.containsKey(s.x.id) && !s.fase.equals("LIMPANDO")) iniciarLimpeza(s, 0);
    }

    private static void limpar(Sessao s) {
        if (agoraTick < s.limpezaEm) return;
        if (!s.retornou) {
            devolverJogadores(s);
            s.retornou = true;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState ar = Blocks.AIR.defaultBlockState();
        ArenaGeo.Caixa c = s.caixa.expandida(1);
        for (int i = 0; i < CAMADAS_POR_TICK + 1 && s.limpezaCamada <= c.y1(); i++, s.limpezaCamada++) limparCamada(s.nivel, c, s.limpezaCamada, p, ar);
        if (s.limpezaCamada <= c.y1()) return;
        descartarEntidades(s.nivel, c, true);
        SESSOES.remove(s.x.id);
        reg.liberar(s.x.id);
        salvar();
        Melbybet.LOGGER.info("[arena] duelo {}: arena removida", s.x.id);
    }

    /** Sem jeito de limpar: registra e solta a reserva para não travar o sistema (o aviso fica no log). */
    private static void abandonar(Sessao s) {
        Melbybet.LOGGER.error("[arena] desisti de limpar a arena do duelo {}; ela segue em arenas.json e será limpa ao reiniciar", s.x.id);
        SESSOES.remove(s.x.id);
        salvar();
    }

    private static void devolverJogadores(Sessao s) {
        if (!s.teleportou) return;
        voltar(s, s.a.uuidA, s.a.origemA);
        voltar(s, s.a.uuidB, s.a.origemB);
    }

    private static void voltar(Sessao s, String uuid, ArenaRegistro.Origem o) {
        ServerPlayer p = on(uuid);
        if (p == null) {
            reg.devolver.put(uuid, o);
            return;
        }
        if (p.isDeadOrDying()) return;
        boolean naArena = p.level() == s.nivel && s.caixa.expandida(2).contem((int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getZ()));
        if (!naArena) return;
        if (!ir(p, o)) {
            // não deu para voltar ao lugar de antes: manda para a cama/spawn, para não ficar preso no céu
            try {
                var t = p.findRespawnPositionAndUseSpawnBlock(false, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);
                p.teleport(t);
            } catch (Exception e) {
                reg.devolver.put(uuid, o);
                Melbybet.LOGGER.error("[arena] não consegui devolver {} ao lugar de antes", p.getGameProfile().name(), e);
            }
        }
    }

    private static boolean ir(ServerPlayer p, ArenaRegistro.Origem o) {
        ServerLevel n = Corpos.nivelDe(o.dim);
        if (n == null) return false;
        p.stopRiding();
        return p.teleportTo(n, o.x, o.y, o.z, Set.of(), o.yaw, o.pitch, true);
    }

    // ---------- jogadores que entram ----------

    public static void aoEntrar(ServerPlayer p) {
        if (reg.devolver.containsKey(p.getUUID().toString())) VOLTAR.add(p.getUUID());
    }

    private static void processarVoltas() {
        if (VOLTAR.isEmpty()) return;
        for (UUID u : new ArrayList<>(VOLTAR)) {
            ServerPlayer p = Jogo.server.getPlayerList().getPlayer(u);
            if (p == null) {
                VOLTAR.remove(u);
                continue;
            }
            ArenaRegistro.Origem o = reg.devolver.get(u.toString());
            if (o == null) {
                VOLTAR.remove(u);
                continue;
            }
            // se ainda existir uma arena dele, o tick de limpeza cuida de levar para fora
            boolean aindaNaArena = false;
            for (Sessao s : SESSOES.values()) aindaNaArena |= s.duelista(u) && !s.fase.equals("LIMPANDO");
            if (aindaNaArena) continue;
            if (ir(p, o)) {
                reg.devolver.remove(u.toString());
                salvar();
                p.sendSystemMessage(Jogo.prefixo("Você voltou para onde estava antes do duelo."));
                Melbybet.LOGGER.info("[arena] {} devolvido ao lugar de antes do duelo", p.getGameProfile().name());
            }
            VOLTAR.remove(u);
        }
    }

    // ---------- corpo ----------

    /** Se o jogador está (ou acabou de estar) num duelo com arena, o lugar de antes dele; senão null. */
    public static ArenaRegistro.Origem origemParaCorpo(UUID u) {
        for (Sessao s : SESSOES.values()) {
            if (!s.teleportou || !s.duelista(u)) continue;
            return u.toString().equals(s.a.uuidA) ? s.a.origemA : s.a.origemB;
        }
        PosFim pf = POS_FIM.get(u);
        return pf == null || pf.ate() < System.currentTimeMillis() ? null : pf.o();
    }

    // ---------- proteção ----------

    private static void titulo(ServerPlayer p, String t, String sub, ChatFormatting cor) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 18, 4));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t).withStyle(cor, ChatFormatting.BOLD)));
    }

    private static Sessao sessaoDe(UUID u) {
        for (Sessao s : SESSOES.values()) if (s.teleportou && !s.fase.equals("LIMPANDO") && s.duelista(u)) return s;
        return null;
    }

    private static Sessao sessaoEm(net.minecraft.world.level.Level n, BlockPos pos) {
        if (SESSOES.isEmpty()) return null;
        for (Sessao s : SESSOES.values()) {
            if (s.nivel == n && s.caixa.expandida(1).contem(pos.getX(), pos.getY(), pos.getZ())) return s;
        }
        return null;
    }

    public static void registrar() {
        // ninguém quebra o vidro nem a margem; dentro, só o que os duelistas colocaram
        PlayerBlockBreakEvents.BEFORE.register((n, p, pos, st, be) -> {
            Sessao s = sessaoEm(n, pos);
            return s == null || s.caixa.interior(pos.getX(), pos.getY(), pos.getZ());
        });
        // só os duelistas colocam blocos, e só dentro da jaula
        UseBlockCallback.EVENT.register((p, n, mao, hit) -> {
            if (n.isClientSide() || SESSOES.isEmpty()) return InteractionResult.PASS;
            ItemStack st = p.getItemInHand(mao);
            if (st.isEmpty()) return InteractionResult.PASS;
            BlockPos alvo = hit.getBlockPos().relative(hit.getDirection());
            Sessao s = sessaoEm(n, alvo);
            if (s == null) s = sessaoEm(n, hit.getBlockPos());
            if (s == null) return InteractionResult.PASS;
            if (s.duelista(p.getUUID()) && s.caixa.interior(alvo.getX(), alvo.getY(), alvo.getZ())) return InteractionResult.PASS;
            p.sendSystemMessage(Component.literal("A arena é protegida.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        });
        // pérola e fruta do coro teleportam: o único teleporte permitido é o do começo e o do fim
        UseItemCallback.EVENT.register((p, n, mao) -> {
            if (n.isClientSide() || SESSOES.isEmpty()) return InteractionResult.PASS;
            ItemStack st = p.getItemInHand(mao);
            if (!(st.is(Items.ENDER_PEARL) || st.is(Items.CHORUS_FRUIT))) return InteractionResult.PASS;
            if (sessaoDe(p.getUUID()) == null) return InteractionResult.PASS;
            p.sendSystemMessage(Component.literal("Teleporte bloqueado durante o duelo.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        });
        // contagem regressiva: ninguém leva dano
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, fonte, dano) -> {
            if (SESSOES.isEmpty() || !(e instanceof ServerPlayer p)) return true;
            Sessao s = sessaoDe(p.getUUID());
            return s == null || !s.fase.equals("CONTAGEM");
        });
    }
}
