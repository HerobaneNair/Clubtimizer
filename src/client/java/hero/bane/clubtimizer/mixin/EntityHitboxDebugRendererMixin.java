package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.util.PlayerUtil;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    @Inject(
            method = "showHitboxes",
            at = @At("HEAD"),
            cancellable = true
    )
    private void club$hideHitboxes(Entity entity, float partialTicks, boolean serverEntity, CallbackInfo ci) {
        if (PlayerUtil.shouldHideEntity(entity)) {
            ci.cancel();
        }
    }
}
