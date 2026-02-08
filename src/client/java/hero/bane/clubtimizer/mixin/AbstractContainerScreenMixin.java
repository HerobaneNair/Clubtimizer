package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.mixin.accessor.AbstractContainerScreenAccessor;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(
            method = "mouseClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void club$antiBackgroundClicker(
            MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir
    ) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return;

        AbstractContainerScreen<?> screen =
                (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerScreenAccessor accessor =
                (AbstractContainerScreenAccessor) screen;

        Slot hoveredSlot =
                accessor.invokeGetHoveredSlot(mouseButtonEvent.x(), mouseButtonEvent.y());
        if (hoveredSlot == null) return;

        ItemStack stack = hoveredSlot.getItem();
        if (stack.isEmpty()) return;

        List<Component> tooltip =
                accessor.invokeGetTooltipFromContainerItem(stack);
        if (tooltip == null || tooltip.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
