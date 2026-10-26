package br.melby.melbybet;

import br.melby.melbybet.core.Bolao;
import br.melby.melbybet.core.Casa;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Monta uma combinada: escolhe uma opção em cada bolão aberto, define o valor e confirma. */
public final class MenuCombinada {
    private MenuCombinada() {}

    private static final class Rascunho {
        final Map<Integer, String> escolhas = new LinkedHashMap<>();
        int valor = 1;
    }

    private static final Map<UUID, Rascunho> RASCUNHOS = new HashMap<>();
    private static final int[] VALORES = {1, 5, 10, 32, 64};

    private static Component t(String s, ChatFormatting... f) {
        return Component.literal(s).withStyle(f);
    }

    private static GuiElementBuilder btn(Item item, String nome, ChatFormatting cor) {
        return new GuiElementBuilder(item).setName(t(nome, cor, ChatFormatting.BOLD)).hideDefaultTooltip();
    }

    private static Rascunho rascunho(ServerPlayer p) {
        return RASCUNHOS.computeIfAbsent(p.getUUID(), k -> new Rascunho());
    }

    public static void abrir(ServerPlayer p) {
        Casa casa = Jogo.casa;
        Rascunho r = rascunho(p);
        r.escolhas.keySet().removeIf(id -> {
            Bolao b = casa.achar(id);
            return b == null || !b.aberto();
        });
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        g.setTitle(t("Combinada", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        g.setSlot(4, btn(Items.NETHER_STAR, "Como funciona", ChatFormatting.YELLOW)
            .addLoreLine(t("Escolha de 2 a 4 bolões abertos e uma opção em cada.", ChatFormatting.GRAY))
            .addLoreLine(t("Cada perna é uma aposta normal.", ChatFormatting.GRAY))
            .addLoreLine(t("Se acertar todas, a casa paga um bônus (+" + casa.config.combinadaBonusPct + "% por perna extra).", ChatFormatting.GRAY))
            .build());
        int slot = 9;
        for (Bolao b : casa.bolaos) {
            if (!b.aberto() || slot >= 45) continue;
            String esc = r.escolhas.get(b.id);
            GuiElementBuilder e = btn(esc != null ? Items.ENCHANTED_BOOK : Items.WRITABLE_BOOK, "#" + b.id + " " + b.titulo,
                esc != null ? ChatFormatting.GREEN : ChatFormatting.GOLD);
            e.addLoreLine(t(esc != null ? "Sua escolha: " + esc : "Clique para escolher", esc != null ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            e.setCallback((i, ty, a, gg) -> escolher(p, b));
            g.setSlot(slot++, e.build());
        }
        if (slot == 9) g.setSlot(22, btn(Items.BARRIER, "Nenhum bolão aberto", ChatFormatting.GRAY).build());

        int k = r.escolhas.size();
        int total = k * r.valor;
        g.setSlot(46, btn(Items.DIAMOND, "Valor por bolão: " + r.valor, ChatFormatting.AQUA)
            .addLoreLine(t("Clique para mudar (1, 5, 10, 32, 64)", ChatFormatting.GRAY))
            .setCallback((i, ty, a, gg) -> {
                int idx = 0;
                for (int j = 0; j < VALORES.length; j++) if (VALORES[j] == r.valor) idx = j;
                r.valor = VALORES[(idx + 1) % VALORES.length];
                abrir(p);
            }).build());
        GuiElementBuilder ok = btn(k >= 2 ? Items.EMERALD : Items.BARRIER, k >= 2 ? "Confirmar combinada" : "Escolha pelo menos 2 bolões",
            k >= 2 ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        if (k >= 2) {
            ok.addLoreLine(t(k + " bolões x " + r.valor + " = " + total + " diamantes", ChatFormatting.WHITE));
            ok.addLoreLine(t("Bônus se acertar tudo: " + Math.min(casa.bonusDe(k, total), (int) Math.max(0, casa.cofre)) + " (limitado ao cofre)", ChatFormatting.YELLOW));
            ok.setCallback((i, ty, a, gg) -> {
                List<Casa.Perna> pernas = new ArrayList<>();
                r.escolhas.forEach((id, op) -> pernas.add(new Casa.Perna(id, op)));
                String erro = Jogo.apostarCombinada(p, pernas, r.valor);
                if (erro != null) {
                    p.sendSystemMessage(Jogo.prefixo(erro));
                    return;
                }
                RASCUNHOS.remove(p.getUUID());
                MenuBet.abrir(p);
            });
        }
        g.setSlot(49, ok.build());
        g.setSlot(51, btn(Items.BARRIER, "Limpar escolhas", ChatFormatting.RED)
            .setCallback((i, ty, a, gg) -> {
                r.escolhas.clear();
                abrir(p);
            }).build());
        g.setSlot(45, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> MenuBet.abrir(p)).build());
        g.open();
    }

    private static void escolher(ServerPlayer p, Bolao b) {
        Rascunho r = rascunho(p);
        String nome = p.getGameProfile().name();
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x4, p, false);
        g.setTitle(t("#" + b.id + " " + b.titulo, ChatFormatting.GOLD));
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < b.opcoes.size() && i < slots.length; i++) {
            String op = b.opcoes.get(i);
            boolean cabeca = Jogo.casa.config.roster.stream().anyMatch(x -> x.equalsIgnoreCase(op));
            GuiElementBuilder e = btn(cabeca ? Items.PLAYER_HEAD : Items.PAPER, op, ChatFormatting.WHITE);
            if (cabeca) e.setProfile(op);
            if (op.equalsIgnoreCase(nome)) {
                e.addLoreLine(t("Você não pode apostar em si mesmo", ChatFormatting.RED));
            } else {
                if (op.equalsIgnoreCase(r.escolhas.get(b.id))) e.addLoreLine(t("Escolhido", ChatFormatting.GREEN));
                else e.addLoreLine(t("Clique para escolher", ChatFormatting.YELLOW));
                e.setCallback((ix, ty, a, gg) -> {
                    r.escolhas.put(b.id, op);
                    abrir(p);
                });
            }
            g.setSlot(slots[i], e.build());
        }
        if (r.escolhas.containsKey(b.id)) {
            g.setSlot(31, btn(Items.REDSTONE, "Tirar este bolão", ChatFormatting.RED).setCallback((ix, ty, a, gg) -> {
                r.escolhas.remove(b.id);
                abrir(p);
            }).build());
        }
        g.setSlot(27, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((ix, ty, a, gg) -> abrir(p)).build());
        g.open();
    }
}
