package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ClientConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void club$interactionRemover(Packet<?> packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (MCPVPStateChanger.inGame() && packet instanceof ServerboundInteractPacket interactPacket) {
            interactPacket.dispatch(new ServerboundInteractPacket.Handler() {
                @Override
                public void onInteraction(@NonNull InteractionHand hand) {
                }

                @Override
                public void onInteraction(@NonNull InteractionHand hand, @NonNull Vec3 pos) {
                }

                @Override
                public void onAttack() {
                    HitResult hitResult = mc.hitResult;
                    if (hitResult == null) return;

                    if (hitResult.getType() == HitResult.Type.ENTITY) {
                        Entity entity = ((EntityHitResult) hitResult).getEntity();
                        if (entity instanceof Interaction) {
                            if (mc.player == null) return;
                            entity.remove(Entity.RemovalReason.KILLED);
                            entity.gameEvent(GameEvent.ENTITY_DIE);
                        }
                    }
                }
            });
        }
    }

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
