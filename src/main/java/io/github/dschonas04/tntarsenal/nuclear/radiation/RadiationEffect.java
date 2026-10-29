package io.github.dschonas04.tntarsenal.nuclear.radiation;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Strahlenkrankheit als ein Effekt, angezeigt wie Gift oder Regeneration mit eigenem Symbol und
 * einer Stufe. Er ersetzt das Bündel aus Übelkeit, Schwäche, Hunger, Langsamkeit und Verdorren, das
 * Strahlung früher verursacht hat.
 *
 * <ul>
 *   <li>I (ab 100 mSv): Der Spieler wird schneller hungrig, Schläge und Tempo leiden etwas
 *   <li>II (ab 250 mSv): mehr davon
 *   <li>III (ab 500 mSv): dazu alle zwei Sekunden ein halbes Herz Schaden
 *   <li>IV (ab 1000 mSv): jede Sekunde ein ganzes Herz
 * </ul>
 */
public final class RadiationEffect extends MobEffect {
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x7CFF3A);
        // Vanilla skaliert beides mit der Stufe (Verstärker + 1): -8 % Schaden und -3 % Tempo pro
        // Stufe.
        addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                NuclearTnt.id("radiation_weakness"),
                -0.08,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                NuclearTnt.id("radiation_slowness"),
                -0.03,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % (amplifier >= 3 ? 20 : 40) == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player player)
            player.getFoodData().addExhaustion(0.8f * (amplifier + 1));
        if (amplifier >= 2)
            entity.hurtServer(level, Radiation.damage(level), amplifier >= 3 ? 2f : 1f);
        return true;
    }
}
