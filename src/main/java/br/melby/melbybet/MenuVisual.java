package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Gradiente;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Visual do jogador: apelido e título (escritos no chat pela placa) e cor (única ou degradê) escolhida pelos corantes.
 * Todo jogador muda a cor do próprio nick; OP também escreve apelido e título e edita os de outros.
 */
final class MenuVisual {
    private MenuVisual() {}

    private static final String[] PT = {"Branco", "Laranja", "Magenta", "Azul-claro", "Amarelo", "Verde-limão", "Rosa", "Cinza",
        "Cinza-claro", "Ciano", "Roxo", "Azul", "Marrom", "Verde", "Vermelho", "Preto"};

    private static final class Estado {
        boolean titulo;
        boolean degrade;
        int primeira = -1; // 1ª cor do degradê já escolhida, esperando a 2ª
    }

    private static final Map<UUID, Estado> ESTADOS = new HashMap<>();

    private static GuiElementBuilder btn(Item item, String nome, ChatFormatting cor, String... lore) {
        GuiElementBuilder e = new GuiElementBuilder(item).setName(Component.literal(nome).withStyle(cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        for (String l : lore) e.addLoreLine(Component.literal(l).withStyle(ChatFormatting.GRAY));
        return e;
    }

    private static void aviso(ServerPlayer p, String s) {
        p.sendSystemMessage(Jogo.prefixo(s));
    }

    static void abrir(ServerPlayer p, String alvo, Runnable voltar) {
        boolean admin = Comandos.admin(p);
        boolean proprio = alvo.equalsIgnoreCase(p.getGameProfile().name());
        if (!admin && !proprio) return;
        Estado e = ESTADOS.computeIfAbsent(p.getUUID(), k -> new Estado());
        if (!admin) e.titulo = false;
        String chave = alvo.toLowerCase();
        Casa.CorNick cn = Jogo.casa.config.cores.get(chave);
        Casa.Titulo ti = Jogo.casa.config.titulos.get(chave);

        int[] atual = null;
        if (e.titulo && ti != null) atual = new int[] {ti.cor1, ti.cor2};
        else if (!e.titulo && cn != null) atual = new int[] {cn.cor1, cn.cor2};

        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x5, p, false);
        g.setTitle(Component.literal((e.titulo ? "Título de " : "Visual de ") + alvo).withStyle(ChatFormatting.DARK_PURPLE));

        // prévia: como aparece no chat
        MutableComponent previa = Component.empty();
        if (ti != null) {
            previa.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY));
            previa.append(Prefixos.degrade(ti.texto, ti.cor1, ti.cor2));
            previa.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
        }
        String nomeMostrado = cn != null && cn.apelido != null && !cn.apelido.isBlank() ? cn.apelido : alvo;
        previa.append(cn == null ? Component.literal(nomeMostrado).withStyle(ChatFormatting.WHITE) : Prefixos.degrade(nomeMostrado, cn.cor1, cn.cor2));
        g.setSlot(4, new GuiElementBuilder(Items.NAME_TAG).setName(previa).hideDefaultTooltip()
            .addLoreLine(Component.literal("Assim aparece no chat").withStyle(ChatFormatting.GRAY)).build());

        // abas
        g.setSlot(2, new GuiElementBuilder(Items.PLAYER_HEAD).setProfile(alvo).hideDefaultTooltip()
            .setName(Component.literal("Apelido e cor do nick").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)).glow(!e.titulo)
            .setCallback((i, ty, a, gg) -> {
                e.titulo = false;
                e.primeira = -1;
                abrir(p, alvo, voltar);
            }).build());
        if (admin) {
            g.setSlot(6, btn(Items.PAPER, "Título", ChatFormatting.GOLD, "Tag entre colchetes antes do nick").glow(e.titulo)
                .setCallback((i, ty, a, gg) -> {
                    e.titulo = true;
                    e.primeira = -1;
                    abrir(p, alvo, voltar);
                }).build());
        }

        // placa: escrever o texto no chat (apelido: todo jogador no próprio; título: só OP)
        if (admin || !e.titulo) {
            String textoAtual = e.titulo ? (ti == null ? "(nenhum)" : ti.texto) : (cn == null || cn.apelido == null ? "(o nick de verdade)" : cn.apelido);
            g.setSlot(13, btn(Items.OAK_SIGN, e.titulo ? "Escrever o título" : "Escrever o apelido", ChatFormatting.YELLOW,
                "Atual: " + textoAtual, e.titulo ? "Até 20 letras. Pode usar duas cores: #ff8a24 #ffd23f Lenda" : "Pode usar &c, &l e &#ff8a24; os códigos viram degradê")
                .setCallback((i, ty, a, gg) -> {
                    if (e.titulo) {
                        Chat.perguntar(p, "Digite o título (até 20 letras). Pode começar com duas cores: #ff8a24 #ffd23f Lenda.", txt -> {
                            int[] antes = ti == null ? null : new int[] {ti.cor1, ti.cor2};
                            String erro = Comandos.aplicarTitulo(alvo, txt);
                            Casa.Titulo novo = Jogo.casa.config.titulos.get(chave);
                            if (erro == null && antes != null && novo != null && !txt.trim().startsWith("#")) {
                                novo.cor1 = antes[0];
                                novo.cor2 = antes[1];
                                Jogo.salvar();
                            }
                            aviso(p, erro == null ? "Título aplicado." : erro);
                            abrir(p, alvo, voltar);
                        });
                    } else {
                        Chat.perguntar(p, "Digite o apelido (2 a 16 letras). Códigos de cor como &6Rei&eDoMel viram degradê.", txt -> {
                            String erro = Comandos.aplicarApelido(alvo, txt);
                            aviso(p, erro == null ? "Apelido aplicado." : erro);
                            abrir(p, alvo, voltar);
                        });
                    }
                }).build());
        }

        // modo da cor
        g.setSlot(10, btn(Items.EMERALD, "Cor única", ChatFormatting.GREEN, "Um corante e pronto").glow(!e.degrade)
            .setCallback((i, ty, a, gg) -> {
                e.degrade = false;
                e.primeira = -1;
                abrir(p, alvo, voltar);
            }).build());
        g.setSlot(16, btn(Items.AMETHYST_SHARD, "Degradê", ChatFormatting.LIGHT_PURPLE, "Escolha dois corantes: o 1º é o começo, o 2º o fim").glow(e.degrade)
            .setCallback((i, ty, a, gg) -> {
                e.degrade = true;
                e.primeira = -1;
                abrir(p, alvo, voltar);
            }).build());

        // corantes
        DyeColor[] cores = DyeColor.values();
        for (int n = 0; n < cores.length && n < 16; n++) {
            final int cor = cores[n].getTextureDiffuseColor() & 0xFFFFFF;
            boolean ehPrimeira = e.degrade ? (e.primeira >= 0 ? e.primeira == cor : atual != null && atual[0] == cor) : atual != null && atual[0] == cor && atual[1] == cor;
            boolean ehSegunda = e.degrade && e.primeira < 0 && atual != null && atual[1] == cor && atual[0] != atual[1];
            GuiElementBuilder d = new GuiElementBuilder(Items.DYE.pick(cores[n])).hideDefaultTooltip()
                .setName(Component.literal(PT[n]).withStyle(st -> st.withColor(cor).withBold(true)))
                .addLoreLine(Component.literal(Gradiente.paraHex(cor)).withStyle(ChatFormatting.GRAY));
            if (e.degrade && ehPrimeira) d.addLoreLine(Component.literal("1ª cor (começo)").withStyle(ChatFormatting.YELLOW));
            if (ehSegunda) d.addLoreLine(Component.literal("2ª cor (fim)").withStyle(ChatFormatting.YELLOW));
            if (e.degrade && e.primeira >= 0 && !ehPrimeira) d.addLoreLine(Component.literal("Clique para ser a 2ª cor (fim)").withStyle(ChatFormatting.GREEN));
            if (ehPrimeira || ehSegunda) d.glow();
            d.setCallback((i, ty, a, gg) -> {
                if (!e.degrade) {
                    escolher(p, alvo, e, cor, cor);
                } else if (e.primeira < 0) {
                    e.primeira = cor;
                } else {
                    int ini = e.primeira;
                    e.primeira = -1;
                    escolher(p, alvo, e, ini, cor);
                }
                abrir(p, alvo, voltar);
            });
            g.setSlot(n < 8 ? 18 + n : 27 + (n - 8), d.build());
        }

        g.setSlot(36, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> {
            e.primeira = -1;
            if (voltar != null) voltar.run();
            else gg.close();
        }).build());
        g.setSlot(38, btn(Items.NAME_TAG, "Cor exata", ChatFormatting.AQUA, "Digite no chat: #cor1 [#cor2]", "Ex.: #ff8a24 #ffd23f", "Você recebe um painel para escolher a cor")
            .setCallback((i, ty, a, gg) -> {
                Chat.perguntar(p, "Digite uma ou duas cores, como #ff8a24 #ffd23f.", txt -> {
                    String[] t = txt.trim().split("\\s+");
                    int c1 = Gradiente.hex(t[0]);
                    int c2 = t.length > 1 ? Gradiente.hex(t[1]) : c1;
                    if (c1 < 0 || c2 < 0) aviso(p, "Use cores como #ff8a24.");
                    else escolher(p, alvo, e, c1, c2);
                    abrir(p, alvo, voltar);
                });
                linkDoPainel(p);
            }).build());
        g.setSlot(44, btn(Items.BLAZE_POWDER, "Cores prontas", ChatFormatting.GOLD, "Fogo, oceano, neon e outras")
            .setCallback((i, ty, a, gg) -> prontas(p, alvo, e, voltar)).build());
        g.setSlot(40, btn(Items.BARRIER, "Tirar a cor", ChatFormatting.RED, "Volta ao branco").setCallback((i, ty, a, gg) -> {
            if (e.titulo) {
                if (ti != null) {
                    ti.cor1 = 0xAAAAAA;
                    ti.cor2 = 0xFFFFFF;
                }
            } else if (cn != null) {
                if (cn.apelido == null) Jogo.casa.config.cores.remove(chave);
                else {
                    cn.cor1 = 0xFFFFFF;
                    cn.cor2 = 0xFFFFFF;
                }
            }
            Jogo.salvar();
            Prefixos.esquecer();
            abrir(p, alvo, voltar);
        }).build());
        if (admin || !e.titulo) {
            g.setSlot(42, btn(Items.TNT, e.titulo ? "Tirar o título" : "Tirar o apelido", ChatFormatting.DARK_RED).setCallback((i, ty, a, gg) -> {
                if (e.titulo) {
                    Jogo.casa.config.titulos.remove(chave);
                } else if (cn != null) {
                    cn.apelido = null;
                }
                Jogo.salvar();
                Prefixos.esquecer();
                abrir(p, alvo, voltar);
            }).build());
        }
        g.open();
    }

    /** Manda o link do painel onde dá para escolher a cor e copiar o código. */
    private static void linkDoPainel(ServerPlayer p) {
        String url = Jogo.casa.config.painelCoresUrl;
        if (url == null || url.isBlank()) return;
        Component link = Component.literal("[Abrir o painel de cores]").withStyle(st -> st.withColor(0xFFC928).withBold(true).withUnderlined(true)
            .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)))
            .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal(url))));
        p.sendSystemMessage(Component.literal("Não sabe o código? ").withStyle(ChatFormatting.GRAY).append(link)
            .append(Component.literal(" escolha a cor, copie o código e cole aqui.").withStyle(ChatFormatting.GRAY)));
    }

    private static final Item[] ICONES_PRONTAS = {Items.BLAZE_POWDER, Items.HEART_OF_THE_SEA, Items.SLIME_BALL, Items.EMERALD,
        Items.PORKCHOP, Items.GOLD_INGOT, Items.SNOWBALL, Items.LAVA_BUCKET};

    /** As cores prontas de antes (fogo, oceano, neon...). */
    private static void prontas(ServerPlayer p, String alvo, Estado e, Runnable voltar) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(Component.literal("Cores prontas").withStyle(ChatFormatting.DARK_PURPLE));
        String[] nomes = Gradiente.NOMES_PRESETS;
        for (int i = 0; i < nomes.length && i < ICONES_PRONTAS.length; i++) {
            String nome = nomes[i];
            int[] c = Gradiente.preset(nome);
            g.setSlot(9 + i, new GuiElementBuilder(ICONES_PRONTAS[i]).hideDefaultTooltip()
                .setName(Prefixos.degrade(nome, c[0], c[1]))
                .addLoreLine(Component.literal("Clique para usar" + (e.titulo ? " no título" : " no nick")).withStyle(ChatFormatting.GRAY))
                .setCallback((ix, ty, a, gg) -> {
                    escolher(p, alvo, e, c[0], c[1]);
                    prontas(p, alvo, e, voltar);
                }).build());
        }
        g.setSlot(18, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> abrir(p, alvo, voltar)).build());
        g.open();
    }

    /** Aplica a cor escolhida no nick ou no título. */
    private static void escolher(ServerPlayer p, String alvo, Estado e, int c1, int c2) {
        if (e.titulo) {
            Casa.Titulo ti = Jogo.casa.config.titulos.get(alvo.toLowerCase());
            if (ti == null) {
                aviso(p, "Escreva o título primeiro, com a placa no centro.");
                return;
            }
            ti.cor1 = c1;
            ti.cor2 = c2;
            Jogo.salvar();
            Prefixos.esquecer();
        } else {
            String erro = Comandos.aplicarCor(alvo, Gradiente.paraHex(c1), Gradiente.paraHex(c2));
            if (erro != null) {
                aviso(p, erro);
                return;
            }
        }
        p.sendSystemMessage(Component.literal("Cor aplicada.").withStyle(ChatFormatting.GREEN), true);
    }
}
