package br.melby.melbybet;

import br.melby.melbybet.core.Busca;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /find <item>: olhando para um baú, tira o item dele e coloca direto no seu inventário. */
public final class Find {
    private Find() {}

    private static Map<String, String> nomesPt;

    private static Map<String, String> nomesPt() {
        if (nomesPt == null) {
            try (var in = Find.class.getResourceAsStream("/melbybet/nomes_pt.json")) {
                nomesPt = in == null ? new HashMap<>() : new Gson().fromJson(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {}.getType());
            } catch (Exception e) {
                nomesPt = new HashMap<>();
            }
        }
        return nomesPt;
    }

    private static String idDe(ItemStack st) {
        return BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
    }

    private static String nomeEn(ItemStack st) {
        return st.getDisplayName().getString().replace("[", "").replace("]", "");
    }

    private static String nomeBonito(ItemStack st) {
        String pt = nomesPt().get(idDe(st));
        return pt != null ? pt : nomeEn(st);
    }

    private static int pontos(String consulta, ItemStack st) {
        String id = idDe(st);
        return Busca.melhor(consulta, nomesPt().getOrDefault(id, ""), nomeEn(st), id.replace('_', ' '));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("find")
            .then(argument("item", StringArgumentType.greedyString()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                String txt = StringArgumentType.getString(c, "item").trim();
                int limite = Integer.MAX_VALUE;
                String[] partes = txt.split("\\s+", 2);
                if (partes.length == 2 && partes[0].matches("\\d{1,4}")) {
                    limite = Math.max(1, Integer.parseInt(partes[0]));
                    txt = partes[1];
                }
                return buscar(p, txt, limite);
            })));
    }

    private static int buscar(ServerPlayer p, String consulta, int limite) {
        HitResult h = p.pick(6.0, 1.0F, false);
        if (!(h instanceof BlockHitResult bh)) return erro(p, "Olhe para um baú (a até 6 blocos).");
        var nivel = p.level();
        var pos = bh.getBlockPos();
        if (nivel.getBlockEntity(pos) instanceof BaseContainerBlockEntity bc && !bc.canOpen(p)) return erro(p, "Esse baú está trancado.");
        Container cont = HopperBlockEntity.getContainerAt(nivel, pos);
        if (cont == null) return erro(p, "Isso não é um baú, barril ou outro recipiente.");
        if (!nivel.mayInteract(p, pos)) return erro(p, "Você não pode mexer aí.");

        int melhor = 0;
        ItemStack alvo = ItemStack.EMPTY;
        for (int i = 0; i < cont.getContainerSize(); i++) {
            ItemStack st = cont.getItem(i);
            if (st.isEmpty()) continue;
            int pt = pontos(consulta, st);
            if (pt > melhor) {
                melhor = pt;
                alvo = st;
            }
        }
        if (melhor == 0) return erro(p, "Não achei \"" + consulta + "\" nesse baú.");

        int pegou = 0;
        boolean cheio = false;
        for (int i = 0; i < cont.getContainerSize() && pegou < limite && !cheio; i++) {
            ItemStack st = cont.getItem(i);
            if (st.isEmpty() || !st.is(alvo.getItem())) continue;
            ItemStack copia = st.copy();
            int quer = Math.min(copia.getCount(), limite - pegou);
            copia.setCount(quer);
            p.getInventory().add(copia);
            int deu = quer - copia.getCount();
            if (deu > 0) {
                cont.removeItem(i, deu);
                pegou += deu;
            }
            if (!copia.isEmpty()) cheio = true;
        }
        cont.setChanged();
        p.containerMenu.broadcastChanges();
        if (pegou == 0) return erro(p, "Seu inventário está cheio.");
        Melbybet.LOGGER.info("[find] {} pegou {}x {} do recipiente em {}", p.getGameProfile().name(), pegou, idDe(alvo), pos.toShortString());
        p.sendSystemMessage(Component.literal("Pegou " + pegou + "x " + nomeBonito(alvo) + " do baú." + (cheio ? " Seu inventário encheu; ficou o resto." : ""))
            .withStyle(ChatFormatting.GREEN), true);
        Jogo.som(p, "minecraft:entity.item.pickup", 1.0f);
        return pegou;
    }

    private static int erro(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED), true);
        return 0;
    }
}
