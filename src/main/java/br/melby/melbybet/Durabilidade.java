package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.commands.Commands.literal;

/** Aviso na action bar quando uma ferramenta ou peça de armadura que você está usando está quase quebrando. */
public final class Durabilidade {
    private Durabilidade() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
        EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Último nível de aviso dado (1 = 10%, 2 = 5%) por jogador, espaço e item. */
    private static final Map<String, Integer> AVISADO = new HashMap<>();

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("durabilidade").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String nick = p.getGameProfile().name().toLowerCase();
            boolean desligou = Jogo.casa.config.semAvisoDurabilidade.add(nick);
            if (!desligou) Jogo.casa.config.semAvisoDurabilidade.remove(nick);
            Jogo.salvar();
            p.sendSystemMessage(Jogo.prefixo("Aviso de durabilidade " + (desligou ? "desligado." : "ligado.")));
            return 1;
        }));
    }

    public static int nivel(int restante, int maximo) {
        if (maximo <= 0) return 0;
        double pct = restante * 100.0 / maximo;
        if (pct <= 5) return 2;
        if (pct <= 10) return 1;
        return 0;
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        if (Jogo.server == null) return;
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            if (Jogo.casa.config.semAvisoDurabilidade.contains(p.getGameProfile().name().toLowerCase())) continue;
            for (EquipmentSlot slot : SLOTS) checar(p, slot);
        }
    }

    private static void checar(ServerPlayer p, EquipmentSlot slot) {
        String chave = p.getUUID() + "|" + slot.getName();
        ItemStack st = p.getItemBySlot(slot);
        if (st.isEmpty() || !st.isDamageableItem()) {
            AVISADO.remove(chave);
            return;
        }
        int restante = st.getMaxDamage() - st.getDamageValue();
        int nivel = nivel(restante, st.getMaxDamage());
        String id = chave + "|" + st.getItem();
        Integer antes = AVISADO.get(chave);
        if (nivel == 0) {
            AVISADO.remove(chave);
            return;
        }
        int ultimo = antes == null ? 0 : antes % 10;
        boolean mesmoItem = antes != null && AVISADO.containsKey(chave) && ultimo > 0 && (antes / 10) == st.getItem().hashCode();
        if (mesmoItem && ultimo >= nivel) return;
        AVISADO.put(chave, st.getItem().hashCode() * 10 + nivel);
        String nome = st.getDisplayName().getString().replace("[", "").replace("]", "");
        p.sendSystemMessage(Component.literal("⚠ " + nome + " está quase quebrando: " + restante + (restante == 1 ? " uso restante" : " usos restantes"))
            .withStyle(nivel >= 2 ? ChatFormatting.RED : ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        if (nivel >= 2) Jogo.som(p, "minecraft:block.note_block.bass", 0.7f);
    }
}
