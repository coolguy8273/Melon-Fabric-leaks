package dev.zenhao.melon.mixin.render;

import dev.zenhao.melon.Melon;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.class)
public class MixinRenderTickCounter {
    @Shadow
    public float lastFrameDuration;
    @Shadow
    public float tickDelta;
    @Shadow
    private long prevTimeMillis;
    @Shadow
    public float tickTime;

    @Inject(method = "beginRenderTick", at = @At("HEAD"), cancellable = true)
    private void beginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> ci) {
        this.lastFrameDuration = ((timeMillis - this.prevTimeMillis) / this.tickTime) * Melon.Companion.getTICK_TIMER();
        this.prevTimeMillis = timeMillis;
        this.tickDelta += this.lastFrameDuration;
        int i = (int) this.tickDelta;
        this.tickDelta -= i;
        ci.setReturnValue(i);
    }

}
