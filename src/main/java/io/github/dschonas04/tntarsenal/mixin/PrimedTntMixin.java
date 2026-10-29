package io.github.dschonas04.tntarsenal.mixin;

import io.github.dschonas04.tntarsenal.ArsenalTntBlock;
import io.github.dschonas04.tntarsenal.TntArsenal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wenn die Lunte eines gezündeten TNT abgebrannt ist, prüfen, wessen Block es trägt. Unsere
 * detonieren auf ihre eigene Art; alles andere explodiert wie bisher.
 */
@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void tntArsenal$detonate(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        if (self.getBlockState().getBlock() instanceof ArsenalTntBlock block
                && self.level() instanceof ServerLevel level) {
            TntArsenal.burst(level, self, block.kind());
            block.kind().detonate(level, self);
            ci.cancel();
        }
    }
}
