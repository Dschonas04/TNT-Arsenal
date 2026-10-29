package io.github.dschonas04.tntarsenal.nuclear.mixin;

import io.github.dschonas04.tntarsenal.nuclear.client.GreenFog;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Färbt den Nebel grün und zieht ihn heran, wenn die Kamera in verstrahltem Land ist. */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void tntArsenal$greenFog(
            Camera camera,
            int renderDistance,
            DeltaTracker deltaTracker,
            float darkness,
            ClientLevel level,
            CallbackInfoReturnable<FogData> cir) {
        GreenFog.apply(cir.getReturnValue(), camera, level);
    }
}
