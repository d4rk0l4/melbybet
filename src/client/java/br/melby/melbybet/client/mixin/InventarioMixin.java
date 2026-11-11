package br.melby.melbybet.client.mixin;

import br.melby.melbybet.client.InventarioCliente;
import br.melby.melbybet.client.InventarioMixinAjuda;
import br.melby.melbybet.client.PerfilTela;
import br.melby.melbybet.core.Organizar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Botões no inventário e nos baús: Organizar (usa o seu perfil), e nos recipientes "Guardar iguais" e "Pegar iguais".
 * Tudo por cliques normais de inventário, como se você mesmo clicasse.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class InventarioMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;

    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput input);

    protected InventarioMixin(Component titulo) {
        super(titulo);
    }

    @Unique
    private AbstractContainerScreen<?> melbybet$tela() {
        return (AbstractContainerScreen<?>) (Object) this;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void melbybet$botoesInventario(CallbackInfo ci) {
        var menu = melbybet$tela().getMenu();
        boolean recipiente = menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu;
        if (!recipiente && !(melbybet$tela() instanceof InventoryScreen)) return;
        int x = this.leftPos + this.imageWidth + 4;
        int y = this.topPos;
        InventarioCliente.modoTrava = false;
        // contorno dourado nos slots travados (fica por baixo do item)
        this.addRenderableOnly((g, mx, my, d) -> {
            for (Slot s : melbybet$tela().getMenu().slots) {
                if (!(s.container instanceof Inventory) || !InventarioCliente.perfil.travado(s.getContainerSlot())) continue;
                int sx = this.leftPos + s.x, sy = this.topPos + s.y;
                g.fill(sx - 1, sy - 1, sx + 17, sy, 0xFFFFC928);
                g.fill(sx - 1, sy + 16, sx + 17, sy + 17, 0xFFFFC928);
                g.fill(sx - 1, sy, sx, sy + 16, 0xFFFFC928);
                g.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFC928);
            }
        });
        this.addRenderableWidget(Button.builder(Component.literal("Organizar"), b -> melbybet$organizar())
            .bounds(x, y, 78, 20).tooltip(Tooltip.create(Component.literal("Arruma o seu inventário pelo seu perfil e junta pilhas iguais."))).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Travar slots"), b -> {
            InventarioCliente.modoTrava = !InventarioCliente.modoTrava;
            b.setMessage(Component.literal(InventarioCliente.modoTrava ? "Travar: clique" : "Travar slots"));
        }).bounds(x, y, 78, 20).tooltip(Tooltip.create(Component.literal("Ligue e clique nos slots do seu inventário para travar ou destravar. Travado: o Organizar e o Guardar iguais não mexem, e o shift-clique e o drop não tiram o item."))).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Perfil..."), b -> Minecraft.getInstance().setScreenAndShow(new PerfilTela()))
            .bounds(x, y, 78, 20).tooltip(Tooltip.create(Component.literal("Escolha onde você gosta de cada tipo de item."))).build());
        if (!recipiente) return;
        y += 26;
        this.addRenderableWidget(Button.builder(Component.literal("Guardar iguais"), b -> melbybet$guardarIguais())
            .bounds(x, y, 78, 20).tooltip(Tooltip.create(Component.literal("Manda para cá o que você tem e que já existe aqui dentro."))).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Pegar iguais"), b -> melbybet$pegarIguais())
            .bounds(x, y, 78, 20).tooltip(Tooltip.create(Component.literal("Traz para você tudo daqui que você já tem pelo menos uma unidade."))).build());
    }

    /** Versão com o botão como int (protegida). Se não existir nesta versão do jogo, é ignorada. */
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void melbybet$travas(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        if (melbybet$travar(slot, input)) ci.cancel();
    }

    /** Versão com MouseButtonEvent (privada). Se não existir nesta versão do jogo, é ignorada. A decisão é a mesma da outra, então rodar nas duas é seguro. */
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;ILnet/minecraft/client/input/MouseButtonEvent;Lnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void melbybet$travasEvento(Slot slot, int slotId, MouseButtonEvent event, ContainerInput input, CallbackInfo ci) {
        if (melbybet$travar(slot, input)) ci.cancel();
    }

    /** true = cancela o clique. */
    @Unique
    private static boolean melbybet$travar(Slot slot, ContainerInput input) {
        if (slot == null || !(slot.container instanceof Inventory)) return false;
        int i = slot.getContainerSlot();
        if (i < 0 || i >= Organizar.SLOTS) return false;
        if (InventarioCliente.modoTrava) {
            InventarioCliente.perfil.alternarTrava(i);
            InventarioCliente.salvar();
            return true;
        }
        if (InventarioCliente.perfil.travado(i) && (input == ContainerInput.QUICK_MOVE || input == ContainerInput.THROW)) {
            InventarioCliente.aviso("Slot travado. Use o botão Travar slots para destravar.");
            return true;
        }
        return false;
    }

    /** Os slots do jogador (0 a 8 hotbar, 9 a 35 mochila) pela posição no inventário. */
    @Unique
    private Slot[] melbybet$slotsDoJogador() {
        Slot[] r = new Slot[Organizar.SLOTS];
        for (Slot s : melbybet$tela().getMenu().slots) {
            if (!(s.container instanceof Inventory)) continue;
            int i = s.getContainerSlot();
            if (i >= 0 && i < Organizar.SLOTS) r[i] = s;
        }
        return r;
    }

    @Unique
    private void melbybet$clicar(Slot s, ContainerInput tipo) {
        this.slotClicked(s, s.index, 0, tipo);
    }

    @Unique
    private void melbybet$organizar() {
        if (!melbybet$tela().getMenu().getCarried().isEmpty()) {
            InventarioCliente.aviso("Solte o item do cursor antes de organizar.");
            return;
        }
        Slot[] slots = melbybet$slotsDoJogador();
        Organizar.Pilha[] inv = new Organizar.Pilha[Organizar.SLOTS];
        for (int i = 0; i < Organizar.SLOTS; i++) {
            if (slots[i] == null) return;
            inv[i] = InventarioMixinAjuda.pilhaDe(slots[i].getItem());
        }
        List<int[]> ops = Organizar.plano(inv, InventarioCliente.perfil);
        if (ops.isEmpty()) {
            InventarioCliente.aviso("Já está organizado.");
            return;
        }
        for (int[] t : ops) {
            melbybet$clicar(slots[t[0]], ContainerInput.PICKUP);
            melbybet$clicar(slots[t[1]], ContainerInput.PICKUP);
            melbybet$clicar(slots[t[0]], ContainerInput.PICKUP);
        }
        InventarioCliente.aviso("Inventário organizado (" + ops.size() + " trocas).");
    }

    @Unique
    private List<Slot> melbybet$slotsDoRecipiente() {
        List<Slot> r = new ArrayList<>();
        for (Slot s : melbybet$tela().getMenu().slots) if (!(s.container instanceof Inventory)) r.add(s);
        return r;
    }

    @Unique
    private static boolean melbybet$existeIgual(ItemStack st, List<ItemStack> lista) {
        for (ItemStack o : lista) if (!o.isEmpty() && ItemStack.isSameItemSameComponents(st, o)) return true;
        return false;
    }

    @Unique
    private void melbybet$guardarIguais() {
        List<ItemStack> dentro = new ArrayList<>();
        for (Slot s : melbybet$slotsDoRecipiente()) if (s.hasItem()) dentro.add(s.getItem().copy());
        Slot[] meus = melbybet$slotsDoJogador();
        boolean pulaHotbar = InventarioCliente.perfil.guardarIgnoraHotbar;
        int movidos = 0;
        for (int i = pulaHotbar ? 9 : 0; i < Organizar.SLOTS; i++) {
            Slot s = meus[i];
            if (s == null || !s.hasItem() || InventarioCliente.perfil.travado(i) || !melbybet$existeIgual(s.getItem(), dentro)) continue;
            int antes = s.getItem().getCount();
            melbybet$clicar(s, ContainerInput.QUICK_MOVE);
            movidos += antes - (s.hasItem() ? s.getItem().getCount() : 0);
        }
        InventarioCliente.aviso(movidos == 0 ? "Nada para guardar (ou não tem espaço)." : "Guardou " + movidos + " itens.");
    }

    @Unique
    private void melbybet$pegarIguais() {
        List<ItemStack> comigo = new ArrayList<>();
        for (Slot s : melbybet$slotsDoJogador()) if (s != null && s.hasItem()) comigo.add(s.getItem().copy());
        int movidos = 0;
        boolean cheio = false;
        for (Slot s : melbybet$slotsDoRecipiente()) {
            if (!s.hasItem() || !melbybet$existeIgual(s.getItem(), comigo)) continue;
            int antes = s.getItem().getCount();
            melbybet$clicar(s, ContainerInput.QUICK_MOVE);
            int depois = s.hasItem() ? s.getItem().getCount() : 0;
            movidos += antes - depois;
            if (depois == antes) {
                cheio = true;
                break;
            }
        }
        InventarioCliente.aviso(movidos == 0 ? "Nada para pegar (ou o inventário está cheio)." : "Pegou " + movidos + " itens." + (cheio ? " Seu inventário encheu." : ""));
    }
}
