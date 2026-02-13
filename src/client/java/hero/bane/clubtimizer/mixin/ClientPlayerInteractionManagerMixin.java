package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void club$noProblemWindcharge(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null) return;

        if (MCPVPStateChanger.inLobby()) {
            if (player.getOffhandItem().is(Items.WIND_CHARGE) && player.getMainHandItem().isEmpty()) {
                boolean wasSneaking = player.isShiftKeyDown();
                player.setShiftKeyDown(true);
                InteractionResult result = ((MultiPlayerGameMode) (Object) this)
                        .useItem(player, InteractionHand.OFF_HAND);
                player.setShiftKeyDown(wasSneaking);
                cir.setReturnValue(result);
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void club$attackEntity(Player player, Entity target, CallbackInfo ci) {
        if (player == null) return;
        if (!player.isSpectator()) return;
        if (!MCPVPStateChanger.inSpec()) return;
        if (!(target instanceof Player targetPlayer)) return;

        Spectator.startFollowing(targetPlayer);
        ci.cancel();
    }
}