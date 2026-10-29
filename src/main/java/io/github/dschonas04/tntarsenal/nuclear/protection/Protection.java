package io.github.dschonas04.tntarsenal.nuclear.protection;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;
import io.github.dschonas04.tntarsenal.nuclear.radiation.Radiation;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.Map;

/**
 * Was zwischen einem Spieler und der Strahlung steht: Jodtabletten, RadAway, der
 * Strahlenschutzanzug und Blei.
 *
 * <ul>
 *   <li><b>Jodtabletten</b> geben zehn Minuten den Jod-Effekt, der halbiert, was der Körper
 *       aufnimmt.
 *   <li><b>RadAway</b> spült auf einmal 200 mSv aus, um den Preis einer kurzen Übelkeit.
 *   <li><b>Strahlenschutzanzug</b>: Jedes getragene Teil hält ein Fünftel ab, der ganze Anzug 90 %.
 *   <li><b>Bleiblöcke</b> über dem Kopf schirmen viermal so gut ab wie Stein.
 * </ul>
 */
public final class Protection {
    private Protection() {}

    public static final ResourceKey<EquipmentAsset> HAZMAT_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, NuclearTnt.id("hazmat"));
    public static final TagKey<Item> HAZMAT_REPAIR =
            TagKey.create(Registries.ITEM, NuclearTnt.id("repairs_hazmat_suit"));
    public static final ArmorMaterial HAZMAT =
            new ArmorMaterial(
                    12,
                    Map.of(
                            ArmorType.HELMET,
                            1,
                            ArmorType.CHESTPLATE,
                            3,
                            ArmorType.LEGGINGS,
                            2,
                            ArmorType.BOOTS,
                            1,
                            ArmorType.BODY,
                            3),
                    9,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    0f,
                    0f,
                    HAZMAT_REPAIR,
                    HAZMAT_ASSET);

    public static Holder<MobEffect> IODINE;
    public static Item IODINE_TABLETS;
    public static Item RAD_AWAY;
    public static Block LEAD_BLOCK;
    public static final Map<ArmorType, Item> SUIT = new EnumMap<>(ArmorType.class);

    private static final class Iodine extends MobEffect {
        Iodine() {
            super(MobEffectCategory.BENEFICIAL, 0xC9A0DC);
        }
    }

    /** RadAway: wie ein Trank getrunken, senkt die Dosis um 200 mSv. */
    private static final class RadAway extends Item {
        RadAway(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (entity instanceof ServerPlayer player) {
                Radiation.setDose(player, Math.max(0f, Radiation.dose(player) - 200f));
            }
            return super.finishUsingItem(stack, level, entity);
        }
    }

    public static void register() {
        IODINE =
                Registry.registerForHolder(
                        BuiltInRegistries.MOB_EFFECT, NuclearTnt.id("iodine"), new Iodine());
        FoodProperties nothing =
                new FoodProperties.Builder()
                        .nutrition(0)
                        .saturationModifier(0f)
                        .alwaysEdible()
                        .build();

        IODINE_TABLETS =
                NuclearTnt.item(
                        "iodine_tablets",
                        Item::new,
                        new Item.Properties()
                                .stacksTo(16)
                                .food(
                                        nothing,
                                        Consumables.defaultFood()
                                                .consumeSeconds(0.8f)
                                                .onConsume(
                                                        new ApplyStatusEffectsConsumeEffect(
                                                                new MobEffectInstance(
                                                                        IODINE, 12000, 0)))
                                                .build()));
        RAD_AWAY =
                NuclearTnt.item(
                        "rad_away",
                        RadAway::new,
                        new Item.Properties()
                                .stacksTo(8)
                                .food(
                                        nothing,
                                        Consumables.defaultDrink()
                                                .consumeSeconds(1.6f)
                                                .animation(ItemUseAnimation.DRINK)
                                                .build()));

        LEAD_BLOCK =
                NuclearTnt.block(
                        "lead_block",
                        Block::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                                .strength(6f, 1200f));

        for (ArmorType type :
                new ArmorType[] {
                    ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS
                }) {
            String name =
                    switch (type) {
                        case HELMET -> "hazmat_helmet";
                        case CHESTPLATE -> "hazmat_suit";
                        case LEGGINGS -> "hazmat_leggings";
                        default -> "hazmat_boots";
                    };
            SUIT.put(
                    type,
                    NuclearTnt.item(
                            name, Item::new, new Item.Properties().humanoidArmor(HAZMAT, type)));
        }
    }

    /** Wie viel der Strahlung von außen den Körper erreicht, von 1 (nichts davon) abwärts. */
    public static float factor(ServerPlayer player) {
        int pieces = 0;
        for (Map.Entry<ArmorType, Item> piece : SUIT.entrySet()) {
            EquipmentSlot slot = piece.getKey().getSlot();
            if (player.getItemBySlot(slot).is(piece.getValue())) pieces++;
        }
        float factor = pieces == 4 ? 0.1f : 1f - 0.2f * pieces;
        if (player.hasEffect(IODINE)) factor *= 0.5f;
        return factor;
    }
}
