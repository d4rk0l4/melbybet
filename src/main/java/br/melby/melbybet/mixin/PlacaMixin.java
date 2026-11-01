package br.melby.melbybet.mixin;

import br.melby.melbybet.Placas;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Depois que o jogador escreve na placa, os códigos (&c, &l, degradê) viram texto colorido. */
@Mixin(SignBlockEntity.class)
public abstract class PlacaMixin {
    @Inject(method = "updateSignText", at = @At("TAIL"), require = 0)
    private void melbybet$cores(Player player, boolean front, List<FilteredText> lines, CallbackInfo ci) {
        Placas.converter((SignBlockEntity) (Object) this, front);
    }
}
