package hero.bane.mixin;

import hero.bane.state.MCPVPStateChanger;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void club$noProblemWindcharge(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (player == null) return;

        if (MCPVPStateChanger.inLobby()) {
            if (player.getOffHandStack().isOf(Items.WIND_CHARGE)) {
                boolean wasSneaking = player.isSneaking();
                player.setSneaking(true);
                ActionResult result = ((ClientPlayerInteractionManager) (Object) this)
                        .interactItem(player, Hand.OFF_HAND);
                player.setSneaking(wasSneaking);
                cir.setReturnValue(result);
            }
        }
    }
}
