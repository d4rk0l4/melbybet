package br.melby.melbybet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Botão de "novas trocas" na tela do villager (precisa do mod no cliente). Vale como no jogo normal, quando se quebra e
 * repõe a bancada: só enquanto o villager ainda não foi usado (nível 1, sem experiência e nenhuma troca feita).
 */
public final class TrocasVillager {
    private TrocasVillager() {}

    public static void registrar() {
        PayloadTypeRegistry.serverboundPlay().register(RefreshPayload.TYPE, RefreshPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RefreshPayload.TYPE, (payload, ctx) -> atualizar(ctx.player()));
    }

    private static void atualizar(ServerPlayer p) {
        if (!(p.containerMenu instanceof MerchantMenu)) return;
        Villager v = null;
        for (Villager c : p.level().getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(10.0))) {
            if (c.getTradingPlayer() == p) {
                v = c;
                break;
            }
        }
        if (v == null) return;
        var prof = v.getVillagerData().profession();
        if (prof.is(VillagerProfession.NONE) || prof.is(VillagerProfession.NITWIT)) return;
        if (v.getVillagerXp() > 0 || v.getVillagerData().level() > 1) {
            aviso(p, "Esse villager já foi usado: não dá mais para trocar as ofertas.");
            return;
        }
        for (MerchantOffer o : v.getOffers()) {
            if (o.getUses() > 0) {
                aviso(p, "Esse villager já foi usado: não dá mais para trocar as ofertas.");
                return;
            }
        }
        v.setOffers(null);
        var novas = v.getOffers();
        p.sendMerchantOffers(p.containerMenu.containerId, novas, 1, v.getVillagerXp(), true, v.canRestock());
        Jogo.som(p, "minecraft:entity.villager.work_librarian", 1.0f);
    }

    private static void aviso(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED), true);
    }
}
