package dev.zenhao.melon.mixin.render;

import melon.events.client.VerificationEvent;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferRenderer.class)
public class MixinBufferRenderer {
    @Inject(method = "drawWithGlobalProgram", at = @At("HEAD"))
    private static void drawWithGlobalProgram(BufferBuilder.BuiltBuffer buffer, CallbackInfo ci) {
        new VerificationEvent.DrawBuffer().post();
    }
}
