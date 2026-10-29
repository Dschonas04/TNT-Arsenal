// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Die eigenen Geräusche der Mod, synthetisiert von tools/sounds.py; Dateien in
 * assets/tnt_arsenal/sounds, aufgeführt in sounds.json.
 */
public final class Sounds {
    private Sounds() {}

    /** Ein vier Sekunden langes Auf und Ab der Luftschutzsirene. */
    public static final SoundEvent SIREN = register("siren");

    /** Ein Klicken eines Geigerzählers (drei Varianten). */
    public static final SoundEvent GEIGER = register("geiger");

    /** Eine Atomexplosion: Knall, Druckwelle, rollender Donner. */
    public static final SoundEvent NUKE_BLAST = register("nuke_blast");

    /** Der Countdown-Piepton einer gezündeten Bombe und des Zeitzünders. */
    public static final SoundEvent BEEP = register("beep");

    /** Der Fernzünder löst aus. */
    public static final SoundEvent DETONATOR = register("detonator");

    private static SoundEvent register(String name) {
        Identifier id = NuclearTnt.id(name);
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Lädt die Klasse, die die Geräusche registriert. */
    public static void register() {}
}
