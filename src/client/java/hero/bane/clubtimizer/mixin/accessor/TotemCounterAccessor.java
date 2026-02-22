package hero.bane.clubtimizer.mixin.accessor;

import net.uku3lig.totemcounter.TotemCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(TotemCounter.class)
public interface TotemCounterAccessor {

    @Accessor("pops")
    static Map<UUID, Integer> getPops() {
        throw new AssertionError();
    }
}