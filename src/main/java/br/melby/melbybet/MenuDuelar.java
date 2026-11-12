package br.melby.melbybet;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** /duelo sem argumentos: cabeças dos jogadores online; clique em uma para escolher o tipo de duelo. */
public final class MenuDuelar {
    private MenuDuelar() {}

    private static Component t(String s, ChatFormatting... f) {
        return Component.literal(s).withStyle(f);
    }

    public static void abrir(ServerPlayer p) {
        List<ServerPlayer> alvos = new ArrayList<>();
        for (ServerPlayer o : Jogo.server.getPlayerList().getPlayers()) if (!o.getUUID().equals(p.getUUID())) alvos.add(o);
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x4, p, false);
        g.setTitle(t("Duelo: escolha quem desafiar", ChatFormatting.GOLD));
        if (alvos.isEmpty()) {
            g.setSlot(13, new GuiElementBuilder(Items.BARRIER).setName(t("Ninguém online para desafiar", ChatFormatting.GRAY)).hideDefaultTooltip().build());
        }
        int slot = 0;
        for (ServerPlayer o : alvos) {
            if (slot >= 27) break;
            String nick = o.getGameProfile().name();
            g.setSlot(slot++, new GuiElementBuilder(Items.PLAYER_HEAD).setProfile(nick)
                .setName(t(nick, ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .addLoreLine(t("Clique para desafiar", ChatFormatting.GRAY))
                .setCallback((i, ty, a, gg) -> tipo(p, nick)).build());
        }
        g.setSlot(31, new GuiElementBuilder(Items.BARRIER).setName(t("Fechar", ChatFormatting.RED, ChatFormatting.BOLD)).hideDefaultTooltip()
            .setCallback((i, ty, a, gg) -> gg.close()).build());
        g.open();
    }

    private static ServerPlayer achar(String nick) {
        return Jogo.server.getPlayerList().getPlayerByName(nick);
    }

    private static void tipo(ServerPlayer p, String nick) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(t("Duelo contra " + nick, ChatFormatting.GOLD));
        int[] valores = {1, 5, 10, 32};
        int[] pos = {10, 11, 12, 13};
        for (int k = 0; k < valores.length; k++) {
            int v = valores[k];
            g.setSlot(pos[k], new GuiElementBuilder(Items.DIAMOND).setCount(Math.min(v, 64))
                .setName(t(v + " diamante(s) cada", ChatFormatting.AQUA, ChatFormatting.BOLD)).hideDefaultTooltip()
                .addLoreLine(t("Quem matar o outro primeiro leva tudo", ChatFormatting.GRAY))
                .setCallback((i, ty, a, gg) -> {
                    gg.close();
                    ServerPlayer alvo = achar(nick);
                    if (alvo == null) p.sendSystemMessage(Jogo.prefixo(nick + " saiu."));
                    else Duelos.propor(p, alvo, v);
                }).build());
        }
        g.setSlot(14, new GuiElementBuilder(Items.NAME_TAG).setName(t("Outro valor", ChatFormatting.GREEN, ChatFormatting.BOLD)).hideDefaultTooltip()
            .addLoreLine(t("Você digita no chat", ChatFormatting.GRAY))
            .setCallback((i, ty, a, gg) -> {
                gg.close();
                Chat.perguntar(p, "Quantos diamantes cada um? (digite o número)", txt -> {
                    try {
                        int v = Integer.parseInt(txt.trim());
                        ServerPlayer alvo = achar(nick);
                        if (v < 1) p.sendSystemMessage(Jogo.prefixo("Use um número maior que zero."));
                        else if (alvo == null) p.sendSystemMessage(Jogo.prefixo(nick + " saiu."));
                        else Duelos.propor(p, alvo, v);
                    } catch (NumberFormatException e) {
                        p.sendSystemMessage(Jogo.prefixo("Isso não é um número."));
                    }
                });
            }).build());
        g.setSlot(16, new GuiElementBuilder(Items.CHEST).setName(t("Duelo de itens", ChatFormatting.GOLD, ChatFormatting.BOLD)).hideDefaultTooltip()
            .addLoreLine(t("Aposta itens escolhidos do inventário do outro", ChatFormatting.GRAY))
            .setCallback((i, ty, a, gg) -> {
                gg.close();
                ServerPlayer alvo = achar(nick);
                if (alvo == null) p.sendSystemMessage(Jogo.prefixo(nick + " saiu."));
                else Duelos.proporItens(p, alvo);
            }).build());
        g.setSlot(18, new GuiElementBuilder(Items.ARROW).setName(t("Voltar", ChatFormatting.GRAY, ChatFormatting.BOLD)).hideDefaultTooltip()
            .setCallback((i, ty, a, gg) -> abrir(p)).build());
        g.open();
    }
}
