package br.melby.melbybet.client;

import br.melby.melbybet.ConstrucaoRede;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Editor de armor stand com sliders. As mudanças vão ao servidor na hora e você vê o resultado no próprio boneco. */
public class ArmorStandTela extends Screen {
    private static final String[] PARTES = {"Cabeça", "Corpo", "Braço esq.", "Braço dir.", "Perna esq.", "Perna dir."};
    private final int entidade;
    private final int parte;
    private long ultimoEnvio;

    public ArmorStandTela(int entidade, int parte) {
        super(Component.literal("Armor stand"));
        this.entidade = entidade;
        this.parte = parte;
    }

    /** O armor stand para onde o jogador olha (a até 6 blocos), ou null. */
    public static ArmorStand olhado() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        Vec3 de = mc.player.getEyePosition();
        Vec3 ate = de.add(mc.player.getLookAngle().scale(6.0));
        ArmorStand melhor = null;
        double dist = Double.MAX_VALUE;
        for (ArmorStand a : mc.level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(de, ate).inflate(1.0))) {
            Optional<Vec3> hit = a.getBoundingBox().inflate(0.1).clip(de, ate);
            if (hit.isEmpty()) continue;
            double d = de.distanceToSqr(hit.get());
            if (d < dist) {
                dist = d;
                melhor = a;
            }
        }
        return melhor;
    }

    private ArmorStand stand() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(entidade) instanceof ArmorStand a ? a : null;
    }

    private Rotations pose(ArmorStand a, int i) {
        return switch (i) {
            case 0 -> a.getHeadPose();
            case 1 -> a.getBodyPose();
            case 2 -> a.getLeftArmPose();
            case 3 -> a.getRightArmPose();
            case 4 -> a.getLeftLegPose();
            default -> a.getRightLegPose();
        };
    }

    private void enviar(ConstrucaoRede.PosePayload p) {
        ClientPlayNetworking.send(p);
    }

    /** Slider de -180 a 180 graus (ou 0 a 360 no giro do corpo). */
    private class Angulo extends AbstractSliderButton {
        private final String nome;
        private final double min, max;
        private final java.util.function.DoubleConsumer aoMudar;

        Angulo(int x, int y, int w, String nome, double min, double max, double valor, java.util.function.DoubleConsumer aoMudar) {
            super(x, y, w, 20, Component.empty(), (Math.max(min, Math.min(max, valor)) - min) / (max - min));
            this.nome = nome;
            this.min = min;
            this.max = max;
            this.aoMudar = aoMudar;
            updateMessage();
        }

        private double graus() {
            return Math.round(min + this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(nome + ": " + (int) graus() + "°"));
        }

        @Override
        protected void applyValue() {
            long agora = System.currentTimeMillis();
            if (agora - ultimoEnvio < 70) return;
            ultimoEnvio = agora;
            aoMudar.accept(graus());
        }

        @Override
        public void onRelease(net.minecraft.client.input.MouseButtonEvent e) {
            super.onRelease(e);
            aoMudar.accept(graus());
        }
    }

    private float[] angulosAtuais = new float[3];

    @Override
    protected void init() {
        ArmorStand a = stand();
        if (a == null) return;
        int w = 150, x = 8, y = 8;
        for (int i = 0; i < PARTES.length; i++) {
            final int p = i;
            Button b = this.addRenderableWidget(Button.builder(Component.literal(PARTES[i]), bt -> Minecraft.getInstance().setScreenAndShow(new ArmorStandTela(entidade, p)))
                .bounds(x + (i % 2) * 76, y + (i / 2) * 22, 74, 20).build());
            b.active = i != parte;
        }
        y += 3 * 22 + 4;
        Rotations r = pose(a, parte);
        angulosAtuais = new float[] {r.x(), r.y(), r.z()};
        String[] eixos = {"X (frente/trás)", "Y (giro)", "Z (lados)"};
        for (int e = 0; e < 3; e++) {
            final int eixo = e;
            this.addRenderableWidget(new Angulo(x, y, w, eixos[e], -180, 180, angulosAtuais[e], g -> {
                angulosAtuais[eixo] = (float) g;
                enviar(new ConstrucaoRede.PosePayload(entidade, 0, parte, angulosAtuais[0], angulosAtuais[1], angulosAtuais[2], false));
            }));
            y += 22;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Zerar " + PARTES[parte].toLowerCase()), b -> {
            enviar(new ConstrucaoRede.PosePayload(entidade, 0, parte, 0, 0, 0, false));
            Minecraft.getInstance().setScreenAndShow(new ArmorStandTela(entidade, parte));
        }).bounds(x, y, w, 20).build());
        y += 28;
        this.addRenderableWidget(new Angulo(x, y, w, "Giro do corpo", -180, 180, ((a.getYRot() + 180) % 360 + 360) % 360 - 180, g -> enviar(new ConstrucaoRede.PosePayload(entidade, 4, 0, (float) g, 0, 0, false))));
        y += 26;
        this.addRenderableWidget(Button.builder(Component.literal("Braços: " + (a.showArms() ? "Sim" : "Não")), b -> {
            enviar(new ConstrucaoRede.PosePayload(entidade, 1, 0, 0, 0, 0, !a.showArms()));
            b.setMessage(Component.literal("Braços: " + (!a.showArms() ? "Sim" : "Não")));
        }).bounds(x, y, w, 20).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Base: " + (a.showBasePlate() ? "Sim" : "Não")), b -> {
            enviar(new ConstrucaoRede.PosePayload(entidade, 2, 0, 0, 0, 0, !a.showBasePlate()));
            b.setMessage(Component.literal("Base: " + (!a.showBasePlate() ? "Sim" : "Não")));
        }).bounds(x, y, w, 20).build());
        y += 22;
        this.addRenderableWidget(Button.builder(Component.literal("Invisível: " + (a.isInvisible() ? "Sim" : "Não")), b -> {
            enviar(new ConstrucaoRede.PosePayload(entidade, 3, 0, 0, 0, 0, !a.isInvisible()));
            b.setMessage(Component.literal("Invisível: " + (!a.isInvisible() ? "Sim" : "Não")));
        }).bounds(x, y, w, 20).build());
        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> this.onClose()).bounds(x, y, w, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // sem fundo escuro: o boneco precisa continuar visível
    }

    @Override
    public void tick() {
        if (stand() == null) this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
