package com.oneof90.handler;

import org.lwjgl.input.Mouse;

import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTItemControlPacket;
import com.oneof90.items.ItemBulkLoader;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Opens the bulk loader GUI on a middle mouse click (mouse wheel click).
 *
 * The item itself cannot see mouse input and the GUI needs a server-side container, so the
 * click is turned into a control packet that tells the server to open the GUI.
 */
@SideOnly(Side.CLIENT)
public class BulkLoaderClickHandler {

	private static final int MIDDLE_BUTTON = 2;

	@SubscribeEvent
	public void onMouseInput(MouseInputEvent event) {
		if(Mouse.getEventButton() != MIDDLE_BUTTON || !Mouse.getEventButtonState()) return;

		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.thePlayer;

		// let the GUI have its own middle clicks, and leave pick block alone for every other item
		if(mc.currentScreen != null || player == null) return;

		ItemStack held = player.getHeldItem();
		if(held == null || !(held.getItem() instanceof ItemBulkLoader)) return;

		// Vanilla middle click picks the block under the cursor, which would swap the loader
		// (and the item loaded into it) out of the hotbar. Dropping the ray trace for this click
		// suppresses that; it is rebuilt by the renderer on the very next frame.
		mc.objectMouseOver = null;

		NBTTagCompound data = new NBTTagCompound();
		data.setBoolean(ItemBulkLoader.KEY_OPEN, true);
		PacketDispatcher.wrapper.sendToServer(new NBTItemControlPacket(data));
	}
}
