// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.item;

import io.github.dschonas04.tntarsenal.Bombs;
import io.github.dschonas04.tntarsenal.nuclear.Sounds;
import io.github.dschonas04.tntarsenal.nuclear.explosion.Jobs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Der Zeitzünder. Schleichend rechtsklicken stellt die Zeit ein (5 s, 10 s, 30 s, 1, 2 oder 5
 * Minuten), dann Rechtsklick auf eine Bombe (atomar, Arsenal oder einfaches TNT), um ihn dort
 * anzubringen: Der Countdown läuft über der Hotbar aller im Umkreis von 64 Blöcken und schwebt über
 * der Bombe, piept am Ende schneller und zündet die Bombe bei null. Der Zünder ist danach
 * verbraucht.
 */
public class TimerItem extends Item {
    private static final int[] STEPS = {5, 10, 30, 60, 120, 300};

    public TimerItem(Properties properties) {
        super(properties);
    }

    private static int seconds(ItemStack stack) {
        CompoundTag tag =
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getIntOr("seconds", STEPS[1]);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return super.use(level, player, hand);
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel) {
            int now = seconds(stack), next = STEPS[0];
            for (int i = 0; i < STEPS.length; i++)
                if (STEPS[i] == now) next = STEPS[(i + 1) % STEPS.length];
            CompoundTag tag = new CompoundTag();
            tag.putInt("seconds", next);
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
            player.sendOverlayMessage(Component.translatable("item.tnt_arsenal.timer.set", next));
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.PLAYERS,
                    0.5f,
                    1.4f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!Bombs.isBomb(level.getBlockState(pos))) return super.useOn(context);
        if (level instanceof ServerLevel server) {
            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            countdown(server, pos, seconds(stack) * 20, player);
            if (player != null) {
                player.sendOverlayMessage(
                        Component.translatable("item.tnt_arsenal.timer.started", seconds(stack)));
                if (!player.isCreative()) stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Führt einen Befehl still als Server in der Dimension der Bombe aus. */
    private static void command(ServerLevel level, String command) {
        var server = level.getServer();
        server.getCommands()
                .performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput().withLevel(level),
                        "execute in " + level.dimension().identifier() + " run " + command);
    }

    /** Die verbleibenden Sekunden, über der Bombe schwebend: groß, rot, immer dir zugewandt. */
    private static void label(
            ServerLevel level, BlockPos pos, String tag, int seconds, boolean create) {
        String text =
                "{text:\""
                        + (seconds >= 60
                                ? seconds / 60 + ":" + String.format("%02d", seconds % 60)
                                : String.valueOf(seconds))
                        + "\",color:\""
                        + (seconds <= 5 ? "red" : seconds <= 10 ? "gold" : "white")
                        + "\",bold:1b}";
        if (create) {
            command(
                    level,
                    String.format(
                            java.util.Locale.ROOT,
                            "summon text_display %.2f %.2f %.2f"
                                + " {Tags:[\"%s\"],billboard:\"center\",text:%s,"
                                + "transformation:{scale:[1.6f,1.6f,1.6f],translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}",
                            pos.getX() + 0.5,
                            pos.getY() + 1.35,
                            pos.getZ() + 0.5,
                            tag,
                            text));
        } else {
            command(
                    level,
                    "data merge entity @e[type=text_display,tag="
                            + tag
                            + ",limit=1] {text:"
                            + text
                            + "}");
        }
    }

    private static void countdown(ServerLevel level, BlockPos pos, int ticks, Player igniter) {
        String tag = "tnt_arsenal.timer." + pos.asLong();
        label(level, pos, tag, (ticks + 19) / 20, true);
        Jobs.add(
                new Jobs.Job() {
                    private int left = ticks;

                    @Override
                    public ServerLevel level() {
                        return level;
                    }

                    @Override
                    public boolean tick() {
                        if (!Bombs.isBomb(level.getBlockState(pos)) || left <= 0)
                            command(level, "kill @e[type=text_display,tag=" + tag + "]");
                        if (!Bombs.isBomb(level.getBlockState(pos))) {
                            if (igniter instanceof ServerPlayer player)
                                player.sendOverlayMessage(
                                        Component.translatable("item.tnt_arsenal.timer.gone"));
                            return true;
                        }
                        if (left <= 0) {
                            Bombs.ignite(level, pos, igniter, 1);
                            return true;
                        }
                        int every = left > 200 ? 20 : left > 60 ? 10 : 4;
                        if (left % every == 0) {
                            level.playSound(
                                    null,
                                    pos,
                                    Sounds.BEEP,
                                    SoundSource.BLOCKS,
                                    1.5f,
                                    left > 200 ? 0.8f : 1.2f);
                            level.sendParticles(
                                    ParticleTypes.SMOKE,
                                    pos.getX() + 0.5,
                                    pos.getY() + 1.1,
                                    pos.getZ() + 0.5,
                                    2,
                                    0.1,
                                    0.05,
                                    0.1,
                                    0);
                        }
                        if (left % 20 == 0) {
                            label(level, pos, tag, (left + 19) / 20, false);
                            Component text =
                                    Component.translatable(
                                                    "item.tnt_arsenal.timer.countdown",
                                                    (left + 19) / 20)
                                            .withStyle(ChatFormatting.RED);
                            for (ServerPlayer player : level.players()) {
                                if (player.distanceToSqr(Vec3.atCenterOf(pos)) < 64 * 64)
                                    player.sendOverlayMessage(text);
                            }
                        }
                        left--;
                        return false;
                    }
                });
    }
}
