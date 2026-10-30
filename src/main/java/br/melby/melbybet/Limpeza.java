package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static net.minecraft.commands.Commands.literal;

/** Limpeza automática dos itens largados no chão, com aviso no chat. Configurada em /limpar. */
public final class Limpeza {
    private Limpeza() {}

    /** Instante (ms) da próxima limpeza; 0 = ainda não marcado. */
    private static long proxima;

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("limpar")
            .requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer()))
            .executes(c -> {
                MenuLimpeza.abrir(c.getSource().getPlayerOrException(), null);
                return 1;
            })
            .then(literal("aviso")
                .then(com.mojang.brigadier.builder.RequiredArgumentBuilder.<CommandSourceStack, String>argument("valor", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((c, s) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[] {"sim", "nao"}, s))
                    .executes(c -> {
                        String v = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "valor").toLowerCase();
                        boolean liga;
                        if (v.equals("sim") || v.equals("s") || v.equals("on")) liga = true;
                        else if (v.equals("nao") || v.equals("não") || v.equals("n") || v.equals("off")) liga = false;
                        else {
                            c.getSource().sendFailure(Component.literal("Use: /limpar aviso sim  ou  /limpar aviso nao"));
                            return 0;
                        }
                        Jogo.casa.config.limpezaAviso = liga;
                        Jogo.salvar();
                        c.getSource().sendSuccess(() -> Component.literal("Aviso da limpeza " + (liga ? "ligado." : "desligado.")), false);
                        return 1;
                    })))
            .then(literal("agora").executes(c -> {
                executar();
                return 1;
            })));
    }

    /** Recalcula a próxima limpeza (depois de mudar o intervalo, ou de limpar). */
    public static void reagendar() {
        int min = Jogo.casa.config.limpezaMinutos;
        proxima = min > 0 ? System.currentTimeMillis() + min * 60_000L : 0;
        ultimoMs = -1;
    }

    /** Quanto falta para a próxima limpeza, em ms; -1 se estiver desligada. */
    public static long restanteMs() {
        if (Jogo.casa.config.limpezaMinutos <= 0 || proxima == 0) return -1;
        return Math.max(0, proxima - System.currentTimeMillis());
    }

    private static boolean valioso(ItemStack s) {
        if (s.is(Items.DIAMOND) || s.is(Items.DIAMOND_BLOCK) || s.is(Items.NETHERITE_INGOT) || s.is(Items.NETHERITE_SCRAP)
            || s.is(Items.ELYTRA) || s.is(Items.TOTEM_OF_UNDYING) || s.is(Items.NETHER_STAR)) return true;
        var enc = s.get(DataComponents.ENCHANTMENTS);
        if (enc != null && !enc.isEmpty()) return true;
        return s.get(DataComponents.CUSTOM_NAME) != null;
    }

    /** Apaga os itens do chão agora, respeitando as proteções. Devolve quantos itens (entidades) saíram. */
    public static int executar() {
        Casa.Config cfg = Jogo.casa.config;
        int removidos = 0;
        int protegidos = 0;
        for (ServerLevel nivel : Jogo.server.getAllLevels()) {
            for (Entity e : nivel.getAllEntities()) {
                if (!(e instanceof ItemEntity it)) continue;
                if (it.tickCount < cfg.limpezaIdadeMinSeg * 20) {
                    protegidos++;
                    continue;
                }
                if (cfg.limpezaProtege && valioso(it.getItem())) {
                    protegidos++;
                    continue;
                }
                it.discard();
                removidos++;
            }
        }
        Component msg = Component.literal("[Limpeza] ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            .append(Component.literal(removidos == 0 ? "Nada para apagar no chão."
                : removidos + " item(ns) do chão foram apagados." + (protegidos > 0 ? " (" + protegidos + " ficaram: recém-largados ou valiosos)" : ""))
                .withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) p.sendSystemMessage(msg);
        reagendar();
        return removidos;
    }

    private static long ultimoMs = -1;

    private static void avisar(long segundos) {
        String t = segundos >= 60 ? (segundos / 60) + (segundos / 60 == 1 ? " minuto" : " minutos") : segundos + (segundos == 1 ? " segundo" : " segundos");
        Component chat = Component.literal("[Limpeza] ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
            .append(Component.literal("Os itens do chão serão apagados em " + t + ". Pegue o que quiser!").withStyle(ChatFormatting.WHITE));
        Component barra = Component.literal("Limpeza do chão em " + t).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            if (segundos > 5) p.sendSystemMessage(chat);
            else p.sendSystemMessage(barra, true);
        }
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        if (Jogo.server == null) return;
        if (Jogo.casa.config.limpezaMinutos <= 0) {
            proxima = 0;
            ultimoMs = -1;
            return;
        }
        if (proxima == 0) reagendar();
        long resta = proxima - System.currentTimeMillis();
        if (resta <= 0) {
            executar();
            return;
        }
        if (ultimoMs >= 0) {
            long aviso = br.melby.melbybet.core.Limpeza.cruzado(br.melby.melbybet.core.Limpeza.limiares(Jogo.casa.config.limpezaAvisoSeg), ultimoMs, resta);
            if (aviso > 0 && Jogo.casa.config.limpezaAviso) avisar(aviso);
        }
        ultimoMs = resta;
    }
}
