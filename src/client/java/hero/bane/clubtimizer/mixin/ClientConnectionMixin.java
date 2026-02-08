package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void club$interactionRemover(Packet<?> packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (MCPVPStateChanger.inGame() && packet instanceof PlayerInteractEntityC2SPacket interactPacket) {
            interactPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override
                public void interact(Hand hand) {}

                @Override
                public void interactAt(Hand hand, Vec3d pos) {}

                @Override
                public void attack() {
                    HitResult hitResult = mc.crosshairTarget;
                    if (hitResult == null) return;

                    if (hitResult.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult entityHitResult = (EntityHitResult) hitResult;
                        Entity entity = entityHitResult.getEntity();
                        if (entity instanceof InteractionEntity) {
                            if (mc.player == null) return;
                            entity.discard();
                            entity.setRemoved(Entity.RemovalReason.KILLED);
                            entity.emitGameEvent(GameEvent.ENTITY_DIE);
                        }
                    }
                }
            });
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void club$onAttack(Packet<?> packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(packet instanceof PlayerInteractEntityC2SPacket)) return;
        if (mc.player == null || !mc.player.isSpectator()) return;
        if (!MCPVPStateChanger.inSpec()) return;

        HitResult hr = mc.crosshairTarget;
        if (hr == null || hr.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) hr).getEntity();
        if (!(target instanceof PlayerEntity player)) return;

        Spectator.startFollowing(player);
    }
}
