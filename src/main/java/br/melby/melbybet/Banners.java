package br.melby.melbybet;

import br.melby.melbybet.core.BannerReceita;
import br.melby.melbybet.core.BannerReceita.Camada;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /banner: catálogo de estandartes (bandeiras e símbolos) com a receita passo a passo, e um construtor com pré-visualização.
 * Tudo é só para olhar: os estandartes do menu não podem ser pegos, então não dá item de graça. Não precisa de nada no cliente.
 */
public final class Banners {
    private Banners() {}

    private static List<BannerReceita> catalogo = new ArrayList<>();
    private static BannerReceita.Favoritos favoritos = new BannerReceita.Favoritos();
    private static Path arq;
    private static boolean sujo;
    /** O que cada jogador está montando no construtor. */
    private static final Map<UUID, BannerReceita> RASCUNHOS = new HashMap<>();

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("banners-favoritos.json");
        favoritos = BannerReceita.Favoritos.ler(arq);
        sujo = false;
        try (var in = Banners.class.getResourceAsStream("/melbybet/banners.json")) {
            if (in != null) catalogo = BannerReceita.lerCatalogo(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Melbybet.LOGGER.error("Não consegui ler o catálogo de banners", e);
        }
    }

    public static void salvarAgora() {
        if (arq == null || !sujo) return;
        try {
            favoritos.gravar(arq);
            sujo = false;
        } catch (Exception e) {
            Melbybet.LOGGER.error("Falha ao salvar {}", arq, e);
        }
    }

    public static void aCadaSegundo() {
        if (sujo) salvarAgora();
    }

    public static void aoSair(ServerPlayer p) {
        RASCUNHOS.remove(p.getUUID());
    }

    // ---------- o item de pré-visualização ----------

    private static DyeColor cor(String id) {
        try {
            return DyeColor.valueOf(id.toUpperCase());
        } catch (Exception e) {
            return DyeColor.WHITE;
        }
    }

    /** O estandarte com os padrões aplicados. Padrão que o jogo não conhecer é pulado. */
    static ItemStack item(ServerPlayer p, BannerReceita r) {
        ItemStack st = new ItemStack(Items.BANNER.pick(cor(r.base)));
        try {
            Registry<BannerPattern> reg = p.level().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
            List<BannerPatternLayers.Layer> camadas = new ArrayList<>();
            for (Camada c : r.camadas) {
                Holder<BannerPattern> h = reg.get(Identifier.fromNamespaceAndPath("minecraft", c.padrao)).orElse(null);
                if (h != null) camadas.add(new BannerPatternLayers.Layer(h, cor(c.cor)));
            }
            st.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(camadas));
        } catch (Exception e) {
            Melbybet.LOGGER.warn("Banner sem padrões na pré-visualização: {}", e.toString());
        }
        return st;
    }

    private static GuiElementBuilder vitrine(ServerPlayer p, BannerReceita r, String titulo, ChatFormatting cor, String... extra) {
        GuiElementBuilder b = GuiElementBuilder.from(item(p, r)).setName(Component.literal(titulo).withStyle(cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        b.addLoreLine(Component.literal("Base " + BannerReceita.CORES.get(r.base).toLowerCase() + ", " + r.camadas.size() + " camada(s)").withStyle(ChatFormatting.GRAY));
        for (String s : extra) b.addLoreLine(Component.literal(s).withStyle(ChatFormatting.DARK_GRAY));
        return b;
    }

    private static GuiElementBuilder btn(Item item, String nome, ChatFormatting cor, String... lore) {
        GuiElementBuilder e = new GuiElementBuilder(item).setName(Component.literal(nome).withStyle(cor, ChatFormatting.BOLD)).hideDefaultTooltip();
        for (String l : lore) e.addLoreLine(Component.literal(l).withStyle(ChatFormatting.GRAY));
        return e;
    }

    static void receita(ServerPlayer p, BannerReceita r) {
        p.sendSystemMessage(Component.literal("Receita: " + (r.nome.isEmpty() ? "seu estandarte" : r.nome)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String s : r.passos()) p.sendSystemMessage(Component.literal(" " + s).withStyle(ChatFormatting.WHITE), false);
        p.sendSystemMessage(Component.literal(r.resumoMateriais()).withStyle(ChatFormatting.GRAY), false);
        p.sendSystemMessage(Component.literal("No tear, ponha o estandarte, o corante e (se pedir) o item de padrão, e escolha o desenho. A ordem das camadas importa.")
            .withStyle(ChatFormatting.DARK_GRAY), false);
    }

    // ---------- catálogo ----------

    private static final int POR_PAGINA = 45;

    static void abrirCatalogo(ServerPlayer p, int pagina) {
        int paginas = Math.max(1, (catalogo.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pg = Math.max(0, Math.min(pagina, paginas - 1));
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        g.setTitle(Component.literal("Estandartes").withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pg * POR_PAGINA + i;
            if (idx >= catalogo.size()) break;
            BannerReceita r = catalogo.get(idx);
            g.setSlot(i, vitrine(p, r, r.nome, ChatFormatting.YELLOW, "Clique: ver a receita no chat").setCallback((ix, ty, a, gg) -> receita(p, r)).build());
        }
        g.setSlot(45, btn(Items.CRAFTING_TABLE, "Construtor", ChatFormatting.GREEN, "Monte o seu estandarte camada por camada").setCallback((i, ty, a, gg) -> construtor(p)).build());
        g.setSlot(46, btn(Items.NETHER_STAR, "Favoritos", ChatFormatting.LIGHT_PURPLE, favoritos.de(p.getUUID().toString()).size() + " guardado(s)")
            .setCallback((i, ty, a, gg) -> abrirFavoritos(p)).build());
        if (pg > 0) g.setSlot(48, btn(Items.SPECTRAL_ARROW, "Página anterior", ChatFormatting.AQUA).setCallback((i, ty, a, gg) -> abrirCatalogo(p, pg - 1)).build());
        g.setSlot(49, btn(Items.BOOK, "Página " + (pg + 1) + " de " + paginas, ChatFormatting.WHITE, catalogo.size() + " estandartes").build());
        if (pg < paginas - 1) g.setSlot(50, btn(Items.SPECTRAL_ARROW, "Próxima página", ChatFormatting.AQUA).setCallback((i, ty, a, gg) -> abrirCatalogo(p, pg + 1)).build());
        g.setSlot(53, btn(Items.BARRIER, "Fechar", ChatFormatting.RED).setCallback((i, ty, a, gg) -> gg.close()).build());
        g.open();
    }

    static void abrirFavoritos(ServerPlayer p) {
        List<BannerReceita> l = favoritos.de(p.getUUID().toString());
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(Component.literal("Seus favoritos").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (int i = 0; i < Math.min(l.size(), 18); i++) {
            BannerReceita r = l.get(i);
            g.setSlot(i, vitrine(p, r, r.nome, ChatFormatting.YELLOW, "Esquerdo: receita no chat", "Direito: apagar", "Shift: abrir no construtor").setCallback((ix, ty, a, gg) -> {
                if (ty.shift) {
                    RASCUNHOS.put(p.getUUID(), r.copia());
                    construtor(p);
                } else if (ty.isRight) {
                    favoritos.apagar(p.getUUID().toString(), r.nome);
                    sujo = true;
                    abrirFavoritos(p);
                } else {
                    receita(p, r);
                }
            }).build());
        }
        if (l.isEmpty()) g.setSlot(13, btn(Items.PAPER, "Nenhum favorito ainda", ChatFormatting.GRAY, "Monte um no construtor e use Salvar").build());
        g.setSlot(18, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> abrirCatalogo(p, 0)).build());
        g.open();
    }

    // ---------- construtor ----------

    static void construtor(ServerPlayer p) {
        BannerReceita r = RASCUNHOS.computeIfAbsent(p.getUUID(), k -> new BannerReceita("", "white", new ArrayList<>()));
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(Component.literal("Construtor de estandarte").withStyle(ChatFormatting.GREEN));
        GuiElementBuilder prev = vitrine(p, r, "Seu estandarte", ChatFormatting.GOLD);
        int n = 1;
        for (Camada c : r.camadas) prev.addLoreLine(Component.literal(n++ + ". " + BannerReceita.PADROES.get(c.padrao) + " " + BannerReceita.CORES.get(c.cor).toLowerCase()).withStyle(ChatFormatting.WHITE));
        g.setSlot(4, prev.build());
        g.setSlot(10, btn(Items.WHITE_WOOL, "Cor da base", ChatFormatting.AQUA, "Atual: " + BannerReceita.CORES.get(r.base)).setCallback((i, ty, a, gg) -> escolherCor(p, "Cor da base", c -> {
            r.base = c;
            construtor(p);
        }, () -> construtor(p))).build());
        boolean cheio = r.camadas.size() >= BannerReceita.MAX_CAMADAS;
        g.setSlot(12, btn(Items.PAINTING, "Adicionar camada", ChatFormatting.GREEN, cheio ? "Já tem " + BannerReceita.MAX_CAMADAS + " camadas (o limite do tear)" : "Escolha o padrão e depois a cor")
            .setCallback((i, ty, a, gg) -> {
                if (!cheio) escolherPadrao(p, 0, r);
            }).build());
        g.setSlot(14, btn(Items.FEATHER, "Remover a última camada", ChatFormatting.YELLOW).setCallback((i, ty, a, gg) -> {
            if (!r.camadas.isEmpty()) r.camadas.remove(r.camadas.size() - 1);
            construtor(p);
        }).build());
        g.setSlot(16, btn(Items.LAVA_BUCKET, "Recomeçar", ChatFormatting.RED).setCallback((i, ty, a, gg) -> {
            RASCUNHOS.remove(p.getUUID());
            construtor(p);
        }).build());
        g.setSlot(18, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> abrirCatalogo(p, 0)).build());
        g.setSlot(21, btn(Items.BOOK, "Ver a receita", ChatFormatting.AQUA, "Manda no chat o passo a passo").setCallback((i, ty, a, gg) -> receita(p, r)).build());
        g.setSlot(23, btn(Items.NETHER_STAR, "Salvar nos favoritos", ChatFormatting.LIGHT_PURPLE, "Pede um nome no chat").setCallback((i, ty, a, gg) -> {
            if (r.camadas.isEmpty()) {
                p.sendSystemMessage(Component.literal("Adicione pelo menos uma camada antes de salvar.").withStyle(ChatFormatting.RED), true);
                return;
            }
            Chat.perguntar(p, "Que nome dar a este estandarte?", txt -> {
                String err = favoritos.salvar(p.getUUID().toString(), txt, r);
                if (err != null) p.sendSystemMessage(Component.literal(err).withStyle(ChatFormatting.RED));
                else {
                    sujo = true;
                    p.sendSystemMessage(Component.literal("\"" + txt + "\" salvo nos favoritos (/banner favoritos).").withStyle(ChatFormatting.GREEN));
                }
                construtor(p);
            });
        }).build());
        g.open();
    }

    private static final Item[] CORANTES_ICONE = new Item[16];

    private static void escolherCor(ServerPlayer p, String titulo, java.util.function.Consumer<String> aoEscolher, Runnable voltar) {
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x3, p, false);
        g.setTitle(Component.literal(titulo).withStyle(ChatFormatting.AQUA));
        int s = 0;
        for (String id : BannerReceita.CORES.keySet()) {
            int slot = s < 8 ? 1 + s : 10 + (s - 8);
            s++;
            g.setSlot(slot, btn(Items.DYE.pick(cor(id)), BannerReceita.CORES.get(id), ChatFormatting.WHITE).setCallback((i, ty, a, gg) -> aoEscolher.accept(id)).build());
        }
        g.setSlot(18, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> voltar.run()).build());
        g.open();
    }

    private static void escolherPadrao(ServerPlayer p, int pagina, BannerReceita r) {
        List<String> ids = new ArrayList<>(BannerReceita.PADROES.keySet());
        int paginas = Math.max(1, (ids.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pg = Math.max(0, Math.min(pagina, paginas - 1));
        SimpleGui g = new SimpleGui(MenuType.GENERIC_9x6, p, false);
        g.setTitle(Component.literal("Escolha o padrão").withStyle(ChatFormatting.GREEN));
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pg * POR_PAGINA + i;
            if (idx >= ids.size()) break;
            String id = ids.get(idx);
            BannerReceita amostra = new BannerReceita("", "white", new ArrayList<>(List.of(new Camada(id, "black"))));
            String item = BannerReceita.ITEM_DO_PADRAO.get(id);
            GuiElementBuilder b = vitrine(p, amostra, BannerReceita.PADROES.get(id), ChatFormatting.YELLOW, item == null ? "Só corante" : "Precisa de: " + item);
            g.setSlot(i, b.setCallback((ix, ty, a, gg) -> escolherCor(p, "Cor do padrão", c -> {
                r.camadas.add(new Camada(id, c));
                construtor(p);
            }, () -> escolherPadrao(p, pg, r))).build());
        }
        g.setSlot(45, btn(Items.ARROW, "Voltar", ChatFormatting.GRAY).setCallback((i, ty, a, gg) -> construtor(p)).build());
        if (pg > 0) g.setSlot(48, btn(Items.SPECTRAL_ARROW, "Página anterior", ChatFormatting.AQUA).setCallback((i, ty, a, gg) -> escolherPadrao(p, pg - 1, r)).build());
        g.setSlot(49, btn(Items.BOOK, "Página " + (pg + 1) + " de " + paginas, ChatFormatting.WHITE).build());
        if (pg < paginas - 1) g.setSlot(50, btn(Items.SPECTRAL_ARROW, "Próxima página", ChatFormatting.AQUA).setCallback((i, ty, a, gg) -> escolherPadrao(p, pg + 1, r)).build());
        g.open();
    }

    // ---------- comando ----------

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("banner")
            .then(literal("construir").executes(c -> {
                construtor(c.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("favoritos").executes(c -> {
                abrirFavoritos(c.getSource().getPlayerOrException());
                return 1;
            }))
            .then(literal("receita").then(argument("nome", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(catalogo.stream().map(x -> x.nome).toList(), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String nome = StringArgumentType.getString(c, "nome").trim();
                    for (BannerReceita r : catalogo) if (r.nome.equalsIgnoreCase(nome)) {
                        receita(p, r);
                        return 1;
                    }
                    for (BannerReceita r : favoritos.de(p.getUUID().toString())) if (r.nome.equalsIgnoreCase(nome)) {
                        receita(p, r);
                        return 1;
                    }
                    p.sendSystemMessage(Component.literal("Não achei o estandarte \"" + nome + "\".").withStyle(ChatFormatting.RED), true);
                    return 0;
                })))
            .executes(c -> {
                abrirCatalogo(c.getSource().getPlayerOrException(), 0);
                return 1;
            }));
    }
}
