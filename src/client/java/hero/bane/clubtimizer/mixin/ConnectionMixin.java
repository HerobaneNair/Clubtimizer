package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void club$onAttack(Packet<?> packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!(packet instanceof ServerboundInteractPacket)) return;
        if (mc.player == null || !mc.player.isSpectator()) return;
        if (!MCPVPStateChanger.inSpec()) return;

        HitResult hr = mc.hitResult;
        if (hr == null || hr.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) hr).getEntity();
        if (!(target instanceof Player player)) return;

        Spectator.startFollowing(player);
    }
}
