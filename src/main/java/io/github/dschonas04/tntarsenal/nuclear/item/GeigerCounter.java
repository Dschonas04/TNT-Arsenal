// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.item;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;
import io.github.dschonas04.tntarsenal.nuclear.Sounds;
import io.github.dschonas04.tntarsenal.nuclear.radiation.Radiation;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Der Geigerzähler. In einer der beiden Hände gehalten, misst er die Dosisleistung dort, wo der
 * Spieler steht, nach Abschirmung, und die eigene Dosis des Spielers, und zeigt beides über der
 * Hotbar. Er tickt wie ein echter: je höher die Leistung, desto öfter. Alles läuft auf dem Server,
 * der Zähler braucht auf dem Client also nichts.
 */
public final class GeigerCounter {
    private GeigerCounter() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(
                server -> {
                    boolean reading = server.getTickCount() % 10 == 0;
                    for (ServerLevel level : server.getAllLevels()) {
                        for (ServerPlayer player : level.players()) {
                            if (!holds(player)) continue;
                            float rate =
                                    Radiation.rateAt(level, player.blockPosition())
                                            * Radiation.shielding(level, player);
                            tick(level, player, rate);
                            if (reading) show(player, rate);
                        }
                    }
                });
    }

    private static boolean holds(ServerPlayer player) {
        // Ein Zähler, den ein EMP ausgeschaltet hat, hat eine Abklingzeit und bleibt still.
        for (ItemStack stack :
                new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(NuclearTnt.GEIGER_COUNTER) && !player.getCooldowns().isOnCooldown(stack))
                return true;
        }
        return false;
    }

    /** Hintergrundstrahlung klickt ab und zu; eine heiße Zone macht daraus ein Rattern. */
    private static void tick(ServerLevel level, ServerPlayer player, float rate) {
        float chance = Math.min(0.9f, 0.01f + rate * 0.15f);
        if (player.getRandom().nextFloat() >= chance) return;
        float pitch = 0.9f + player.getRandom().nextFloat() * 0.2f;
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                Sounds.GEIGER,
                SoundSource.PLAYERS,
                0.5f,
                pitch);
    }

    private static void show(ServerPlayer player, float rate) {
        float dose = Radiation.dose(player);
        ChatFormatting colour =
                rate >= 5f
                        ? ChatFormatting.DARK_RED
                        : rate >= 1f
                                ? ChatFormatting.RED
                                : rate >= 0.1f
                                        ? ChatFormatting.GOLD
                                        : rate > 0.001f
                                                ? ChatFormatting.YELLOW
                                                : ChatFormatting.GREEN;
        player.sendOverlayMessage(
                Component.translatable(
                                "item.tnt_arsenal.geiger_counter.reading",
                                String.format("%.3f", rate),
                                String.format("%.1f", dose))
                        .withStyle(colour));
    }
}
