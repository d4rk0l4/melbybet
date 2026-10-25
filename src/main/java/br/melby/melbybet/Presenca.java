package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Gradiente;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** /seen (última vez visto) e as mensagens de entrar e sair. */
public final class Presenca {
    private Presenca() {}

    private static final Random SORTEIO = new Random();

    public static void registrar() {
        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, mensagem, overlay) -> permitir(mensagem));
    }

    /** Troca as mensagens padrão de entrar e sair pelas da Gangue. */
    private static boolean permitir(Component mensagem) {
        if (Jogo.server == null) return true;
        if (!(mensagem.getContents() instanceof TranslatableContents tc)) return true;
        String chave = tc.getKey();
        boolean entrou = chave.startsWith("multiplayer.player.joined");
        boolean saiu = chave.equals("multiplayer.player.left");
        if (!entrou && !saiu) return true;
        String nick = "alguém";
        if (tc.getArgs().length > 0) {
            Object a = tc.getArgs()[0];
            nick = a instanceof Component c ? c.getString() : String.valueOf(a);
        }
        nick = nick.replaceAll("^\\[[^\\]]*\\]\\s*", "").trim();
        List<String> frases = entrou ? Jogo.casa.config.msgEntrar : Jogo.casa.config.msgSair;
        if (frases.isEmpty()) return true;
        String texto = frases.get(SORTEIO.nextInt(frases.size())).replace("{nick}", nick);
        Component msg = Component.literal(entrou ? "+ " : "- ").withStyle(st -> st.withColor(entrou ? 0x55FF55 : 0xFF5555))
            .append(Prefixos.degrade(texto, entrou ? 0x55FF55 : 0xFF5555, entrou ? 0x55FFFF : 0xFFAA00));
        Jogo.server.getPlayerList().broadcastSystemMessage(msg, false);
        return false;
    }

    // ---------- seen ----------

    private static final Set<java.util.UUID> ONLINE = new HashSet<>();

    /** Chamado a cada segundo: registra entradas, saídas e a última posição. */
    public static void aCadaSegundo() {
        long agora = System.currentTimeMillis();
        Set<java.util.UUID> agoraOnline = new HashSet<>();
        boolean mudou = false;
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            agoraOnline.add(p.getUUID());
            String nome = p.getGameProfile().name();
            Casa.Visto v = Jogo.casa.vistos.computeIfAbsent(nome.toLowerCase(), k -> new Casa.Visto());
            if (!ONLINE.contains(p.getUUID())) {
                v.nome = nome;
                v.uuid = p.getUUID().toString();
                if (v.primeira == 0) v.primeira = agora;
                v.entrou = agora;
                v.online = true;
                mudou = true;
            }
            v.dimensao = p.level().dimension().identifier().toString();
            v.x = (int) Math.floor(p.getX());
            v.y = (int) Math.floor(p.getY());
            v.z = (int) Math.floor(p.getZ());
        }
        for (java.util.UUID id : new HashSet<>(ONLINE)) {
            if (agoraOnline.contains(id)) continue;
            for (Casa.Visto v : Jogo.casa.vistos.values()) {
                if (v.uuid.equals(id.toString()) && v.online) {
                    v.online = false;
                    v.saiu = agora;
                    mudou = true;
                }
            }
        }
        ONLINE.clear();
        ONLINE.addAll(agoraOnline);
        if (mudou) Jogo.salvar();
    }

    /** Ao ligar: ninguém está online. */
    public static void aoLigar() {
        for (Casa.Visto v : Jogo.casa.vistos.values()) v.online = false;
        ONLINE.clear();
    }

    /** Ao desligar: quem estava online saiu agora. */
    public static void aoDesligar() {
        long agora = System.currentTimeMillis();
        for (Casa.Visto v : Jogo.casa.vistos.values()) {
            if (v.online) {
                v.online = false;
                v.saiu = agora;
            }
        }
    }

    public static int corPadrao() {
        return Gradiente.hex("#ffaa00");
    }
}
