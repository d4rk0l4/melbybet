package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.JsonOps;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Corpo: quando alguém morre, os itens ficam num corpo (a cabeça dele flutuando, com o nome). Qualquer jogador abre
 * com o botão direito, vê os itens e escolhe qual pegar. Morte no void deixa o corpo no último lugar seguro.
 */
public final class Corpos {
    private Corpos() {}

    private static final String TAG = "mbcorpo";

    private record Seguro(String dim, double x, double y, double z) {}

    private static final Map<UUID, Seguro> SEGUROS = new HashMap<>();
    private static int contador;

    // ---------- itens como texto ----------

    static String paraJson(ItemStack s) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, Jogo.server.registryAccess());
        return ItemStack.OPTIONAL_CODEC.encodeStart(ops, s).getOrThrow().toString();
    }

    static ItemStack deJson(String j) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, Jogo.server.registryAccess());
        return ItemStack.OPTIONAL_CODEC.parse(ops, JsonParser.parseString(j)).result().orElse(ItemStack.EMPTY);
    }

    static String dimDe(ServerLevel nivel) {
        String s = nivel.dimension().toString();
        int a = s.lastIndexOf(':');
        int b = s.lastIndexOf(']');
        return a >= 0 && b > a ? s.substring(a + 1, b) : "overworld";
    }

    static ServerLevel nivelDe(String dim) {
        for (ServerLevel n : Jogo.server.getAllLevels()) if (dimDe(n).equals(dim)) return n;
        return null;
    }

    private static Entity bonecoDe(Casa.Corpo c) {
        if (c.boneco.isEmpty()) return null;
        try {
            UUID id = UUID.fromString(c.boneco);
            for (ServerLevel n : Jogo.server.getAllLevels()) {
                Entity e = n.getEntity(id);
                if (e != null) return e;
            }
        } catch (IllegalArgumentException ignorado) {
            // uuid inválido: sem boneco
        }
        return null;
    }

    // ---------- registro ----------

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        UseEntityCallback.EVENT.register((player, nivel, mao, entidade, hit) -> {
            if (!entidade.entityTags().contains(TAG)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp && mao == InteractionHand.MAIN_HAND && Jogo.server != null) {
                Casa.Corpo c = Jogo.casa.corpoDoBoneco(entidade.getUUID().toString());
                if (c != null) MenuCorpo.abrir(sp, c);
            }
            return InteractionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((player, nivel, mao, entidade, hit) ->
            entidade.entityTags().contains(TAG) ? InteractionResult.FAIL : InteractionResult.PASS);

        d.register(literal("corpo")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                List<Casa.Corpo> meus = Jogo.casa.corposDe(p.getUUID().toString());
                if (meus.isEmpty()) {
                    p.sendSystemMessage(Jogo.prefixo("Você não tem nenhum corpo com itens."));
                    return 1;
                }
                for (Casa.Corpo x : meus)
                    p.sendSystemMessage(Component.literal("Corpo #" + x.id + ": " + x.dim + " " + Math.round(x.x) + " " + Math.round(x.y) + " " + Math.round(x.z)
                        + " (" + x.itens.size() + " item(ns))").withStyle(ChatFormatting.YELLOW));
                return 1;
            })
            .then(literal("abrir").requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer()))
                .then(argument("id", IntegerArgumentType.integer(1)).executes(c -> {
                    Casa.Corpo x = Jogo.casa.corpo(IntegerArgumentType.getInteger(c, "id"));
                    if (x == null) {
                        c.getSource().sendFailure(Component.literal("Não existe esse corpo."));
                        return 0;
                    }
                    MenuCorpo.abrir(c.getSource().getPlayerOrException(), x);
                    return 1;
                })))
            .then(literal("recuperar").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Casa.Morte alvo = null;
                for (int i = Jogo.casa.historicoMortes.size() - 1; i >= 0 && alvo == null; i--) {
                    Casa.Morte m = Jogo.casa.historicoMortes.get(i);
                    if (m.uuid.equals(p.getUUID().toString()) && !m.recuperado && !m.restantes.isEmpty() && Jogo.casa.corpo(m.corpoId) == null) alvo = m;
                }
                if (alvo == null) {
                    p.sendSystemMessage(Jogo.prefixo("Nenhuma morte sua com itens a recuperar (o corpo precisa ter sumido)."));
                    return 0;
                }
                String erro = recuperar(p, alvo, false);
                p.sendSystemMessage(Jogo.prefixo(erro == null ? "Itens recuperados." : erro));
                return erro == null ? 1 : 0;
            })
            .then(argument("nick", com.mojang.brigadier.arguments.StringArgumentType.word()).requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer())).executes(c -> {
                String nick = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nick");
                ServerPlayer op = c.getSource().getPlayerOrException();
                Casa.Morte alvo = null;
                for (int i = Jogo.casa.historicoMortes.size() - 1; i >= 0 && alvo == null; i--) {
                    Casa.Morte m = Jogo.casa.historicoMortes.get(i);
                    Casa.Visto v = Jogo.casa.vistos.get(nick.toLowerCase());
                    if (v != null && m.uuid.equals(v.uuid) && !m.recuperado && !m.restantes.isEmpty()) alvo = m;
                }
                if (alvo == null) {
                    op.sendSystemMessage(Jogo.prefixo("Nenhuma morte de " + nick + " com itens guardados."));
                    return 0;
                }
                ServerPlayer dono = Jogo.server.getPlayerList().getPlayerByName(nick);
                if (dono == null) {
                    op.sendSystemMessage(Jogo.prefixo(nick + " precisa estar online para receber."));
                    return 0;
                }
                String erro = recuperar(dono, alvo, true);
                op.sendSystemMessage(Jogo.prefixo(erro == null ? "Itens de " + nick + " devolvidos." : erro));
                return erro == null ? 1 : 0;
            })))
            .then(literal("log").requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer()))
                .executes(c -> mostrarLog(c.getSource(), ""))
                .then(argument("nick", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> mostrarLog(c.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nick")))))
            .then(literal("lista").requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer())).executes(c -> {
                for (Casa.Corpo x : Jogo.casa.corpos)
                    c.getSource().sendSuccess(() -> Component.literal("#" + x.id + " " + x.nome + ": " + x.dim + " " + Math.round(x.x) + " " + Math.round(x.y) + " " + Math.round(x.z)
                        + " (" + x.itens.size() + " item(ns))"), false);
                return Jogo.casa.corpos.size();
            })));
    }

    private static int mostrarLog(CommandSourceStack src, String filtro) {
        var fmt = new java.text.SimpleDateFormat("dd/MM HH:mm");
        List<String> l = new ArrayList<>();
        for (String e : Jogo.casa.logCorpos) if (filtro.isEmpty() || e.toLowerCase().contains(filtro.toLowerCase())) l.add(e);
        for (String e : l.subList(Math.max(0, l.size() - 15), l.size())) {
            int t = e.indexOf('\t');
            String quando = t > 0 ? fmt.format(new java.util.Date(Long.parseLong(e.substring(0, t)))) : "";
            String txt = t > 0 ? e.substring(t + 1) : e;
            src.sendSuccess(() -> Component.literal(quando + "  " + txt), false);
        }
        if (l.isEmpty()) src.sendSuccess(() -> Component.literal("Log de corpos vazio."), false);
        return l.size();
    }

    // ---------- morte ----------

    /** Antes da morte: tira todos os itens e cria o corpo. */
    public static void antes(ServerPlayer p, DamageSource fonte) {
        if (Jogo.server == null || !(p.level() instanceof ServerLevel nivel)) return;
        if (nivel.getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        Inventory inv = p.getInventory();
        List<String> itens = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            itens.add(paraJson(st));
            slots.add(i);
            inv.setItem(i, ItemStack.EMPTY);
        }
        if (itens.isEmpty()) return;

        String dim = dimDe(nivel);
        double x = p.getX(), y = p.getY(), z = p.getZ();
        var arena = Arena.origemParaCorpo(p.getUUID());
        if (arena != null && nivelDe(arena.dim) != null) {
            // morreu na arena de duelo: o corpo fica onde ele estava antes do duelo
            dim = arena.dim;
            x = arena.x;
            y = arena.y;
            z = arena.z;
            nivel = nivelDe(dim);
        } else if (fonte.typeHolder().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            Seguro s = SEGUROS.get(p.getUUID());
            if (s != null && nivelDe(s.dim()) != null) {
                dim = s.dim();
                x = s.x();
                y = s.y();
                z = s.z();
                nivel = nivelDe(dim);
            } else {
                y = Math.max(y, nivel.getMinY() + 2);
            }
        }
        Casa.Corpo c = Jogo.casa.novoCorpo(p.getUUID().toString(), p.getGameProfile().name(), dim, x, y, z, itens, slots, System.currentTimeMillis());
        UUID idBoneco = UUID.randomUUID();
        String nick = p.getGameProfile().name();
        String pose = Jogo.casa.config.corpoPose.matches("sleeping|swimming|crouching|standing|fall_flying") ? Jogo.casa.config.corpoPose : "sleeping";
        long ua = idBoneco.getMostSignificantBits(), ub = idBoneco.getLeastSignificantBits();
        String nbt = "{UUID:[I;" + (int) (ua >> 32) + "," + (int) ua + "," + (int) (ub >> 32) + "," + (int) ub + "],Tags:[\"" + TAG + "\"],"
            + "CustomName:'{\"text\":\"Corpo de " + nick + "\",\"color\":\"gray\"}',CustomNameVisible:1b,Invulnerable:1b,NoGravity:1b,"
            + "immovable:1b,hide_description:1b,pose:\"" + pose + "\",profile:\"" + nick + "\"}";
        String cmd = String.format(java.util.Locale.ROOT, "execute in minecraft:%s run summon minecraft:mannequin %.3f %.3f %.3f %s", dim, x, y, z, nbt);
        Jogo.server.getCommands().performPrefixedCommand(Jogo.server.createCommandSourceStack().withSuppressedOutput(), cmd);
        c.boneco = idBoneco.toString();
        WaypointsServidor.corpoCriado(p, c);
        MortesServidor.registrarMorte(p, c);
        Jogo.casa.logCorpo(nick + " morreu em " + dim + " " + Math.round(x) + " " + Math.round(y) + " " + Math.round(z) + " (" + itens.size() + " itens, corpo #" + c.id + ")");
        Jogo.salvar();
        p.sendSystemMessage(Component.literal("Seus itens ficaram no seu corpo em " + dim + " " + Math.round(x) + " " + Math.round(y) + " " + Math.round(z)
            + ". Só você pega nos primeiros " + Jogo.casa.config.corpoProtecaoSeg + "s; depois qualquer jogador abre com o botão direito. Use /corpo para ver depois.").withStyle(ChatFormatting.YELLOW));
    }

    /** Veste o manequim com o que o jogador usava (cabeça, peito, calça, bota, mão principal e secundária). */
    static void vestir(Casa.Corpo c, List<ItemStack> equip) {
        Entity e = bonecoDe(c);
        if (!(e instanceof net.minecraft.world.entity.LivingEntity vivo) || equip.size() < 6) return;
        EquipmentSlot[] ordem = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        for (int i = 0; i < 6; i++) if (!equip.get(i).isEmpty()) vivo.setItemSlot(ordem[i], equip.get(i).copy());
    }

    /**
     * Devolve os itens que sobraram de um corpo que sumiu (expirou, foi removido ou o dono morreu de novo tentando pegar).
     * Dono: só se o corpo não existe mais. OP (forcar): remove o corpo se ainda existir. Devolve null se deu certo, ou o motivo.
     */
    public static String recuperar(ServerPlayer p, Casa.Morte m, boolean forcar) {
        Casa.Corpo vivo = Jogo.casa.corpo(m.corpoId);
        if (vivo != null && vivo.uuid.equals(m.uuid)) {
            if (!forcar) return "O corpo ainda existe em " + vivo.dim + " " + Math.round(vivo.x) + " " + Math.round(vivo.y) + " " + Math.round(vivo.z) + ".";
            Entity e = bonecoDe(vivo);
            if (e != null) e.discard();
            WaypointsServidor.corpoRemovido(vivo);
            Jogo.casa.removerCorpo(vivo.id);
        }
        if (m.recuperado || m.restantes.isEmpty()) return "Não há itens para recuperar dessa morte.";
        Inventory inv = p.getInventory();
        int dados = 0;
        for (int i = 0; i < m.restantes.size(); i++) {
            ItemStack st = deJson(m.restantes.get(i));
            if (st.isEmpty()) continue;
            dados++;
            int slot = i < m.restSlots.size() && m.restSlots.get(i) != null ? m.restSlots.get(i) : -1;
            int alvo = Casa.Corpo.destino(slot, inv.getContainerSize(), n -> inv.getItem(n).isEmpty());
            if (alvo >= 0) {
                inv.setItem(alvo, st);
                continue;
            }
            inv.add(st);
            if (!st.isEmpty() && p.level() instanceof ServerLevel nivel) nivel.addFreshEntity(new ItemEntity(nivel, p.getX(), p.getY(), p.getZ(), st));
        }
        m.restantes = new ArrayList<>();
        m.restSlots = new ArrayList<>();
        m.recuperado = true;
        Jogo.casa.logCorpo(p.getGameProfile().name() + " recuperou " + dados + " item(ns) da morte de " + new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date(m.quando)) + (forcar ? " (OP)" : ""));
        p.containerMenu.broadcastChanges();
        MortesServidor.enviar(p);
        Jogo.salvar();
        return null;
    }

    // ---------- pegar itens ----------

    /**
     * Pega um item do corpo. Se quem pega é o dono, o item volta para o slot onde estava (hotbar, armadura, mão
     * secundária) quando esse slot está livre; senão, ou para outros jogadores, vai para o primeiro espaço livre.
     */
    /** Segundos que ainda faltam para o corpo ser liberado para quem não é o dono (0 = pode pegar). */
    public static long protecaoRestante(ServerPlayer p, Casa.Corpo c) {
        if (p.getUUID().toString().equals(c.uuid) || Comandos.admin(p)) return 0;
        long ms = Jogo.casa.config.corpoProtecaoSeg * 1000L - (System.currentTimeMillis() - c.quando);
        return ms <= 0 ? 0 : (ms + 999) / 1000;
    }

    public static void pegar(ServerPlayer p, Casa.Corpo c, int indice) {
        long resta = protecaoRestante(p, c);
        if (resta > 0) {
            p.sendSystemMessage(Component.literal("Esse corpo está protegido por mais " + resta + "s, só o dono pega agora.").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (indice < 0 || indice >= c.itens.size()) return;
        ItemStack st = deJson(c.itens.get(indice));
        String descricao = st.getCount() + "x " + st.getHoverName().getString();
        if (st.isEmpty()) {
            c.removerItem(indice);
        } else {
            if (p.getUUID().toString().equals(c.uuid)) {
                Inventory inv = p.getInventory();
                int alvo = Casa.Corpo.destino(c.slotDe(indice), inv.getContainerSize(), n -> inv.getItem(n).isEmpty());
                if (alvo >= 0) {
                    inv.setItem(alvo, st);
                    st = ItemStack.EMPTY;
                }
            }
            if (!st.isEmpty()) p.getInventory().add(st);
            if (st.isEmpty()) {
                c.removerItem(indice);
            } else {
                c.itens.set(indice, paraJson(st));
                p.sendSystemMessage(Jogo.prefixo("Inventário cheio: o resto ficou no corpo."));
            }
        }
        p.containerMenu.broadcastChanges();
        Jogo.casa.logCorpo(p.getGameProfile().name() + " pegou " + descricao + " do corpo #" + c.id + " de " + c.nome);
        avisarDono(p, c);
        fecharSeVazio(c);
        MortesServidor.corpoMudou(c, c.itens.isEmpty());
        Jogo.salvar();
    }

    public static void pegarTudo(ServerPlayer p, Casa.Corpo c) {
        // primeiro os itens com slot de origem (para ninguém ocupar o lugar deles), depois o resto
        for (int passo = 0; passo < 2; passo++) {
            for (int i = c.itens.size() - 1; i >= 0; i--) {
                if (passo == 0 && c.slotDe(i) < 0) continue;
                int antes = c.itens.size();
                pegar(p, c, i);
                if (Jogo.casa.corpo(c.id) == null) return;
                if (c.itens.size() == antes) return; // não coube mais nada
            }
        }
    }

    private static void avisarDono(ServerPlayer quem, Casa.Corpo c) {
        if (quem.getUUID().toString().equals(c.uuid)) return;
        ServerPlayer dono = Jogo.server.getPlayerList().getPlayer(UUID.fromString(c.uuid));
        if (dono != null) dono.sendSystemMessage(Component.literal(quem.getGameProfile().name() + " mexeu no seu corpo.").withStyle(ChatFormatting.GRAY), true);
    }

    private static void fecharSeVazio(Casa.Corpo c) {
        if (!c.itens.isEmpty()) return;
        Entity e = bonecoDe(c);
        if (e != null) e.discard();
        WaypointsServidor.corpoRemovido(c);
        Jogo.casa.removerCorpo(c.id);
    }

    // ---------- a cada segundo ----------

    public static void aCadaSegundo() {
        if (Jogo.server == null) return;
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            if (p.onGround() && !p.isInWater() && !p.isInLava() && p.level() instanceof ServerLevel nivel)
                SEGUROS.put(p.getUUID(), new Seguro(dimDe(nivel), p.getX(), p.getY(), p.getZ()));
        }
        if (++contador % 60 != 0) return;
        int horas = Jogo.casa.config.corpoHoras;
        if (horas <= 0) return;
        long limite = System.currentTimeMillis() - horas * 3_600_000L;
        for (Casa.Corpo c : new ArrayList<>(Jogo.casa.corpos)) {
            if (c.quando > limite) continue;
            ServerLevel nivel = nivelDe(c.dim);
            boolean guardar = Jogo.casa.config.corpoGuardarAoExpirar;
            for (Casa.Morte m : Jogo.casa.historicoMortes) if (m.corpoId == c.id && m.uuid.equals(c.uuid)) m.expirado = true;
            Jogo.casa.logCorpo("Corpo #" + c.id + " de " + c.nome + " expirou com " + c.itens.size() + " item(ns)" + (guardar ? " (guardados para recuperar)" : " (soltos no chão)"));
            if (nivel != null && !guardar) {
                for (String j : c.itens) {
                    ItemStack st = deJson(j);
                    if (!st.isEmpty()) nivel.addFreshEntity(new ItemEntity(nivel, c.x, c.y, c.z, st));
                }
            }
            c.itens.clear();
            fecharSeVazio(c);
        }
        Jogo.salvar();
    }
}
