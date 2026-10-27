package br.melby.melbybet;

import br.melby.melbybet.core.Bolao;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Melbybet implements ModInitializer {
    public static final String MOD_ID = "melbybet";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int tick;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Comandos.registrar(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(Jogo::iniciar);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Presenca.aoDesligar();
            Jogo.salvar();
        });
        Presenca.registrar();
        Chat.registrar();
        ServerTickEvents.END_SERVER_TICK.register(server -> aCadaTick());
        ServerLivingEntityEvents.AFTER_DEATH.register(Melbybet::aoMorrer);
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, Extras::decorar);

        LOGGER.info("MelbyBet carregado. /bet e /top estão prontos.");
    }

    private void aCadaTick() {
        if (Jogo.server == null) return;
        tick++;
        Gestos.aCadaTick();
        if (tick % 20 == 0) {
            long agora = System.currentTimeMillis();
            boolean mudou = false;
            for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
                String uuid = p.getUUID().toString();
                String nome = p.getGameProfile().name();
                if (!nome.equals(Jogo.casa.conhecidos.get(uuid))) {
                    Jogo.casa.conhecidos.put(uuid, nome);
                    mudou = true;
                }
                Jogo.entregar(p);
            }
            for (Bolao b : Jogo.casa.bolaos) {
                if (b.aberto() && b.expiraEm > 0 && agora >= b.expiraEm) {
                    Jogo.casa.fechar(b);
                    Jogo.avisar("O bolão \"" + b.titulo + "\" fechou para apostas!");
                    mudou = true;
                }
            }
            if (mudou) Jogo.salvar();
        }
        if (tick % 20 == 0) Extras.aCadaSegundo();
        if (tick % 20 == 0) Capsulas.aCadaSegundo();
        if (tick % 600 == 0) Jogo.conferirConquistas();
        if (tick % 6000 == 0) Jogo.salvar();
    }

    /** Morte no Void, Wither e Dragão resolvem bolões na hora. */
    private static void aoMorrer(LivingEntity entidade, DamageSource fonte) {
        if (Jogo.server == null) return;
        if (entidade instanceof ServerPlayer morto) Coords.avisarMorte(morto);
        if (entidade instanceof ServerPlayer p && fonte.typeHolder().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            resolverPorJogador(Bolao.Gatilho.VOID, p.getGameProfile().name());
            return;
        }
        Bolao.Gatilho g = null;
        if (entidade instanceof WitherBoss) g = Bolao.Gatilho.WITHER;
        else if (entidade instanceof EnderDragon) g = Bolao.Gatilho.DRAGAO;
        if (g == null) return;
        if (fonte.getEntity() instanceof ServerPlayer matador) {
            resolverPorJogador(g, matador.getGameProfile().name());
        } else {
            for (Bolao b : Jogo.casa.comGatilho(g)) {
                if (b.status == Bolao.Status.AGUARDANDO) continue;
                Jogo.casa.aguardar(b, b.opcoes);
                Jogo.avisar("Bolão #" + b.id + ": o alvo morreu sem um jogador claro como autor. Um OP confirma: /melbybet resolver " + b.id + " <nick>");
            }
            Jogo.salvar();
        }
    }

    private static void resolverPorJogador(Bolao.Gatilho g, String nick) {
        for (Bolao b : Jogo.casa.comGatilho(g)) {
            if (b.status == Bolao.Status.AGUARDANDO) continue;
            String opcao = b.opcaoExata(nick);
            if (opcao != null) Jogo.resolver(b, opcao);
        }
    }
}
