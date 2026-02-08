package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void club$noProblemWindcharge(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (player == null) return;

        if (MCPVPStateChanger.inLobby()) {
            if (player.getOffHandStack().isOf(Items.WIND_CHARGE) && player.getMainHandStack().isEmpty()) {
                boolean wasSneaking = player.isSneaking();
                player.setSneaking(true);
                ActionResult result = ((ClientPlayerInteractionManager) (Object) this)
                        .interactItem(player, Hand.OFF_HAND);
                player.setSneaking(wasSneaking);
                cir.setReturnValue(result);
            }
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void club$attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player == null) return;
        if (!player.isSpectator()) return;
        if (!MCPVPStateChanger.inSpec()) return;
        if (!(target instanceof PlayerEntity targetPlayer)) return;

        Spectator.startFollowing(targetPlayer);
        ci.cancel();
    }
}
