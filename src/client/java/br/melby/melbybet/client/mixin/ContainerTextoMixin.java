package br.melby.melbybet.client.mixin;

import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A grade do tooltip da shulker substitui a lista de texto padrão (opcional: se o jogo mudar, só aparece as duas). */
@Mixin(ItemContainerContents.class)
public abstract class ContainerTextoMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void melbybet$semLista(CallbackInfo ci) {
        ci.cancel();
    }
}
