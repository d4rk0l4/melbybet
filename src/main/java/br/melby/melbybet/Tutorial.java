package br.melby.melbybet;

import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Livro de boas-vindas da MelbyBet: entregue na primeira vez que a pessoa usa /bet. */
public final class Tutorial {
    private Tutorial() {}

    private static Component t(String s, ChatFormatting... f) {
        return Component.literal(s).withStyle(f);
    }

    public static BookElementBuilder livro() {
        BookElementBuilder b = new BookElementBuilder();
        b.setTitle("Guia da MelbyBet");
        b.setAuthor("Gangue do Melby");
        b.addPage(
            t("MelbyBet\n", ChatFormatting.GOLD, ChatFormatting.BOLD),
            t("A casa de apostas da Gangue.\n\n", ChatFormatting.DARK_GRAY),
            t("Você aposta diamantes de verdade em coisas que vão acontecer no servidor, e quem acerta divide o pote.\n\n", ChatFormatting.BLACK),
            t("Abra o menu com /bet", ChatFormatting.DARK_AQUA));
        b.addPage(
            t("Como apostar\n", ChatFormatting.GOLD, ChatFormatting.BOLD),
            t("1. Digite /bet\n2. Clique no bolão\n3. Clique na cabeça de quem você acha que vai acontecer\n4. Escolha quantos diamantes\n\n", ChatFormatting.BLACK),
            t("Os diamantes saem do seu inventário na hora.", ChatFormatting.DARK_GRAY));
        b.addPage(
            t("Regras\n", ChatFormatting.GOLD, ChatFormatting.BOLD),
            t("- Não pode apostar em você mesmo\n- Há um limite de diamantes por bolão\n- All-in pede confirmação\n- A casa fica com 5% do pote\n- Dá para desistir, mas paga uma taxa", ChatFormatting.BLACK));
        b.addPage(
            t("Prêmio\n", ChatFormatting.GOLD, ChatFormatting.BOLD),
            t("Quando o bolão termina, o prêmio vem em diamantes direto para o seu inventário. Se estiver cheio ou você estiver offline, ele fica guardado até dar.\n\n", ChatFormatting.BLACK),
            t("Se ninguém acertar, o pote vira jackpot e vai para o próximo bolão.", ChatFormatting.DARK_GRAY));
        b.addPage(
            t("Outros comandos\n", ChatFormatting.GOLD, ChatFormatting.BOLD),
            t("/bet: menu e bolões\n/topbet: ranking de quem mais ganhou\n/top: ranking de stats do servidor\n/bet ajuda: este livro de novo\n\n", ChatFormatting.BLACK),
            t("Boa sorte!", ChatFormatting.DARK_GREEN));
        return b;
    }

    /** Abre o livro para ler, sem entregar o item. */
    public static void ajuda(ServerPlayer p) {
        new BookGui(p, livro()).open();
    }

    /** Na primeira vez: aviso no canto da tela e o livro no inventário. */
    public static void primeiraVez(ServerPlayer p) {
        String id = p.getUUID().toString();
        if (Jogo.casa.tutorial.contains(id)) return;
        Jogo.casa.tutorial.add(id);
        Jogo.salvar();
        p.sendSystemMessage(Component.literal("Primeira vez na MelbyBet? Leia o livro que você recebeu!").withStyle(ChatFormatting.GOLD), true);
        ItemStack livro = livro().asStack();
        p.getInventory().add(livro);
        if (!livro.isEmpty()) p.sendSystemMessage(Jogo.prefixo("Inventário cheio, então não deu para te entregar o livro. Use /bet ajuda para ler."));
    }
}
