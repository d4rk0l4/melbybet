package br.melby.melbybet.client.mixin;

import br.melby.melbybet.client.ShulkerTooltip;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Shulker box com itens ganha um tooltip em grade, como o do saco (bundle). */
@Mixin(ItemStack.class)
public abstract class ShulkerTooltipMixin {
    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void melbybet$grade(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock)) return;
        ItemContainerContents c = self.get(DataComponents.CONTAINER);
        if (c == null || c.nonEmptyItemCopyStream().findAny().isEmpty()) return;
        NonNullList<ItemStack> lista = NonNullList.withSize(27, ItemStack.EMPTY);
        c.copyInto(lista);
        cir.setReturnValue(Optional.of(new ShulkerTooltip(lista)));
    }
}
