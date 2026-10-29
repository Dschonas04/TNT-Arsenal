package io.github.dschonas04.tntarsenal.nuclear;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's own sounds, synthesised by tools/sounds.py; files in assets/tnt_arsenal/sounds, listed
 * in sounds.json.
 */
public final class Sounds {
    private Sounds() {}

    /** One four-second rise and fall of the air-raid siren. */
    public static final SoundEvent SIREN = register("siren");

    /** One click of a Geiger counter (three variants). */
    public static final SoundEvent GEIGER = register("geiger");

    /** A nuclear detonation: crack, pressure wave, rolling thunder. */
    public static final SoundEvent NUKE_BLAST = register("nuke_blast");

    /** The countdown beep of a lit bomb and of the timer fuse. */
    public static final SoundEvent BEEP = register("beep");

    /** The remote detonator firing. */
    public static final SoundEvent DETONATOR = register("detonator");

    private static SoundEvent register(String name) {
        Identifier id = NuclearTnt.id(name);
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Loads the class, which registers the sounds. */
    public static void register() {}
}
