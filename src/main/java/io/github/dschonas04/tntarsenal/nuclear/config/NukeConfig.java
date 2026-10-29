// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;
import io.github.dschonas04.tntarsenal.nuclear.block.Tier;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Einstellungen in {@code config/tnt_arsenal-nuclear.json}. Fehlende Einträge behalten ihre
 * Standardwerte, und die Datei wird beim Laden neu geschrieben, damit neue Einträge darin
 * auftauchen.
 */
public final class NukeConfig {
    public int miniRadius = 15;
    public int nukeRadius = 40;
    public int tsarRadius = 80;

    public int miniFuseTicks = 100;
    public int nukeFuseTicks = 200;
    public int tsarFuseTicks = 400;

    /** Wie viele Blöcke die Explosion pro Server-Tick ändern darf. */
    public int blocksPerTick = 2000;

    /**
     * Strahlung: wie lange verseuchtes Land radioaktiv bleibt; bis dahin nimmt sie gleichmäßig ab
     * (48000 Ticks = zwei Minecraft-Tage).
     */
    public long radiationTicks = 48000;

    /** Strahlung: die Dosisleistung am Nullpunkt direkt nach der Explosion, in mSv pro Sekunde. */
    public float miniPeakRate = 2f;

    public float nukePeakRate = 8f;
    public float tsarPeakRate = 25f;

    public float peakRate(Tier tier) {
        return switch (tier) {
            case MINI -> miniPeakRate;
            case NUKE -> nukePeakRate;
            case TSAR -> tsarPeakRate;
        };
    }

    /**
     * Fallout: Ticks nach der Detonation, bevor die Asche zu fallen beginnt, und wie lange das
     * dauert.
     */
    public int falloutDelayTicks = 200;

    public int falloutTicks = 600;

    /** Client: wie dicht der grüne Nebel in verseuchtem Land wird, 0 schaltet ihn ab. */
    public float fogStrength = 1f;

    /** Client: wie viele Partikel der Atompilz pro Tick erzeugen darf. */
    public int cloudParticlesPerTick = 160;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static NukeConfig current = new NukeConfig();

    public static NukeConfig get() {
        return current;
    }

    public int radius(Tier tier) {
        return switch (tier) {
            case MINI -> miniRadius;
            case NUKE -> nukeRadius;
            case TSAR -> tsarRadius;
        };
    }

    public int fuse(Tier tier) {
        return switch (tier) {
            case MINI -> miniFuseTicks;
            case NUKE -> nukeFuseTicks;
            case TSAR -> tsarFuseTicks;
        };
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("tnt_arsenal-nuclear.json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                NukeConfig read = GSON.fromJson(reader, NukeConfig.class);
                if (read != null) current = read;
            } catch (IOException | RuntimeException e) {
                NuclearTnt.LOG.error("Could not read {}, using defaults", file, e);
            }
        }
        save();
    }

    /**
     * Schreibt die aktuellen Einstellungen zurück, damit die Datei jeden Schlüssel mit seinem Wert
     * auflistet.
     */
    public static void save() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("tnt_arsenal-nuclear.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(current, writer);
        } catch (IOException e) {
            NuclearTnt.LOG.warn("Could not write {}", file, e);
        }
    }

    /** Die Einstellungen nach Namen, wie {@code /nuclear config} sie auflistet. */
    public static Map<String, Object> values() {
        Map<String, Object> out = new TreeMap<>();
        for (Field field : NukeConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            try {
                out.put(field.getName(), field.get(current));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    /**
     * Setzt eine Einstellung aus Text und speichert die Datei. Gibt bei Erfolg null zurück, sonst,
     * was falsch war. Gilt ab der nächsten Bombe.
     */
    public static String set(String name, String text) {
        Field field;
        try {
            field = NukeConfig.class.getField(name);
        } catch (NoSuchFieldException e) {
            return "unknown setting";
        }
        if (Modifier.isStatic(field.getModifiers())) return "unknown setting";
        try {
            Class<?> type = field.getType();
            if (type == int.class) field.setInt(current, Integer.parseInt(text));
            else if (type == long.class) field.setLong(current, Long.parseLong(text));
            else if (type == float.class) field.setFloat(current, Float.parseFloat(text));
            else if (type == double.class) field.setDouble(current, Double.parseDouble(text));
            else if (type == boolean.class) field.setBoolean(current, Boolean.parseBoolean(text));
            else return "cannot be set here";
        } catch (NumberFormatException e) {
            return "not a number";
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        save();
        return null;
    }
}
