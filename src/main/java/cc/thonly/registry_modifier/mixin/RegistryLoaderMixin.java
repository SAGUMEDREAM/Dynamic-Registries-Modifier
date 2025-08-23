package cc.thonly.registry_modifier.mixin;

import cc.thonly.registry_modifier.DynamicRegistriesModifier;
import cc.thonly.registry_modifier.api.DynamicRegistryManagerCallback;
import cc.thonly.registry_modifier.util.LoadingPhase;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.registry.*;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin(RegistryLoader.class)
public class RegistryLoaderMixin {
    @Unique
    private static final ThreadLocal<Boolean> IS_SERVER = ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "loadFromResource(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void loadFromResource(ResourceManager resourceManager, List<RegistryWrapper.Impl<?>> registries, List<RegistryLoader.Entry<?>> entries, CallbackInfoReturnable<DynamicRegistryManager.Immutable> cir) {
        if (entries.equals(RegistryLoader.DYNAMIC_REGISTRIES)) {
            DynamicRegistriesModifier.LOADING_PHASE.set(LoadingPhase.DYNAMIC_REGISTRIES);
        }
        if (entries.equals(RegistryLoader.DIMENSION_REGISTRIES)) {
            DynamicRegistriesModifier.LOADING_PHASE.set(LoadingPhase.DIMENSION_REGISTRIES);
        }
    }

    @WrapOperation(method = "loadFromResource(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/RegistryLoader;load(Lnet/minecraft/registry/RegistryLoader$RegistryLoadable;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;"))
    private static DynamicRegistryManager.Immutable wrapIsServerCall(@Coerce Object registryLoadable, List<RegistryWrapper.Impl<?>> baseRegistries, List<RegistryLoader.Entry<?>> entries, Operation<DynamicRegistryManager.Immutable> original) {
        try {
            IS_SERVER.set(true);
            return original.call(registryLoadable, baseRegistries, entries);
        } finally {
            IS_SERVER.set(false);
        }
    }

    @Inject(
            method = {"load(Lnet/minecraft/registry/RegistryLoader$RegistryLoadable;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;"},
            at = {@At(
                    value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
                    ordinal = 1
            )}
    )
    private static void load(@Coerce Object registryLoadable, List<RegistryWrapper.Impl<?>> baseRegistries, List<RegistryLoader.Entry<?>> entries, CallbackInfoReturnable<DynamicRegistryManager.Immutable> cir, @Local(ordinal = 2) List<RegistryLoader.Loader<?>> registriesList) {
        if (IS_SERVER.get()) {
//            System.out.println("加载周期: " + DynamicRegistriesModifier.LOADING_PHASE.get().toString());
            Iterator<RegistryLoader.Loader<?>> iterator = registriesList.iterator();
            while (iterator.hasNext()) {
                RegistryLoader.Loader<?> next = iterator.next();
                MutableRegistry<?> mutableRegistry = next.registry();
//                if (DynamicRegistriesModifier.LOADING_PHASE.get() == LoadingPhase.NONE) {
//                    continue;
//                }

                SimpleRegistry<?> registry = (SimpleRegistry<?>) mutableRegistry;

//                System.out.println("注册表ref键: " + registry.getKey());
//                for (Map.Entry<? extends RegistryKey<?>, ?> entry : mutableRegistry.getEntrySet()) {
//                    System.out.println(entry.getKey() + "-" + entry.getValue());
//                }
                DynamicRegistryManagerCallback.Table.load(registriesList);
//                if (registry.getKey().equals(RegistryKeys.DIMENSION) && DynamicRegistriesModifier.LOADING_PHASE.get() != LoadingPhase.DIMENSION_REGISTRIES) {
//                    continue;
//                }
                DynamicRegistryManagerCallback.start(registry);

            }
        }
    }
}
