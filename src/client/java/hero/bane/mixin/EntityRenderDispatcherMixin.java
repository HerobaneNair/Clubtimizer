package hero.bane.mixin;

import hero.bane.Clubtimizer;
import hero.bane.action.AutoGG;
import hero.bane.auto.Requeue;
import hero.bane.auto.Spectator;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.FriendUtil;
import hero.bane.util.TextUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void club$hidePlayers(Entity entity, double x, double y, double z, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (Spectator.isFollowing(entity)
                && Clubtimizer.client.options.getPerspective().isFirstPerson()) {
            ci.cancel();
            return;
        }

        if (entity instanceof DisplayEntity.TextDisplayEntity textDisplayEntity
                && Clubtimizer.client.options.getPerspective().isFirstPerson()
                && Spectator.isFollowing(textDisplayEntity.getVehicle())) {
            ci.cancel();
            return;
        }

        if (!ClubtimizerConfig.getLobby().hidePlayers) return;
        if (!MCPVPStateChanger.inLobby()) return;

        if (entity instanceof PlayerEntity player) {
            if (player.isMainPlayer()) return;
            if (FriendUtil.isFriend(extractName(player.getName().getString()))) return;
            if (isNPC(player)) return;
            if (Requeue.isInsideCylinder(player)) return;
            if (!AutoGG.inSpawn()) return;

            ci.cancel();
            return;
        }

        if (!(entity instanceof DisplayEntity.TextDisplayEntity)) return;

        Entity vehicle = entity.getVehicle();

        if (vehicle instanceof PlayerEntity player) {
            if (player.isMainPlayer()) return;
            if (FriendUtil.isFriend(extractName(player.getName().getString()))) return;
            if (isNPC(player)) return;
            if (Requeue.isInsideCylinder(player)) return;
            if (!AutoGG.inSpawn()) return;

            ci.cancel();
        }
    }

    @Unique
    private static final double[][] NPC_COORDS = {
            {15.5, 105.5, -57.5},
            {-24.5, 106.0, -42.5},
            {-59.5, 104.9, -15.5}
    };

    @Unique
    private boolean isNPC(PlayerEntity p) {
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        for (double[] c : NPC_COORDS) {
            if (x == c[0] && y == c[1] && z == c[2]) return true;
        }
        return false;
    }

    @Unique
    private String extractName(String raw) {
        if (raw == null) return "";
        String stripped = TextUtil.stripFormatting(raw).strip();
        if (stripped.isEmpty()) return stripped;
        String[] parts = stripped.split("\\s+");
        return parts[parts.length - 1];
    }
}