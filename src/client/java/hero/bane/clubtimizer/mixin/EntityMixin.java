package hero.bane.clubtimizer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.IconUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Component club$remapNametag(Component original) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return original;
        if (original == null) return null;
        return IconUtil.remapIcons(original);
    }
}
