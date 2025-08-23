package cc.thonly.registry_modifier.mixin;

import cc.thonly.registry_modifier.DynamicRegistriesModifier;
import cc.thonly.registry_modifier.api.DynamicRegistryManagerCallback;
import cc.thonly.registry_modifier.util.LoadingPhase;
import net.minecraft.server.SaveLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SaveLoading.class)
public class SaveLoadingMixin {
    @Inject(method = "load", at = @At("HEAD"))
    private static <D, R> void load(SaveLoading.ServerConfig serverConfig, SaveLoading.LoadContextSupplier<D> loadContextSupplier, SaveLoading.SaveApplierFactory<D, R> saveApplierFactory, Executor prepareExecutor, Executor applyExecutor, CallbackInfoReturnable<CompletableFuture<R>> cir) {
        DynamicRegistryManagerCallback.Table.CACHED2IMPL.clear();
        DynamicRegistriesModifier.LOADING_PHASE.set(LoadingPhase.NONE);
    }
}
