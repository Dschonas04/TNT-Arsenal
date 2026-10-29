// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.client;

import io.github.dschonas04.tntarsenal.Particles;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

import org.joml.Quaternionf;

/**
 * Die eigenen Partikel der Mod. Jeder ist ein einzelnes texturiertes Viereck; die Sprites zeichnet
 * tools/particles.py. Leuchtende ignorieren die Helligkeit, damit sie nachts und unter der Erde
 * scheinen.
 */
final class ArsenalParticles {
    private static final int FULL_BRIGHT = 0xF000F0;

    private ArsenalParticles() {}

    @FunctionalInterface
    private interface Maker {
        SingleQuadParticle make(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites);
    }

    private static void register(SimpleParticleType type, Maker maker) {
        ParticleProviderRegistry.getInstance()
                .register(
                        type,
                        sprites ->
                                (ParticleProvider<SimpleParticleType>)
                                        (options, level, x, y, z, xd, yd, zd, random) ->
                                                maker.make(level, x, y, z, xd, yd, zd, sprites));
    }

    static void register() {
        register(Particles.FUSE_SPARK, Spark::new);
        register(Particles.SHOCKWAVE, Shockwave::new);
        register(Particles.SMOKE_BALL, SmokeBall::new);
        register(Particles.EMBER, Ember::new);
        register(Particles.CONFETTI, Confetti::new);
        register(Particles.FROST_SPARKLE, Sparkle::new);
        register(Particles.VOID_SWIRL, Swirl::new);
        register(Particles.RADIATION, Radiation::new);
        register(Particles.ARC, Arc::new);
    }

    /**
     * Gemeinsame Grundlage: die Geschwindigkeit so, wie sie geschickt wurde (der
     * Vanilla-Konstruktor würde sie streuen), und ein Sprite-Satz zur Hand.
     */
    private abstract static class Base extends SingleQuadParticle {
        protected final SpriteSet sprites;

        Base(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0, sprites.first());
            this.sprites = sprites;
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }
    }

    /**
     * Ein Funke von der Lunte: weißglühend bis orange, fällt und springt, nach einer halben Sekunde
     * weg.
     */
    static final class Spark extends Base {
        Spark(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd, sprites);
            lifetime = 6 + random.nextInt(8);
            gravity = 0.6f;
            friction = 0.9f;
            hasPhysics = true;
            quadSize = 0.08f + random.nextFloat() * 0.06f;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            setColor(1f, 1f - t * 0.45f, 0.75f - t * 0.7f);
            setSpriteFromAge(sprites);
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** Ein Ring flach auf dem Boden, der bis zum mitgeschickten Radius hinausrast und verblasst. */
    static final class Shockwave extends Base {
        private final float radius;

        Shockwave(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0, sprites);
            radius = (float) Math.max(1, xd);
            lifetime = 10 + (int) (radius * 0.6);
            gravity = 0;
            hasPhysics = false;
            setColor(1f, 0.95f, 0.85f);
        }

        @Override
        public float getQuadSize(float partialTick) {
            float t = Mth.clamp((age + partialTick) / lifetime, 0f, 1f);
            return radius * (1 - (1 - t) * (1 - t));
        }

        @Override
        public void tick() {
            super.tick();
            setAlpha(0.85f * (1f - (float) age / lifetime));
        }

        @Override
        public FacingCameraMode getFacingCameraMode() {
            return (rotation, camera, partialTick) -> rotation.rotationX((float) (-Math.PI / 2));
        }

        @Override
        public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
            super.extract(state, camera, partialTick);
            // und die Unterseite, damit er auch von unten zu sehen ist
            extractRotatedQuad(
                    state, camera, new Quaternionf().rotationX((float) (Math.PI / 2)), partialTick);
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** Ein Rauchball: schwillt an, steigt auf und wird dünner. */
    static final class SmokeBall extends Base {
        private final float start;

        SmokeBall(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, xd * 0.3, yd * 0.3 + 0.02, zd * 0.3, sprites);
            lifetime = 50 + random.nextInt(50);
            gravity = -0.004f;
            friction = 0.94f;
            start = 0.8f + random.nextFloat() * 0.8f;
            quadSize = start;
            float grey = 0.25f + random.nextFloat() * 0.25f;
            setColor(grey, grey, grey * 0.95f);
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            quadSize = start * (1 + t * 2.2f);
            setAlpha(0.75f * (1 - t * t));
            setSpriteFromAge(sprites);
        }
    }

    /** Eine Glut: steigt und schwankt, kühlt von Gelb über Rot bis Dunkel ab. */
    static final class Ember extends Base {
        private final float phase;

        Ember(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, xd, yd + 0.05, zd, sprites);
            lifetime = 30 + random.nextInt(40);
            gravity = -0.015f;
            friction = 0.93f;
            quadSize = 0.12f + random.nextFloat() * 0.12f;
            phase = random.nextFloat() * 6.28f;
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            xd += Math.sin(age * 0.3 + phase) * 0.004;
            zd += Math.cos(age * 0.27 + phase) * 0.004;
            setColor(1f, Math.max(0.15f, 0.9f - t * 1.1f), Math.max(0.03f, 0.35f - t));
            setAlpha(t < 0.8f ? 1f : (1f - t) * 5f);
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** Ein Fetzen buntes Papier, der taumelnd herunterflattert. */
    static final class Confetti extends Base {
        private static final int[] COLOURS = {
            0xFF3B3B, 0xFFB12B, 0xFFE94A, 0x4BE36B, 0x3BC8FF, 0x6B5BFF, 0xE35BFF, 0xFF7AB0
        };
        private final float spin;

        Confetti(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, xd, yd, zd, sprites);
            lifetime = 50 + random.nextInt(60);
            gravity = 0.05f;
            friction = 0.9f;
            hasPhysics = true;
            quadSize = 0.13f + random.nextFloat() * 0.08f;
            int c = COLOURS[random.nextInt(COLOURS.length)];
            setColor(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
            spin = (random.nextFloat() - 0.5f) * 0.6f;
            roll = random.nextFloat() * 6.28f;
            setSprite(sprites.get(random));
        }

        @Override
        public void tick() {
            oRoll = roll;
            super.tick();
            if (!onGround) {
                roll += spin;
                xd += Math.sin(age * 0.25) * 0.003;
                yd = Math.max(yd, -0.06);
            }
            if (age > lifetime - 20) setAlpha((lifetime - age) / 20f);
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }
    }

    /** Ein eisiges Funkeln, das blinkt, während es treibt. */
    static final class Sparkle extends Base {
        Sparkle(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, xd, yd - 0.01, zd, sprites);
            lifetime = 30 + random.nextInt(30);
            gravity = 0.01f;
            friction = 0.95f;
            quadSize = 0.12f + random.nextFloat() * 0.12f;
            setColor(0.75f, 0.92f, 1f);
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            setAlpha(0.6f + 0.4f * (float) Math.sin(age * 0.8));
        }

        @Override
        public void setSpriteFromAge(SpriteSet sprites) {
            // Blinken: Die Bilder laufen hin und zurück statt einmal durch
            int frame = age % 6;
            setSprite(sprites.get(frame < 4 ? frame : 6 - frame, 3));
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** Eine dunkle Flocke, die spiralförmig in den Punkt hineinzieht, auf den sie zielt. */
    static final class Swirl extends Base {
        private final double cx, cy, cz;

        Swirl(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0, sprites);
            cx = x + xd;
            cy = y + yd;
            cz = z + zd;
            lifetime = 30 + random.nextInt(20);
            gravity = 0;
            hasPhysics = false;
            quadSize = 0.12f + random.nextFloat() * 0.1f;
            float v = random.nextFloat() * 0.3f;
            setColor(0.45f + v, 0.1f, 0.65f + v);
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            double dx = cx - x, dy = cy - y, dz = cz - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 0.3 && Math.abs(dy) < 0.3) {
                remove();
                return;
            }
            // rundherum und mit jeder Runde etwas näher
            double speed = 0.12 + 1.2 / (d + 2);
            x += (-dz / Math.max(d, 0.01)) * speed + dx * 0.06;
            z += (dx / Math.max(d, 0.01)) * speed + dz * 0.06;
            y += dy * 0.06;
            quadSize *= 0.985f;
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** Eine pulsierende grüne Strahlungsflocke. */
    static final class Radiation extends Base {
        private final float phase;

        Radiation(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, xd * 0.2, yd * 0.2 + 0.005, zd * 0.2, sprites);
            lifetime = 60 + random.nextInt(60);
            gravity = 0;
            friction = 0.97f;
            quadSize = 0.06f + random.nextFloat() * 0.08f;
            phase = random.nextFloat() * 6.28f;
            setColor(0.45f, 1f, 0.25f);
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            setAlpha(
                    (0.55f + 0.45f * (float) Math.sin(age * 0.35 + phase))
                            * Math.min(1f, (1 - t) * 4));
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }
    }

    /** Ein knisternder Bogen: jeden Tick ein neuer Zickzack, da und wieder weg. */
    static final class Arc extends Base {
        Arc(
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd,
                SpriteSet sprites) {
            super(level, x, y, z, 0, 0, 0, sprites);
            lifetime = 3 + random.nextInt(5);
            gravity = 0;
            hasPhysics = false;
            quadSize = 0.25f + random.nextFloat() * 0.4f;
            roll = random.nextFloat() * 6.28f;
            setColor(0.75f, 0.9f, 1f);
            setSprite(sprites.get(random));
        }

        @Override
        public void tick() {
            oRoll = roll;
            super.tick();
            roll = random.nextFloat() * 6.28f;
            setSprite(sprites.get(random));
        }

        @Override
        public int getLightCoords(float partialTick) {
            return FULL_BRIGHT;
        }
    }
}
