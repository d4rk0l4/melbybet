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
import net.minecraft.world.entity.decoration.ArmorStand;
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
            if (!(entidade instanceof ArmorStand) || !entidade.entityTags().contains(TAG)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp && mao == InteractionHand.MAIN_HAND && Jogo.server != null) {
                Casa.Corpo c = Jogo.casa.corpoDoBoneco(entidade.getUUID().toString());
                if (c != null) MenuCorpo.abrir(sp, c);
            }
            return InteractionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((player, nivel, mao, entidade, hit) ->
            entidade instanceof ArmorStand && entidade.entityTags().contains(TAG) ? InteractionResult.FAIL : InteractionResult.PASS);

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
            .then(literal("lista").requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer())).executes(c -> {
                for (Casa.Corpo x : Jogo.casa.corpos)
                    c.getSource().sendSuccess(() -> Component.literal("#" + x.id + " " + x.nome + ": " + x.dim + " " + Math.round(x.x) + " " + Math.round(x.y) + " " + Math.round(x.z)
                        + " (" + x.itens.size() + " item(ns))"), false);
                return Jogo.casa.corpos.size();
            })));
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
        ArmorStand boneco = new ArmorStand(nivel, x, y, z);
        boneco.addTag(TAG);
        boneco.setInvisible(true);
        boneco.setNoGravity(true);
        boneco.setPermanentlyInvulnerable(true);
        boneco.setCustomName(Component.literal("Corpo de " + p.getGameProfile().name()).withStyle(ChatFormatting.GRAY));
        boneco.setCustomNameVisible(true);
        boneco.setItemSlot(EquipmentSlot.HEAD, new GuiElementBuilder(Items.PLAYER_HEAD).setProfile(p.getGameProfile().name()).asStack());
        nivel.addFreshEntity(boneco);
        c.boneco = boneco.getUUID().toString();
        WaypointsServidor.corpoCriado(p, c);
        Jogo.salvar();
        p.sendSystemMessage(Component.literal("Seus itens ficaram no seu corpo em " + dim + " " + Math.round(x) + " " + Math.round(y) + " " + Math.round(z)
            + ". Qualquer jogador pode abrir com o botão direito. Use /corpo para ver depois.").withStyle(ChatFormatting.YELLOW));
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
        avisarDono(p, c);
        fecharSeVazio(c);
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
            if (nivel != null) {
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
