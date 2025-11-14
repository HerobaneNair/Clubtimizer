package hero.bane.mixin;

import hero.bane.mixin.accessor.HandledScreenAccessor;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void club$antiBackgroundClicker(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;

        Slot hoveredSlot = accessor.invokeGetSlotAt(mouseX, mouseY);
        if (hoveredSlot == null) return;

        ItemStack stack = hoveredSlot.getStack();
        if (stack.isEmpty()) return;

        List<Text> tooltip = accessor.invokeGetTooltipFromItem(stack);
        if (tooltip == null || tooltip.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
