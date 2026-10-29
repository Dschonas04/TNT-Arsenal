// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.item;

import io.github.dschonas04.tntarsenal.Bombs;
import io.github.dschonas04.tntarsenal.Gimmicks;
import io.github.dschonas04.tntarsenal.nuclear.Sounds;
import io.github.dschonas04.tntarsenal.nuclear.explosion.Jobs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Der Fernzünder. Rechtsklick auf Bomben – atomare, jedes Arsenal-TNT oder einfaches TNT –
 * verknüpft bis zu sechzehn davon (Rechtsklick auf eine verknüpfte löst sie wieder); Rechtsklick
 * irgendwo sonst zündet sie alle nacheinander, solange ihre Chunks geladen sind und man in
 * derselben Dimension ist. Schleichend in die Luft rechtsklicken löst alle. Wer einen verknüpften
 * Zünder hält, sieht über jeder seiner Bomben eine rote Markierung.
 */
public class DetonatorItem extends Item {
    public static final int MAX = 16;
    private static final DustParticleOptions MARKER = new DustParticleOptions(0xFF2020, 1.2f);

    public DetonatorItem(Properties properties) {
        super(properties);
    }

    /** Die roten Markierungen über verknüpften Bomben, nur für den Spieler, der den Zünder hält. */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(
                server -> {
                    if (server.getTickCount() % 10 != 0) return;
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        for (InteractionHand hand : InteractionHand.values()) {
                            ItemStack stack = player.getItemInHand(hand);
                            if (!(stack.getItem() instanceof DetonatorItem)) continue;
                            ServerLevel level = player.level();
                            if (!dimension(stack).equals(level.dimension().identifier().toString()))
                                continue;
                            for (BlockPos pos : bombs(stack)) {
                                if (pos.distSqr(player.blockPosition()) > 96 * 96) continue;
                                level.sendParticles(
                                        player,
                                        MARKER,
                                        true,
                                        false,
                                        pos.getX() + 0.5,
                                        pos.getY() + 1.4,
                                        pos.getZ() + 0.5,
                                        3,
                                        0.05,
                                        0.15,
                                        0.05,
                                        0);
                            }
                        }
                    }
                });
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static String dimension(ItemStack stack) {
        return tag(stack).getStringOr("dimension", "");
    }

    /** Die verknüpften Bomben; Zünder von vor 1.7 hatten ein einzelnes „pos“. */
    public static List<BlockPos> bombs(ItemStack stack) {
        CompoundTag tag = tag(stack);
        List<BlockPos> out = new ArrayList<>();
        for (long p : tag.getLongArray("bombs").orElse(new long[0])) out.add(BlockPos.of(p));
        if (out.isEmpty() && tag.contains("pos")) out.add(BlockPos.of(tag.getLongOr("pos", 0L)));
        return out;
    }

    private static void save(ItemStack stack, List<BlockPos> bombs, String dimension) {
        if (bombs.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putLongArray("bombs", bombs.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putString("dimension", dimension);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!Bombs.isBomb(level.getBlockState(pos))) return super.useOn(context);
        Player player = context.getPlayer();
        if (level instanceof ServerLevel && player != null) {
            ItemStack stack = context.getItemInHand();
            String here = level.dimension().identifier().toString();
            List<BlockPos> bombs = here.equals(dimension(stack)) ? bombs(stack) : new ArrayList<>();
            if (bombs.remove(pos)) {
                player.sendOverlayMessage(
                        Component.translatable(
                                "item.tnt_arsenal.detonator.unlinked", bombs.size()));
                level.playSound(null, pos, Sounds.BEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
            } else if (bombs.size() >= MAX) {
                player.sendOverlayMessage(
                        Component.translatable("item.tnt_arsenal.detonator.full", MAX));
                return InteractionResult.FAIL;
            } else {
                bombs.add(pos);
                player.sendOverlayMessage(
                        Component.translatable(
                                "item.tnt_arsenal.detonator.linked",
                                bombs.size(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ()));
                level.playSound(
                        null,
                        pos,
                        Sounds.BEEP,
                        SoundSource.PLAYERS,
                        1.0f,
                        1.0f + bombs.size() * 0.05f);
            }
            save(stack, bombs, here);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        List<BlockPos> bombs = bombs(stack);
        if (bombs.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("item.tnt_arsenal.detonator.unbound"));
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            save(stack, List.of(), "");
            player.sendOverlayMessage(Component.translatable("item.tnt_arsenal.detonator.cleared"));
            return InteractionResult.SUCCESS;
        }
        if (!dimension(stack).equals(level.dimension().identifier().toString())) {
            player.sendOverlayMessage(
                    Component.translatable("item.tnt_arsenal.detonator.other_dimension"));
            return InteractionResult.FAIL;
        }
        // eine Salve: alle drei Ticks eine Bombe, in der Reihenfolge, in der sie verknüpft wurden
        List<BlockPos> salvo = new ArrayList<>(bombs);
        Jobs.add(
                new Jobs.Job() {
                    private int age;

                    @Override
                    public ServerLevel level() {
                        return server;
                    }

                    @Override
                    public boolean tick() {
                        if (age % 3 == 0 && age / 3 < salvo.size()) {
                            BlockPos pos = salvo.get(age / 3);
                            if (server.isLoaded(pos)) Bombs.ignite(server, pos, player, 0);
                        }
                        return ++age > salvo.size() * 3;
                    }
                });
        save(stack, List.of(), "");
        player.sendOverlayMessage(
                Component.translatable("item.tnt_arsenal.detonator.fired", salvo.size()));
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                Sounds.DETONATOR,
                SoundSource.PLAYERS,
                1.0f,
                1.0f);
        if (salvo.size() >= 8 && player instanceof ServerPlayer sp) Gimmicks.award(sp, "salvo");
        return InteractionResult.SUCCESS;
    }
}
