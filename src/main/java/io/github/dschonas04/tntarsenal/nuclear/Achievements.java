package io.github.dschonas04.tntarsenal.nuclear;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Die Fortschritte, die das Spiel nicht selbst erkennt (Dosis, eine Detonation). Sie tragen ein
 * unerfüllbares Kriterium namens „done“ und werden von hier vergeben; die übrigen in
 * data/tnt_arsenal/advancement/nuclear/ lösen von selbst aus.
 */
public final class Achievements {
    private Achievements() {}

    public static void award(ServerPlayer player, String name) {
        AdvancementHolder holder =
                player.level().getServer().getAdvancements().get(NuclearTnt.id("nuclear/" + name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }
}
