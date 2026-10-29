package io.github.dschonas04.tntarsenal.client;

import io.github.dschonas04.tntarsenal.Particles;
import io.github.dschonas04.tntarsenal.TntArsenal;
import io.github.dschonas04.tntarsenal.nuclear.client.GreenFog;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Die Client-Seite des Arsenals: die Partikel und was eine Detonation mit dem Bildschirm der
 * Spieler in der Nähe macht – die Sicht wackelt und nimmt die Farbe der Art an (eisblau, giftgrün,
 * das Gold eines Segens ...). In verstrahltem Land schweben grüne Strahlungsflocken um den Spieler.
 */
public final class ArsenalClient implements ClientModInitializer {
    private static final RandomSource RANDOM = RandomSource.create();
    private static float shake;
    private static double shakeX;
    private static double shakeY;
    private static int tint;
    private static int tintTicks;
    private static int tintLeft;

    @Override
    public void onInitializeClient() {
        ArsenalParticles.register();
        ClientPlayNetworking.registerGlobalReceiver(
                Particles.ScreenEffect.TYPE,
                (payload, context) -> {
                    shake = Math.max(shake, payload.shake());
                    if ((payload.tint() >>> 24) > 0) {
                        tint = payload.tint();
                        tintTicks = Math.max(1, payload.ticks());
                        tintLeft = tintTicks;
                    }
                });
        ClientTickEvents.END_CLIENT_TICK.register(ArsenalClient::tick);
        HudElementRegistry.addLast(TntArsenal.id("tint"), ArsenalClient::drawTint);
    }

    private static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            shake = 0;
            tintLeft = 0;
            return;
        }
        if (mc.isPaused()) return;
        if (tintLeft > 0) tintLeft--;
        if (shake > 0.01f) {
            // wackeln und das letzte Wackeln zurücknehmen, damit die Sicht bebt, aber nicht
            // abdriftet
            double x = (RANDOM.nextDouble() - 0.5) * shake * 8;
            double y = (RANDOM.nextDouble() - 0.5) * shake * 8;
            player.turn(x - shakeX, y - shakeY);
            shakeX = x;
            shakeY = y;
            shake *= 0.86f;
        } else if (shakeX != 0 || shakeY != 0) {
            player.turn(-shakeX, -shakeY);
            shakeX = 0;
            shakeY = 0;
            shake = 0;
        }
        // sichtbare Strahlung: mehr Flocken, je heißer der Boden
        float rate = GreenFog.rate(mc.level, player.blockPosition());
        if (rate > 0.02f) {
            int motes = Mth.clamp((int) (rate * 6), 1, 8);
            for (int i = 0; i < motes; i++) {
                if (RANDOM.nextFloat() > 0.5f) continue;
                mc.level.addParticle(
                        Particles.RADIATION,
                        player.getX() + (RANDOM.nextDouble() - 0.5) * 12,
                        player.getY() + RANDOM.nextDouble() * 4,
                        player.getZ() + (RANDOM.nextDouble() - 0.5) * 12,
                        (RANDOM.nextDouble() - 0.5) * 0.05,
                        0.02,
                        (RANDOM.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    private static void drawTint(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (tintLeft <= 0) return;
        float left = (float) tintLeft / tintTicks;
        int alpha = (int) (((tint >>> 24) & 0xFF) * left);
        if (alpha <= 0) return;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        int rgb = tint & 0xFFFFFF;
        // eine Vignette: acht ineinander liegende Rahmen, am Rand am stärksten, über einem leichten
        // Schleier
        // auf dem ganzen Bildschirm
        int steps = 8, step = Math.max(2, Math.min(w, h) / 5 / steps);
        for (int i = 0; i < steps; i++) {
            int a = alpha * (steps - i) / steps / 2;
            int c = (a << 24) | rgb, o = i * step;
            graphics.fill(o, o, w - o, o + step, c);
            graphics.fill(o, h - o - step, w - o, h - o, c);
            graphics.fill(o, o + step, o + step, h - o - step, c);
            graphics.fill(w - o - step, o + step, w - o, h - o - step, c);
        }
        graphics.fill(0, 0, w, h, ((alpha / 4) << 24) | rgb);
    }
}
