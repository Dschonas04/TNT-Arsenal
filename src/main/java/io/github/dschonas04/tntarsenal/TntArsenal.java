package io.github.dschonas04.tntarsenal;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;
import io.github.dschonas04.tntarsenal.nuclear.block.Tier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * Registriert einen Block und ein Block-Item je {@link Kind}, dazu einen Kreativ-Reiter, der sie
 * alle auflistet. Alles andere steckt in den Blöcken selbst und in dem Mixin, das eine Detonation
 * an die richtige Art übergibt.
 */
public final class TntArsenal implements ModInitializer {
    public static final String MOD_ID = "tnt_arsenal";
    public static final List<Item> ITEMS = new ArrayList<>();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Einstiegspunkte für das Server-Mixin; die Aufgabenliste selbst bleibt paketintern. */
    public static void tickTasks() {
        Tasks.tickAll();
    }

    public static void clearTasks() {
        Tasks.clear();
    }

    /** Einstiegspunkt für das Mixin: der Blitz und die Funken, mit denen jede Art hochgeht. */
    public static void burst(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.item.PrimedTnt tnt,
            Kind kind) {
        Fuses.burst(level, tnt, kind);
        Gimmicks.detonated(level, tnt, kind);
    }

    @Override
    public void onInitialize() {
        // Ein Mob, den Stasis-TNT eingefroren hat und der vor dem Auftauen entladen wurde, taut
        // auf, wenn er
        // wieder geladen wird.
        ServerEntityEvents.ENTITY_LOAD.register(
                (entity, level) -> {
                    if (entity instanceof Mob mob && mob.removeTag(Detonations.STASIS_TAG))
                        mob.setNoAi(false);
                });
        Gimmicks.register();
        Particles.init();
        for (Kind kind : Kind.values()) {
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id(kind.id()));
            Block block =
                    new ArsenalTntBlock(
                            kind, BlockBehaviour.Properties.ofFullCopy(Blocks.TNT).setId(blockKey));
            Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(kind.id()));
            // Eine Zeile unter dem Namen sagt, was diese Art tut.
            ItemLore lore =
                    new ItemLore(
                            List.of(
                                    Component.translatable(
                                                    "block.tnt_arsenal." + kind.id() + ".desc")
                                            .withStyle(ChatFormatting.GRAY)));
            Item item =
                    new BlockItem(
                            block,
                            new Item.Properties()
                                    .setId(itemKey)
                                    .useBlockDescriptionPrefix()
                                    .component(DataComponents.LORE, lore));
            Registry.register(BuiltInRegistries.ITEM, itemKey, item);
            ITEMS.add(item);
        }

        ResourceKey<Item> slingKey = ResourceKey.create(Registries.ITEM, id("tnt_sling"));
        ITEMS.add(
                Registry.register(
                        BuiltInRegistries.ITEM,
                        slingKey,
                        new SlingItem(
                                new Item.Properties()
                                        .setId(slingKey)
                                        .durability(256)
                                        .component(
                                                DataComponents.LORE,
                                                new ItemLore(
                                                        List.of(
                                                                Component.translatable(
                                                                                "item.tnt_arsenal.tnt_sling.desc")
                                                                        .withStyle(
                                                                                ChatFormatting
                                                                                        .GRAY)))))));

        // Der Vanilla-Builder hält den Typ seiner Item-Ausgabe geschützt, deshalb kommen die
        // Gegenstände
        // stattdessen über Fabrics Ereignis für diesen Reiter hinein.
        ResourceKey<CreativeModeTab> tabKey =
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("tnt"));
        CreativeModeTab tab =
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.tnt_arsenal"))
                        .icon(() -> new ItemStack(NuclearTnt.BOMBS.get(Tier.NUKE)))
                        .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, tab);
        CreativeModeTabEvents.modifyOutputEvent(tabKey)
                .register(
                        output -> {
                            // TabVisibility ist ebenfalls geschützt, die Stapellisten nicht.
                            // erst das gewöhnliche Arsenal, dann das atomare: Bomben, Zünder,
                            // Schutz, Alarme
                            List<Item> all = new ArrayList<>(ITEMS);
                            all.addAll(NuclearTnt.ITEMS);
                            for (Item item : all) {
                                output.getDisplayStacks().add(new ItemStack(item));
                                output.getSearchTabStacks().add(new ItemStack(item));
                            }
                        });
    }
}
