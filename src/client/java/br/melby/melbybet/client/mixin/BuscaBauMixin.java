package br.melby.melbybet.client.mixin;

import br.melby.melbybet.core.Busca;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import br.melby.melbybet.RefreshPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Barra de busca em cima de baús, barris e shulker boxes: digite o nome do item e os que combinam ficam destacados
 * (os outros escurecem); "Pegar" leva todos os que combinam para o inventário. Tudo no cliente, usando o idioma do jogo.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class BuscaBauMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;

    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput input);

    @Unique private EditBox melbybet$busca;

    protected BuscaBauMixin(Component titulo) {
        super(titulo);
    }

    @Unique
    private AbstractContainerScreen<?> melbybet$tela() {
        return (AbstractContainerScreen<?>) (Object) this;
    }

    @Unique
    private boolean melbybet$ehRecipiente() {
        var menu = melbybet$tela().getMenu();
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu;
    }

    @Unique
    private boolean melbybet$combina(ItemStack st) {
        if (melbybet$busca == null || st.isEmpty()) return false;
        String q = melbybet$busca.getValue();
        String id = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath().replace('_', ' ');
        return Busca.melhor(q, st.getHoverName().getString(), id) > 0;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void melbybet$criarBusca(CallbackInfo ci) {
        melbybet$busca = null;
        if (melbybet$tela().getMenu() instanceof MerchantMenu mm) {
            // villager novo (ainda sem uso) e servidor com o mod: botão para trocar as ofertas
            if (mm.getTraderLevel() <= 1 && mm.getTraderXp() == 0 && ClientPlayNetworking.canSend(RefreshPayload.TYPE)) {
                this.addRenderableWidget(Button.builder(Component.literal("Novas trocas"), b -> ClientPlayNetworking.send(new RefreshPayload()))
                    .bounds(this.leftPos + this.imageWidth - 86, this.topPos - 22, 86, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Troca as ofertas, como quebrar e repor a bancada. Só vale antes da primeira troca.")))
                    .build());
            }
            return;
        }
        if (!melbybet$ehRecipiente()) return;
        int larg = Math.min(this.imageWidth - 64, 150);
        int x = this.leftPos + (this.imageWidth - (larg + 56)) / 2;
        int y = this.topPos - 22;
        EditBox caixa = new EditBox(this.font, x, y, larg, 18, Component.literal("Buscar item"));
        caixa.setHint(Component.literal("Buscar item..."));
        caixa.setMaxLength(40);
        melbybet$busca = this.addRenderableWidget(caixa);
        this.addRenderableWidget(Button.builder(Component.literal("Pegar"), b -> melbybet$pegarTodos())
            .bounds(x + larg + 4, y - 1, 52, 20).build());
    }

    @Unique
    private void melbybet$pegarTodos() {
        if (melbybet$busca == null || melbybet$busca.getValue().isBlank()) return;
        for (Slot slot : melbybet$tela().getMenu().slots) {
            if (slot.container instanceof Inventory) continue;
            if (melbybet$combina(slot.getItem())) this.slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void melbybet$destacar(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (melbybet$busca == null || melbybet$busca.getValue().isBlank() || slot.container instanceof Inventory) return;
        if (slot.getItem().isEmpty()) return;
        if (melbybet$combina(slot.getItem())) g.outline(slot.x - 1, slot.y - 1, 18, 18, 0xFFFFC928);
        else g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB0000000);
    }
}
