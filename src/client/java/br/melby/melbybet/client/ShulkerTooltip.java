package br.melby.melbybet.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Conteúdo de uma shulker box para mostrar como imagem no tooltip (27 posições). */
public record ShulkerTooltip(List<ItemStack> itens) implements TooltipComponent {}
