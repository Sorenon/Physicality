package net.sorenon.physicality;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalityMod implements ModInitializer {

	public static final String MODID = "physicality";
	public static final String MOD_NAME = "Physicality";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	public static PhysicalityMod INSTANCE;

	@Override
	public void onInitialize() {
		INSTANCE = this;
	}
}
