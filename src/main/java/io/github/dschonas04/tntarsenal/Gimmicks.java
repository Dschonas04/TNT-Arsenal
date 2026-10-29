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
 * Statistics and advancements: how many TNT a player has set off and thrown, which kinds they have
 * tried, and the advancements of the "TNT Arsenal" tab, which the game cannot detect on its own and
 * are awarded here.
 */
public final class Gimmicks {
    public static final Identifier DETONATED = TntArsenal.id("tnt_detonated");
    public static final Identifier THROWN = TntArsenal.id("tnt_thrown");

    /**
     * Each kind a player has set off is remembered as an entity tag on the player, which survives
     * relogs.
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
     * Awards one advancement of the arsenal tab; the advancements all have a single criterion
     * "done".
     */
    public static void award(ServerPlayer player, String name) {
        AdvancementHolder holder =
                player.level().getServer().getAdvancements().get(TntArsenal.id("arsenal/" + name));
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    /** Called for every arsenal TNT that goes off; the player who lit it gets the credit. */
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
