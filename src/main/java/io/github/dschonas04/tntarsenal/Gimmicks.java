package io.github.dschonas04.tntarsenal;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.PrimedTnt;

/**
 * Statistiken und Fortschritte: wie viele TNT ein Spieler gezündet und geworfen hat, welche Arten
 * er ausprobiert hat, und die Fortschritte des Reiters „TNT Arsenal“, die das Spiel nicht selbst
 * erkennen kann und die hier vergeben werden.
 */
public final class Gimmicks {
    public static final Identifier DETONATED = TntArsenal.id("tnt_detonated");
    public static final Identifier THROWN = TntArsenal.id("tnt_thrown");

    /**
     * Jede Art, die ein Spieler gezündet hat, wird als Entity-Tag am Spieler gemerkt, der ein
     * Neu-Einloggen übersteht.
     */
    private static final String KIND_TAG = "tnt_arsenal.kind.";

    private Gimmicks() {}

    static void register() {
        for (Identifier stat : new Identifier[] {DETONATED, THROWN}) {
            Registry.register(BuiltInRegistries.CUSTOM_STAT, stat, stat);
            Stats.CUSTOM.get(stat, StatFormatter.DEFAULT);
        }
    }

    /**
     * Vergibt einen Fortschritt des Arsenal-Reiters; die Fortschritte haben alle ein einziges
     * Kriterium „done“.
     */
    public static void award(ServerPlayer player, String name) {
        AdvancementHolder holder =
                player.level().getServer().getAdvancements().get(TntArsenal.id("arsenal/" + name));
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    /**
     * Wird für jedes Arsenal-TNT aufgerufen, das hochgeht; der Spieler, der es gezündet hat,
     * bekommt es angerechnet.
     */
    static void detonated(ServerLevel level, PrimedTnt tnt, Kind kind) {
        if (!(tnt.getOwner() instanceof ServerPlayer player)) return;
        player.awardStat(DETONATED);
        award(player, "first_bang");
        if (kind.party()) award(player, "party");
        player.addTag(KIND_TAG + kind.id());
        long tried = player.entityTags().stream().filter(t -> t.startsWith(KIND_TAG)).count();
        if (tried >= 10) award(player, "ten_kinds");
        if (tried >= Kind.values().length) award(player, "all_kinds");
        if (player.getStats().getValue(Stats.CUSTOM.get(DETONATED)) >= 100)
            award(player, "hundred");
    }
}
