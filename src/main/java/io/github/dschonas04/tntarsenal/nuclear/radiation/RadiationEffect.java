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
 * Radiation sickness as one effect, shown like poison or regeneration with its own icon and a level.
 * It replaces the bundle of nausea, weakness, hunger, slowness and wither that radiation used to cause.
 *
 * <ul>
 *   <li>I (from 100 mSv): the player gets hungry faster, blows and pace suffer a little</li>
 *   <li>II (from 250 mSv): more of the same</li>
 *   <li>III (from 500 mSv): also half a heart of damage every two seconds</li>
 *   <li>IV (from 1000 mSv): a whole heart every second</li>
 * </ul>
 */
public final class RadiationEffect extends MobEffect {
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x7CFF3A);
        // Vanilla scales both by the level (amplifier + 1): -8 % damage and -3 % speed per level.
        addAttributeModifier(Attributes.ATTACK_DAMAGE, NuclearTnt.id("radiation_weakness"), -0.08,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, NuclearTnt.id("radiation_slowness"), -0.03,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % (amplifier >= 3 ? 20 : 40) == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) player.getFoodData().addExhaustion(0.8f * (amplifier + 1));
        if (amplifier >= 2) entity.hurtServer(level, Radiation.damage(level), amplifier >= 3 ? 2f : 1f);
        return true;
    }
}
