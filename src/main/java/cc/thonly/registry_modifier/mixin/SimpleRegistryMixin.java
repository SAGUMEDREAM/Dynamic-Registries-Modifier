package cc.thonly.registry_modifier.mixin;

import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Objects;

@Mixin(SimpleRegistry.class)
public abstract class SimpleRegistryMixin<T> implements MutableRegistry<T> {
    @Shadow @Final public Map<Identifier, RegistryEntry.Reference<T>> idToEntry;

    @Shadow @Final public Map<T, RegistryEntry.Reference<T>> valueToEntry;

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    public void add(RegistryKey<T> key, T value, RegistryEntryInfo info, CallbackInfoReturnable<RegistryEntry.Reference<T>> cir) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (this.idToEntry.containsKey(key.getValue())) {
            cir.setReturnValue(this.idToEntry.get(key.getValue()));
            cir.cancel();
        }
        if (this.valueToEntry.containsKey(value)) {
            cir.setReturnValue(this.idToEntry.get(key.getValue()));
            cir.cancel();
        }
    }
}
