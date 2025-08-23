package cc.thonly.registry_modifier;

import cc.thonly.registry_modifier.util.LoadingPhase;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicRegistriesModifier implements ModInitializer {
	public static final String MOD_ID = "dynamic-registries-modifier";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ThreadLocal<LoadingPhase> LOADING_PHASE = ThreadLocal.withInitial(() -> LoadingPhase.NONE);
	@Override
	public void onInitialize() {
	}


}