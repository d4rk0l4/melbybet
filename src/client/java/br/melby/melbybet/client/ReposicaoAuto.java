package br.melby.melbybet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Quando a pilha na mão acaba (colocando bloco, comendo, jogando), traz a próxima igual do inventário. */
public final class ReposicaoAuto {
    private ReposicaoAuto() {}

    private static ItemStack anterior = ItemStack.EMPTY;
    private static int slotAnterior = -1;
    private static int espera;

    public static void aCadaTick(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null || !InventarioCliente.perfil.reporBloco) {
            anterior = ItemStack.EMPTY;
            return;
        }
        var inv = mc.player.getInventory();
        int sel = inv.getSelectedSlot();
        ItemStack mao = inv.getItem(sel);
        if (espera > 0) espera--;
        boolean livre = mc.gui.screen() == null;
        boolean soltou = mc.options.keyDrop.isDown();
        if (livre && espera == 0 && mao.isEmpty() && !anterior.isEmpty() && sel == slotAnterior && !soltou && anterior.getMaxStackSize() > 1) {
            for (int i = 0; i < 36; i++) {
                if (i == sel || InventarioCliente.perfil.travado(i)) continue;
                ItemStack st = inv.getItem(i);
                if (st.isEmpty() || !ItemStack.isSameItemSameComponents(st, anterior)) continue;
                // número do slot no menu do inventário: hotbar 36 a 44, mochila 9 a 35
                int menu = i < 9 ? 36 + i : i;
                mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menu, sel, ContainerInput.SWAP, mc.player);
                espera = 5;
                break;
            }
        }
        anterior = mao.isEmpty() ? ItemStack.EMPTY : mao.copy();
        slotAnterior = sel;
    }
}
