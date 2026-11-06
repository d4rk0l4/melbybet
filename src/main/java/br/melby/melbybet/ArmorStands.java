package br.melby.melbybet;

import br.melby.melbybet.core.Donos;
import br.melby.melbybet.core.PosePreset;
import br.melby.melbybet.core.PosePreset.Pose;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /armorstand: editor de armor stand (pose de cada parte em graus, braços, base, invisível, giro e ajuste fino de posição),
 * com poses guardadas e copiar/colar entre stands. Cada stand colocado depois do mod tem dono; os antigos só OP edita.
 * Não precisa de nada no cliente.
 */
public final class ArmorStands {
    private ArmorStands() {}

    private static Donos donos = new Donos();
    private static PosePreset presets = new PosePreset();
    private static Path arqDonos, arqPoses;
    private static boolean sujoDonos, sujoPoses;
    private static final Map<UUID, Pose> COPIADAS = new HashMap<>();

    private record Pendente(UUID jogador, int tick) {}

    private static final List<Pendente> PENDENTES = new ArrayList<>();

    private static String chave(ArmorStand a) {
        return "stand:" + a.getUUID();
    }

    public static void carregar() {
        Path pasta = FabricLoader.getInstance().getConfigDir().resolve("melbybet");
        arqDonos = pasta.resolve("donos.json");
        arqPoses = pasta.resolve("armorstand-poses.json");
        donos = Donos.ler(arqDonos);
        presets = PosePreset.ler(arqPoses);
        sujoDonos = false;
        sujoPoses = false;
    }

    public static void salvarAgora() {
        try {
            if (sujoDonos && arqDonos != null) {
                donos.gravar(arqDonos);
                sujoDonos = false;
            }
            if (sujoPoses && arqPoses != null) {
                presets.gravar(arqPoses);
                sujoPoses = false;
            }
        } catch (Exception e) {
            Melbybet.LOGGER.error("Falha ao salvar os dados do /armorstand", e);
        }
    }

    public static void aCadaSegundo() {
        if (sujoDonos || sujoPoses) salvarAgora();
        if (!PENDENTES.isEmpty()) {
            int agora = Jogo.server.getTickCount();
            PENDENTES.removeIf(x -> agora - x.tick > 10);
        }
    }

    public static void aoSair(ServerPlayer p) {
        COPIADAS.remove(p.getUUID());
    }

    /** Quem coloca um armor stand passa a ser o dono dele. */
    public static void registrarEventos() {
        UseBlockCallback.EVENT.register((p, n, mao, hit) -> {
            if (n.isClientSide() || !(p instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (p.getItemInHand(mao).is(Items.ARMOR_STAND)) PENDENTES.add(new Pendente(sp.getUUID(), Jogo.server.getTickCount()));
            return InteractionResult.PASS;
        });
        ServerEntityEvents.ENTITY_LOAD.register((e, n) -> {
            if (!(e instanceof ArmorStand a) || PENDENTES.isEmpty() || Jogo.server == null) return;
            int agora = Jogo.server.getTickCount();
            for (Pendente x : new ArrayList<>(PENDENTES)) {
                ServerPlayer p = Jogo.server.getPlayerList().getPlayer(x.jogador);
                if (p == null || p.level() != a.level() || agora - x.tick > 3 || p.distanceTo(a) > 10) continue;
                if (donos.reivindicar(chave(a), p.getUUID().toString(), p.getGameProfile().name(), System.currentTimeMillis())) sujoDonos = true;
                PENDENTES.remove(x);
                break;
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((e, fonte) -> {
            if (e instanceof ArmorStand a && donos.de(chave(a)) != null) {
                donos.remover(chave(a));
                sujoDonos = true;
            }
        });
    }

    // ---------- achar o stand para o qual o jogador olha ----------

    private static ArmorStand olhado(ServerPlayer p) {
        Vec3 de = p.getEyePosition();
        Vec3 ate = de.add(p.getLookAngle().scale(6.0));
        AABB caixa = new AABB(de, ate).inflate(1.0);
        ArmorStand melhor = null;
        double dist = Double.MAX_VALUE;
        for (ArmorStand a : p.level().getEntitiesOfClass(ArmorStand.class, caixa)) {
            Optional<Vec3> hit = a.getBoundingBox().inflate(0.1).clip(de, ate);
            if (hit.isEmpty()) continue;
            double d = de.distanceToSqr(hit.get());
            if (d < dist) {
                dist = d;
                melhor = a;
            }
        }
        return melhor;
    }

    private static boolean vazio(ArmorStand a) {
        for (EquipmentSlot s : EquipmentSlot.values()) if (!a.getItemBySlot(s).isEmpty()) return false;
        return true;
    }

    /** Devolve o motivo de não poder, ou null. */
    private static String pode(ServerPlayer p, ArmorStand a) {
        return donos.pode(chave(a), p.getUUID().toString(), Comandos.admin(p));
    }

    // ---------- comandos ----------

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("armorstand")
            .then(literal("reivindicar").executes(c -> reivindicar(c.getSource().getPlayerOrException())))
            .then(literal("dono").executes(c -> dono(c.getSource().getPlayerOrException())))
            .then(literal("transferir").then(argument("jogador", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Jogo.casa.conhecidos.values(), b))
                .executes(c -> transferir(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "jogador")))))
            .then(literal("poses").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                ArmorStand a = olhado(p);
                if (a == null) return erro(p, "Olhe para um armor stand (a até 6 blocos).");
                String err = pode(p, a);
                if (err != null) return erro(p, err);
                abrirPoses(p, a);
                return 1;
            }))
            .executes(c -> abrir(c.getSource().getPlayerOrException())));
    }

    private static int abrir(ServerPlayer p) {
        ArmorStand a = olhado(p);
        if (a == null) return erro(p, "Olhe para um armor stand (a até 6 blocos).");
        String err = pode(p, a);
        if (err != null) return erro(p, err);
        editor(p, a);
        return 1;
    }

    private static int reivindicar(ServerPlayer p) {
        ArmorStand a = olhado(p);
        if (a == null) return erro(p, "Olhe para um armor stand (a até 6 blocos).");
        if (donos.de(chave(a)) != null) return erro(p, "Esse armor stand já pertence a " + donos.de(chave(a)).nome + ".");
        if (!vazio(a) && !Comandos.admin(p)) return erro(p, "Só dá para reivindicar um armor stand vazio (sem armadura nem itens). Tire tudo dele antes.");
        donos.reivindicar(chave(a), p.getUUID().toString(), p.getGameProfile().name(), System.currentTimeMillis());
        sujoDonos = true;
        p.sendSystemMessage(Component.literal("Esse armor stand agora é seu.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int dono(ServerPlayer p) {
        ArmorStand a = olhado(p);
        if (a == null) return erro(p, "Olhe para um armor stand (a até 6 blocos).");
        Donos.Dono d = donos.de(chave(a));
        p.sendSystemMessage(Component.literal(d == null ? "Sem dono registrado (colocado antes do mod): só OP edita. /armorstand reivindicar se estiver vazio."
            : "Dono: " + d.nome).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int transferir(ServerPlayer p, String nick) {
        ArmorStand a = olhado(p);
        if (a == null) return erro(p, "Olhe para um armor stand (a até 6 blocos).");
        String uuid = null;
        for (var e : Jogo.casa.conhecidos.entrySet()) if (e.getValue().equalsIgnoreCase(nick)) uuid = e.getKey();
        if (uuid == null) return erro(p, "Não conheço o jogador \"" + nick + "\".");
        String err = donos.transferir(chave(a), p.getUUID().toString(), Comandos.admin(p), uuid, nick, System.currentTimeMillis());
        if (err != null) return erro(p, err);
        sujoDonos = true;
        p.sendSystemMessage(Component.literal("Armor stand passado para " + nick + ".").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ---------- ler e aplicar poses ----------

    private static Rotations[] partes(ArmorStand a) {
        return new Rotations[] {a.getHeadPose(), a.getBodyPose(), a.getLeftArmPose(), a.getRightArmPose(), a.getLeftLegPose(), a.getRightLegPose()};
    }

    private static Pose ler(ArmorStand a) {
        Pose po = new Pose();
        Rotations[] r = partes(a);
        for (int i = 0; i < PosePreset.PARTES; i++) po.angulos[i] = new float[] {r[i].x(), r[i].y(), r[i].z()};
        po.bracos = a.showArms();
        po.base = a.showBasePlate();
        return po;
    }

    private static void aplicarParte(ArmorStand a, int i, float[] v) {
        Rotations r = new Rotations(v[0], v[1], v[2]);
        switch (i) {
            case 0 -> a.setHeadPose(r);
            case 1 -> a.setBodyPose(r);
            case 2 -> a.setLeftArmPose(r);
            case 3 -> a.setRightArmPose(r);
            case 4 -> a.setLeftLegPose(r);
            default -> a.setRightLegPose(r);
        }
    }

    private static void aplicar(ArmorStand a, Pose po) {
        for (int i = 0; i < PosePreset.PARTES; i++) aplicarParte(a, i, po.angulos[i]);
        a.setShowArms(po.bracos);
        a.setNoBasePlate(!po.base);
    }

    // ---------- o editor ----------

    private static GuiElementBuilder btn(Item item, String nome, ChatFormatting cor, String... lore) {
        GuiElementBuilder e = new GuiElementBuilder(item).setName(Component.literal(nome).withStyle(cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        for (String l : lore) e.addLoreLine(Component.literal(l).withStyle(ChatFormatting.GRAY));
        return e;
    }

    private static final Item[] ICONES = {Items.SKELETON_SKULL, Items.IRON_CHESTPLATE, Items.STICK, Items.STICK, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
    private static final String[] EIXOS = {"X (para frente e para trás)", "Y (giro)", "Z (para os lados)"};

    private static final class Sessao {
        int parte, passo = 15;
    }

    static void editor(ServerPlayer p, ArmorStand a) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        g.setTitle(Component.literal("Armor stand").withStyle(ChatFormatting.GOLD));
        Sessao s = new Sessao();
        desenhar(g, p, a, s);
        g.open();
    }

    /** Executa a mudança se o stand ainda existe e o jogador ainda pode editá-lo, e redesenha. */
    private static void mudar(SimpleGui g, ServerPlayer p, ArmorStand a, Sessao s, Runnable acao) {
        if (a.isRemoved() || !a.isAlive()) {
            g.close();
            return;
        }
        String err = pode(p, a);
        if (err != null || p.distanceTo(a) > 12) {
            g.close();
            p.sendSystemMessage(Component.literal(err != null ? err : "Você se afastou demais do armor stand.").withStyle(ChatFormatting.RED), true);
            return;
        }
        acao.run();
        desenhar(g, p, a, s);
    }

    private static void desenhar(SimpleGui g, ServerPlayer p, ArmorStand a, Sessao s) {
        for (int i = 0; i < 54; i++) g.clearSlot(i);
        Rotations[] rot = partes(a);
        for (int i = 0; i < PosePreset.PARTES; i++) {
            final int k = i;
            GuiElementBuilder b = btn(ICONES[i], PosePreset.NOMES[i], i == s.parte ? ChatFormatting.GREEN : ChatFormatting.WHITE,
                i == s.parte ? "Editando esta parte" : "Clique para editar");
            if (i == s.parte) b.glow();
            g.setSlot(i, b.setCallback((ix, ty, ac, gg) -> {
                s.parte = k;
                desenhar(g, p, a, s);
            }).build());
        }
        g.setSlot(8, btn(Items.CLOCK, "Passo: " + s.passo + "°", ChatFormatting.AQUA, "Clique para trocar (1, 5, 15, 45)").setCallback((ix, ty, ac, gg) -> {
            s.passo = PosePreset.proximoPasso(s.passo);
            desenhar(g, p, a, s);
        }).build());
        Rotations atual = rot[s.parte];
        float[] v = {atual.x(), atual.y(), atual.z()};
        for (int e = 0; e < 3; e++) {
            final int eixo = e;
            int col = 1 + e * 2;
            g.setSlot(9 + col, btn(Items.DYE.pick(DyeColor.LIME), "+" + s.passo + "°", ChatFormatting.GREEN, EIXOS[e]).setCallback((ix, ty, ac, gg) ->
                mudar(g, p, a, s, () -> mexer(a, s.parte, eixo, s.passo))).build());
            g.setSlot(18 + col, btn(Items.PAPER, EIXOS[e].substring(0, 1) + ": " + Math.round(v[e]) + "°", ChatFormatting.WHITE, EIXOS[e]).build());
            g.setSlot(27 + col, btn(Items.DYE.pick(DyeColor.RED), "-" + s.passo + "°", ChatFormatting.RED, EIXOS[e]).setCallback((ix, ty, ac, gg) ->
                mudar(g, p, a, s, () -> mexer(a, s.parte, eixo, -s.passo))).build());
        }
        g.setSlot(16, btn(Items.BARRIER, "Zerar esta parte", ChatFormatting.RED).setCallback((ix, ty, ac, gg) ->
            mudar(g, p, a, s, () -> aplicarParte(a, s.parte, new float[] {0, 0, 0}))).build());
        g.setSlot(25, btn(Items.BOOK, "Copiar a pose", ChatFormatting.AQUA, "Guarda a pose inteira desse stand").setCallback((ix, ty, ac, gg) -> {
            COPIADAS.put(p.getUUID(), ler(a));
            p.sendSystemMessage(Component.literal("Pose copiada. Abra outro armor stand e use Colar.").withStyle(ChatFormatting.GREEN), true);
        }).build());
        g.setSlot(34, btn(Items.WRITABLE_BOOK, "Colar a pose", ChatFormatting.AQUA, "Shift: colar espelhada").setCallback((ix, ty, ac, gg) -> {
            Pose c = COPIADAS.get(p.getUUID());
            if (c == null) {
                p.sendSystemMessage(Component.literal("Copie uma pose primeiro.").withStyle(ChatFormatting.RED), true);
                return;
            }
            mudar(g, p, a, s, () -> aplicar(a, ty.shift ? PosePreset.espelhar(c) : c));
        }).build());
        g.setSlot(36, btn(Items.STICK, "Braços: " + (a.showArms() ? "mostrando" : "escondidos"), ChatFormatting.YELLOW).setCallback((ix, ty, ac, gg) ->
            mudar(g, p, a, s, () -> a.setShowArms(!a.showArms()))).build());
        g.setSlot(37, btn(Items.SMOOTH_STONE_SLAB, "Base: " + (a.showBasePlate() ? "com" : "sem"), ChatFormatting.YELLOW).setCallback((ix, ty, ac, gg) ->
            mudar(g, p, a, s, () -> a.setNoBasePlate(a.showBasePlate()))).build());
        g.setSlot(38, btn(Items.GLASS, "Invisível: " + (a.isInvisible() ? "sim" : "não"), ChatFormatting.YELLOW).setCallback((ix, ty, ac, gg) ->
            mudar(g, p, a, s, () -> a.setInvisible(!a.isInvisible()))).build());
        g.setSlot(40, btn(Items.ARROW, "Girar -" + s.passo + "°", ChatFormatting.GOLD, "Direção do stand: " + Math.round(a.getYRot()) + "°").setCallback((ix, ty, ac, gg) ->
            mudar(g, p, a, s, () -> girarCorpo(a, -s.passo))).build());
        g.setSlot(41, btn(Items.ARROW, "Girar +" + s.passo + "°", ChatFormatting.GOLD, "Direção do stand: " + Math.round(a.getYRot()) + "°").setCallback((ix, ty, ac, gg) ->
            mudar(g, p, a, s, () -> girarCorpo(a, s.passo))).build());
        String[] nomes = {"Oeste", "Leste", "Norte", "Sul", "Baixo", "Cima"};
        double[][] d = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, -1, 0}, {0, 1, 0}};
        for (int i = 0; i < 6; i++) {
            final double[] dd = d[i];
            g.setSlot(45 + i, btn(Items.COMPASS, "Mover " + nomes[i], ChatFormatting.LIGHT_PURPLE, "1/16 de bloco por clique", "Shift: 1/4 de bloco").setCallback((ix, ty, ac, gg) ->
                mudar(g, p, a, s, () -> mover(a, dd, ty.shift ? 0.25 : 0.0625))).build());
        }
        g.setSlot(51, btn(Items.NETHER_STAR, "Poses guardadas", ChatFormatting.LIGHT_PURPLE).setCallback((ix, ty, ac, gg) -> abrirPoses(p, a)).build());
        g.setSlot(53, btn(Items.BARRIER, "Fechar", ChatFormatting.RED).setCallback((ix, ty, ac, gg) -> gg.close()).build());
    }

    private static void mexer(ArmorStand a, int parte, int eixo, int passo) {
        Rotations r = partes(a)[parte];
        float[] v = {r.x(), r.y(), r.z()};
        v[eixo] = PosePreset.girar(v[eixo], passo);
        aplicarParte(a, parte, v);
    }

    private static void girarCorpo(ArmorStand a, int graus) {
        float y = PosePreset.girar(a.getYRot(), graus);
        a.setYRot(y);
        a.setYBodyRot(y);
        a.setYHeadRot(y);
    }

    private static void mover(ArmorStand a, double[] d, double passo) {
        a.setPos(a.getX() + d[0] * passo, a.getY() + d[1] * passo, a.getZ() + d[2] * passo);
    }

    // ---------- poses guardadas ----------

    static void abrirPoses(ServerPlayer p, ArmorStand a) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(Component.literal("Poses guardadas").withStyle(ChatFormatting.LIGHT_PURPLE));
        List<Pose> l = presets.de(p.getUUID().toString());
        for (int i = 0; i < Math.min(l.size(), 18); i++) {
            Pose po = l.get(i);
            g.setSlot(i, btn(Items.ARMOR_STAND, po.nome, ChatFormatting.YELLOW, "Esquerdo: aplicar", "Shift: aplicar espelhada", "Direito: apagar").setCallback((ix, ty, ac, gg) -> {
                if (ty.isRight) {
                    presets.apagar(p.getUUID().toString(), po.nome);
                    sujoPoses = true;
                    abrirPoses(p, a);
                    return;
                }
                String err = a.isRemoved() ? "O armor stand sumiu." : pode(p, a);
                if (err != null) {
                    p.sendSystemMessage(Component.literal(err).withStyle(ChatFormatting.RED), true);
                    return;
                }
                aplicar(a, ty.shift ? PosePreset.espelhar(po) : po);
                p.sendSystemMessage(Component.literal("Pose \"" + po.nome + "\" aplicada.").withStyle(ChatFormatting.GREEN), true);
            }).build());
        }
        if (l.isEmpty()) g.setSlot(13, btn(Items.PAPER, "Nenhuma pose guardada", ChatFormatting.GRAY, "Use Guardar para salvar a pose atual").build());
        g.setSlot(18, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((ix, ty, ac, gg) -> editor(p, a)).build());
        g.setSlot(22, btn(Items.WRITABLE_BOOK, "Guardar a pose atual", ChatFormatting.GREEN, "Pede um nome no chat").setCallback((ix, ty, ac, gg) ->
            Chat.perguntar(p, "Que nome dar a esta pose?", txt -> {
                if (a.isRemoved()) {
                    p.sendSystemMessage(Component.literal("O armor stand sumiu.").withStyle(ChatFormatting.RED));
                    return;
                }
                String err = presets.salvar(p.getUUID().toString(), txt, ler(a), System.currentTimeMillis());
                if (err != null) p.sendSystemMessage(Component.literal(err).withStyle(ChatFormatting.RED));
                else {
                    sujoPoses = true;
                    p.sendSystemMessage(Component.literal("Pose \"" + txt + "\" guardada.").withStyle(ChatFormatting.GREEN));
                }
                abrirPoses(p, a);
            })).build());
        g.open();
    }

    private static int erro(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED), true);
        return 0;
    }
}
