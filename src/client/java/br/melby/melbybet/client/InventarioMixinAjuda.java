package br.melby.melbybet.client;

import br.melby.melbybet.core.Organizar;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Converte pilhas do jogo para o modelo puro de organização. */
public final class InventarioMixinAjuda {
    private InventarioMixinAjuda() {}

    public static String idDe(ItemStack st) {
        return BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
    }

    public static Organizar.Categoria categoriaDe(ItemStack st) {
        return Organizar.categoria(idDe(st), st.has(DataComponents.FOOD), st.getItem() instanceof BlockItem);
    }

    public static Organizar.Pilha pilhaDe(ItemStack st) {
        if (st.isEmpty()) return null;
        String id = idDe(st);
        return new Organizar.Pilha(id, id + "#" + st.getComponents().hashCode(), st.getCount(), st.getMaxStackSize(), categoriaDe(st));
    }
}
