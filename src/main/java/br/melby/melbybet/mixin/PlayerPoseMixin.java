package br.melby.melbybet.mixin;

import br.melby.melbybet.Gestos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** /crawl: o servidor marca o jogador como rastejando, e assim todos os outros jogadores o veem rastejar. */
@Mixin(Player.class)
public abstract class PlayerPoseMixin {
    @Inject(method = "updatePlayerPose", at = @At("TAIL"), require = 0)
    private void melbybet$rastejar(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer sp && Gestos.rastejando(sp)) {
            self.setPose(Pose.SWIMMING);
        }
    }
}
