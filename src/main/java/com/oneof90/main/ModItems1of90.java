// filepath: c:\Users\nikok\Desktop\Coding\NTM-1-of-90-Edition-GIT1\src\main\java\com\oneof90\main\ModItems1of90.java
package com.oneof90.main;

import com.hbm.main.MainRegistry;
import com.oneof90.items.ItemBulkLoader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;

/**
 * 1of90 Edition Item Registry
 *
 * Placeholder for registering items added by the 1of90 extension.
 */
public class ModItems1of90 {
	// The 1of90 creative tab is created from MainRegistry so it lands on the same
	// creative tab page as the HBM tabs (page 2). Do not instantiate it here.
	public static Item hbm_fluid_compat;
	public static Item bulk_loader;

	public static void init() {
		bulk_loader = new ItemBulkLoader()
			.setUnlocalizedName("bulk_loader")
			.setTextureName("oneof90:bulk_loader")
			.setCreativeTab(MainRegistry.oneof90Tab);
	}
	public static void mainRegistry() {
		GameRegistry.registerItem(bulk_loader, "bulk_loader");
	}
}
