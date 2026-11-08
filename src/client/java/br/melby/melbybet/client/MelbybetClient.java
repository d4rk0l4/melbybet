package br.melby.melbybet.client;

import br.melby.melbybet.ConstrucaoRede;
import br.melby.melbybet.EstatRede;
import br.melby.melbybet.MortesRede;
import br.melby.melbybet.WaypointRede;
import br.melby.melbybet.core.Waypoints;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

public class MelbybetClient implements ClientModInitializer {
	private static KeyMapping teclaLista, teclaCriar, teclaTemp, teclaLigar;
	private static KeyMapping teclaGuias, teclaRegua, teclaEspelho, teclaMateriais, teclaStand, teclaPlaca, teclaLuz, teclaZoom, teclaImagem, teclaFarm, teclaMortes, teclaEstat;
	private static int tick;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category categoria = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("melbybet", "waypoints"));
		teclaLista = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.waypoint_lista", InputConstants.KEY_B, categoria));
		teclaCriar = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.waypoint_criar", InputConstants.KEY_N, categoria));
		teclaTemp = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.waypoint_temp", InputConstants.KEY_J, categoria));
		teclaLigar = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.waypoint_ligar", InputConstants.KEY_H, categoria));

		KeyMapping.Category cons = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("melbybet", "construcao"));
		teclaGuias = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.guias", InputConstants.KEY_G, cons));
		teclaRegua = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.regua", InputConstants.KEY_R, cons));
		teclaEspelho = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.espelho", InputConstants.KEY_Y, cons));
		teclaMateriais = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.materiais", InputConstants.KEY_U, cons));
		teclaStand = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.armorstand", InputConstants.KEY_K, cons));
		teclaPlaca = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.placa", InputConstants.KEY_O, cons));

		teclaLuz = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.luz", InputConstants.KEY_L, cons));
		teclaZoom = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.zoom", InputConstants.KEY_C, cons));
		teclaImagem = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.foto_parede", InputConstants.KEY_F8, cons));

		teclaFarm = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.farm", InputConstants.KEY_V, cons));
		teclaMortes = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.mortes", InputConstants.KEY_M, cons));
		teclaEstat = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.melbybet.estat", InputConstants.KEY_F7, cons));
		ClientPlayNetworking.registerGlobalReceiver(EstatRede.DadosPayload.TYPE, (payload, ctx) -> EstatTela.receber(payload));
		ClientPlayNetworking.registerGlobalReceiver(MortesRede.ListaPayload.TYPE, (payload, ctx) -> MortesTela.receber(payload));
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("melbybet", "farm"), (g, tracker) -> FarmHud.desenhar(g));

		ClientPlayNetworking.registerGlobalReceiver(ConstrucaoRede.MateriaisPayload.TYPE, (payload, ctx) -> {
			ConstrucaoCliente.projeto = payload.nome();
			ConstrucaoCliente.blocos = payload.blocos();
			ConstrucaoCliente.itens = payload.itens();
			ConstrucaoCliente.qtds = payload.qtds();
		});
		ClientPlayNetworking.registerGlobalReceiver(ConstrucaoRede.MapArtPayload.TYPE, (payload, ctx) ->
			ConstrucaoCliente.grade = payload.ativo() ? payload : null);
		ClientPlayNetworking.registerGlobalReceiver(ConstrucaoRede.PlacaDadosPayload.TYPE, (payload, ctx) -> PlacaTela.receber(payload));
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("melbybet", "materiais"), (g, tracker) -> MateriaisHud.desenhar(g));

		InventarioCliente.carregar();
		ClientTooltipComponentCallback.EVENT.register(c -> c instanceof ShulkerTooltip s ? new ShulkerTooltipCliente(s) : null);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			WaypointStore.carregar(client);
			ClientPlayNetworking.send(new br.melby.melbybet.JarRede.OlaPayload(br.melby.melbybet.JarObrigatorio.versaoDoServidor()));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			WaypointStore.descarregar();
			ConstrucaoCliente.limparTudo();
		});

		ClientPlayNetworking.registerGlobalReceiver(WaypointRede.ConfigPayload.TYPE, (payload, ctx) -> WaypointStore.liberado = payload.waypoints());
		ClientPlayNetworking.registerGlobalReceiver(WaypointRede.AreaPayload.TYPE, (payload, ctx) ->
			WaypointStore.area = payload.tem1() || payload.tem2() ? payload : null);
		ClientPlayNetworking.registerGlobalReceiver(WaypointRede.PontoPayload.TYPE, (payload, ctx) -> {
			if (payload.remover()) {
				if (Waypoints.BUSCA.equals(payload.tipo()) && "busca".equals(payload.chave())) WaypointStore.dados.pontos.removeIf(p -> Waypoints.BUSCA.equals(p.tipo));
				else WaypointStore.dados.remover(payload.chave());
			} else {
				WaypointStore.dados.upsertDoServidor(payload.chave(), payload.nome(), payload.dim(), payload.x(), payload.y(), payload.z(),
					payload.tipo(), payload.cor(), System.currentTimeMillis());
				if (Waypoints.CORPO.equals(payload.tipo())) WaypointStore.aviso("Waypoint do seu corpo criado.");
				if (Waypoints.BUSCA.equals(payload.tipo())) {
					var ponto = WaypointStore.dados.porId(payload.chave());
					if (ponto != null) {
						ponto.raio = 4;
						ponto.expiraEm = System.currentTimeMillis() + 10 * 60_000L;
					}
				}
			}
			WaypointStore.salvar();
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("melbybet", "bussola"), (g, tracker) -> WaypointHud.desenhar(g));
		LevelRenderEvents.BEFORE_GIZMOS.register(ctx -> WaypointMundo.desenhar());
		ClientTickEvents.END_CLIENT_TICK.register(MelbybetClient::aCadaTick);
	}

	private static void aCadaTick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		boolean livre = mc.gui.screen() == null;
		while (teclaLista.consumeClick()) {
			if (livre && WaypointStore.ativo()) mc.setScreenAndShow(new WaypointTela(0));
			else if (livre) WaypointStore.aviso("Waypoints desligados neste servidor.");
		}
		while (teclaCriar.consumeClick()) if (livre) criarAqui(mc, false);
		while (teclaTemp.consumeClick()) if (livre) criarAqui(mc, true);
		while (teclaLigar.consumeClick()) {
			if (!livre || !WaypointStore.ativo()) continue;
			WaypointStore.dados.tudoVisivel = !WaypointStore.dados.tudoVisivel;
			WaypointStore.salvar();
			WaypointStore.aviso("Waypoints " + (WaypointStore.dados.tudoVisivel ? "ligados" : "desligados"));
		}
		while (teclaGuias.consumeClick()) if (livre) mc.setScreenAndShow(new GuiasTela());
		while (teclaRegua.consumeClick()) if (livre) GuiasTela.marcarRegua();
		while (teclaEspelho.consumeClick()) if (livre) GuiasTela.definirEspelho();
		while (teclaMateriais.consumeClick()) {
			if (!livre) continue;
			ConstrucaoCliente.materiaisVisivel = !ConstrucaoCliente.materiaisVisivel;
			WaypointStore.aviso("Lista de materiais " + (ConstrucaoCliente.materiaisVisivel ? "visível" : "escondida"));
		}
		while (teclaStand.consumeClick()) {
			if (!livre) continue;
			var a = ArmorStandTela.olhado();
			if (a == null) WaypointStore.aviso("Olhe para um armor stand.");
			else mc.setScreenAndShow(new ArmorStandTela(a.getId(), 0));
		}
		while (teclaPlaca.consumeClick()) if (livre) PlacaTela.pedir();
		while (teclaLuz.consumeClick()) {
			if (!livre) continue;
			LuzCliente.ativo = !LuzCliente.ativo;
			WaypointStore.aviso("Luz dos monstros " + (LuzCliente.ativo ? "ligada (vermelho: nasce sempre; amarelo: nasce à noite)" : "desligada"));
		}
		while (teclaImagem.consumeClick()) if (livre) mc.setScreenAndShow(new FotoTela());
		FotoModo.zoom(mc, livre && teclaZoom.isDown());
		while (teclaFarm.consumeClick()) {
			if (!livre) continue;
			FarmHud.alternar(mc);
			WaypointStore.aviso("Contador de farm " + (FarmHud.ativo ? "ligado (aperte de novo para parar)" : "desligado"));
		}
		while (teclaMortes.consumeClick()) if (livre) MortesTela.abrir();
		while (teclaEstat.consumeClick()) if (livre) EstatTela.abrir();
		FarmHud.aCadaTick(mc);
		ReposicaoAuto.aCadaTick(mc);
		LuzCliente.aCadaTick(mc);
		if (++tick % 10 == 0 && WaypointStore.ativo()) {
			var saiu = WaypointStore.dados.limparTemporarios(System.currentTimeMillis(), WaypointStore.dimAtual(mc), mc.player.getX(), mc.player.getY(), mc.player.getZ());
			if (!saiu.isEmpty()) {
				WaypointStore.salvar();
				WaypointStore.aviso("Waypoint temporário \"" + saiu.get(0).nome + "\" removido.");
			}
		}
	}

	private static void criarAqui(Minecraft mc, boolean temporario) {
		if (!WaypointStore.ativo()) {
			WaypointStore.aviso("Waypoints desligados neste servidor.");
			return;
		}
		Waypoints w = WaypointStore.dados;
		Waypoints.Ponto p = new Waypoints.Ponto();
		p.nome = temporario ? "Temporário" : w.nomeNovo();
		p.dim = WaypointStore.dimAtual(mc);
		p.x = mc.player.blockPosition().getX();
		p.y = mc.player.blockPosition().getY();
		p.z = mc.player.blockPosition().getZ();
		p.cor = Waypoints.corDoIndice(w.pontos.size());
		p.criadoEm = System.currentTimeMillis();
		if (temporario) {
			p.tipo = Waypoints.TEMP;
			p.raio = w.raioPadrao;
			p.expiraEm = w.minutosTemp > 0 ? p.criadoEm + w.minutosTemp * 60_000L : 0;
		}
		String erro = w.adicionar(p);
		if (erro != null) {
			WaypointStore.aviso(erro);
			return;
		}
		WaypointStore.salvar();
		WaypointStore.aviso("Waypoint \"" + p.nome + "\" criado. Aperte B para editar.");
	}
}
