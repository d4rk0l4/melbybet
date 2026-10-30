package br.melby.melbybet.mixin;

import br.melby.melbybet.Prefixos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nome na tab com o degradê de cor que o jogador escolheu. */
@Mixin(ServerPlayer.class)
public abstract class TabNomeMixin {
    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true, require = 0)
    private void melbybet$tab(CallbackInfoReturnable<Component> cir) {
        Component c = Prefixos.nomeNaTab((ServerPlayer) (Object) this);
        if (c != null) cir.setReturnValue(c);
    }
}
