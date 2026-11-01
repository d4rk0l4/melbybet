package br.melby.melbybet.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Villager: shift+clique no resultado da troca repete a troca até acabarem os itens.
 * O jogo já repete enquanto houver item nos quadrinhos de pagamento; aqui o servidor reabastece os
 * quadrinhos a partir do inventário, então vende (ou compra) tudo de uma vez.
 */
@Mixin(MerchantMenu.class)
public abstract class VenderTudoMixin {
    @Unique
    private int melbybet$troca = -1;

    @Inject(method = "setSelectionHint", at = @At("TAIL"), require = 0)
    private void melbybet$guardar(int i, CallbackInfo ci) {
        this.melbybet$troca = i;
    }

    @Inject(method = "quickMoveStack", at = @At("TAIL"), require = 0)
    private void melbybet$repor(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != 2 || this.melbybet$troca < 0 || !(player instanceof ServerPlayer)) return;
        if (cir.getReturnValue() == null || cir.getReturnValue().isEmpty()) return;
        MerchantMenu menu = (MerchantMenu) (Object) this;
        menu.tryMoveItems(this.melbybet$troca);
        // o cliente só previu uma troca: manda o estado real das ofertas para ele mostrar a troca esgotada
        ((ServerPlayer) player).sendMerchantOffers(menu.containerId, menu.getOffers(), menu.getTraderLevel(), menu.getTraderXp(),
            menu.showProgressBar(), menu.canRestock());
    }
}
