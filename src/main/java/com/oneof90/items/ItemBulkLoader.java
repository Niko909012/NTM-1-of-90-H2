package com.oneof90.items;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.items.IItemControlReceiver;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.util.ItemStackUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * Bulk Item Loader
 *
 * Middle-click (mouse wheel click) to open the GUI and load a single item stack into the tool.
 * Afterwards, sneak + right-click any inventory (chest, crate, barrel, machine, ...)
 * to fill every empty slot of that inventory with the loaded item.
 *
 * Sneaking is required because 1.7.10 opens a block's own GUI on a normal right click
 * before the held item ever gets a say; sneaking makes vanilla skip that activation.
 */
public class ItemBulkLoader extends Item implements IGUIProvider, IItemControlReceiver {

	/** Control key sent from the client when the GUI is requested with a middle mouse click. */
	public static final String KEY_OPEN = "open";

	public ItemBulkLoader() {
		this.setMaxStackSize(1);
	}

	/**
	 * Items never receive mouse input, so the client asks the server to open the GUI instead.
	 * The container only exists server-side, which is why the request has to travel there first.
	 */
	@Override
	public void receiveControl(EntityPlayer player, ItemStack stack, NBTTagCompound data) {
		if(data.getBoolean(KEY_OPEN)) player.openGui(MainRegistry.instance, 0, player.worldObj, 0, 0, 0);
	}

	/**
	 * Runs before the clicked block's own right-click handler, which is required because
	 * chests and machines consume the interaction in onBlockActivated.
	 */
	@Override
	public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fx, float fy, float fz) {
		ItemStack loaded = getLoadedItem(stack);

		// nothing loaded yet or not sneaking, let the click pass so the block itself can be used
		if(loaded == null || !player.isSneaking()) return false;

		IInventory inventory = getInventoryAt(world, x, y, z);

		// not an inventory, let the click pass to the block
		if(inventory == null) return false;

		// The client must return false here, otherwise the client returns before sending the
		// block-use packet and the server never gets to run the fill below.
		if(world.isRemote) return false;

		int filled = 0;

		for(int i = 0; i < inventory.getSizeInventory(); i++) {
			if(inventory.getStackInSlot(i) != null) continue;
			if(!inventory.isItemValidForSlot(i, loaded)) continue;

			ItemStack fill = loaded.copy();
			fill.stackSize = Math.min(fill.stackSize, Math.min(fill.getMaxStackSize(), inventory.getInventoryStackLimit()));
			if(fill.stackSize <= 0) continue;

			inventory.setInventorySlotContents(i, fill);
			filled++;
		}

		inventory.markDirty();
		if(inventory instanceof TileEntity) world.markBlockForUpdate(x, y, z);

		if(filled > 0) {
			player.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + String.format(StatCollector.translateToLocal("item.bulk_loader.filled"), filled)));
		} else {
			player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + StatCollector.translateToLocal("item.bulk_loader.full")));
		}

		return true;
	}

	/** Returns the inventory at the given position, or null if the block has none. */
	public static IInventory getInventoryAt(World world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if(tile instanceof IInventory) return (IInventory) tile;
		return null;
	}

	/** Returns the stack currently loaded into the tool, or null if nothing is loaded. */
	public static ItemStack getLoadedItem(ItemStack stack) {
		if(stack == null || !stack.hasTagCompound()) return null;

		ItemStack[] loaded = ItemStackUtil.readStacksFromNBT(stack, 1);
		return loaded != null ? loaded[0] : null;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		ItemStack loaded = getLoadedItem(stack);

		if(loaded != null) {
			list.add(EnumChatFormatting.GRAY + String.format(StatCollector.translateToLocal("item.bulk_loader.loaded"), loaded.getDisplayName()));
		} else {
			list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("item.bulk_loader.empty"));
		}

		list.add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("item.bulk_loader.usage"));
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerBulkLoader(player.inventory, new InventoryBulkLoader(player));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GuiBulkLoader(player.inventory, new InventoryBulkLoader(player));
	}

	/**
	 * Single-slot inventory that reads from and writes to the loader currently held in the
	 * player's hotbar. The hotbar slot is resolved on every access instead of caching the
	 * ItemStack, because the server can swap that stack instance while the GUI is open.
	 */
	public static class InventoryBulkLoader implements IInventory {

		/** Content is stored in the same "items" list that crates, boxes and bags use. */
		private final EntityPlayer player;
		private final int hotbarSlot;
		private final ItemStack fallback;
		private final ItemStack[] slots = new ItemStack[1];

		public InventoryBulkLoader(EntityPlayer player) {
			this.player = player;
			this.hotbarSlot = player.inventory.currentItem;
			this.fallback = player.inventory.getCurrentItem();

			ItemStack loaded = getLoadedItem(fallback);
			if(loaded != null) slots[0] = loaded;
		}

		/** The loader stack, preferring whatever currently sits in the tracked hotbar slot. */
		private ItemStack getTarget() {
			if(player.inventory.mainInventory != null && hotbarSlot >= 0 && hotbarSlot < player.inventory.mainInventory.length) {
				ItemStack current = player.inventory.mainInventory[hotbarSlot];
				if(current != null && current.getItem() instanceof ItemBulkLoader) return current;
			}
			return fallback;
		}

		/** Writes the loaded stack into the tool's NBT and pushes the change to the client. */
		private void writeToTarget() {
			ItemStack target = getTarget();
			if(target == null) return;

			if(slots[0] == null) {
				if(target.hasTagCompound()) {
					target.getTagCompound().removeTag("items");
					if(target.getTagCompound().hasNoTags()) target.setTagCompound(null);
				}
			} else {
				ItemStackUtil.addStacksToNBT(target, slots);
			}

			// the held item's NBT is not part of any open container, so it must be synced by hand
			if(player instanceof EntityPlayerMP) {
				((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
			}
		}

		@Override
		public int getSizeInventory() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return slots[slot];
		}

		@Override
		public ItemStack decrStackSize(int slot, int amount) {
			ItemStack stack = getStackInSlot(slot);

			if(stack != null) {
				if(stack.stackSize > amount) {
					stack = stack.splitStack(amount);
					markDirty();
				} else {
					slots[slot] = null;
					markDirty();
				}
			}

			return stack;
		}

		@Override
		public ItemStack getStackInSlotOnClosing(int slot) {
			return getStackInSlot(slot);
		}

		@Override
		public void setInventorySlotContents(int slot, ItemStack stack) {
			if(stack != null) {
				stack.stackSize = Math.min(stack.stackSize, this.getInventoryStackLimit());
			}

			slots[slot] = stack;
			markDirty();
		}

		@Override
		public void markDirty() {
			writeToTarget();
		}

		@Override
		public boolean isItemValidForSlot(int slot, ItemStack stack) {
			return stack == null || !(stack.getItem() instanceof ItemBulkLoader);
		}

		@Override
		public int getInventoryStackLimit() {
			return 64;
		}

		@Override
		public boolean isUseableByPlayer(EntityPlayer player) {
			return true;
		}

		@Override
		public void openInventory() { }

		@Override
		public void closeInventory() {
			writeToTarget();
		}

		@Override
		public String getInventoryName() {
			return "container.bulk_loader";
		}

		@Override
		public boolean hasCustomInventoryName() {
			return false;
		}
	}

	public static class ContainerBulkLoader extends Container {

		private final InventoryBulkLoader loader;

		public ContainerBulkLoader(InventoryPlayer invPlayer, InventoryBulkLoader loader) {
			this.loader = loader;

			this.addSlotToContainer(new Slot(loader, 0, 80, 35) {
				@Override public boolean isItemValid(ItemStack stack) {
					// the loader itself must not be loaded into its own slot
					return stack == null || !(stack.getItem() instanceof ItemBulkLoader);
				}
			});

			for(int i = 0; i < 3; i++) {
				for(int j = 0; j < 9; j++) {
					this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
				}
			}

			for(int i = 0; i < 9; i++) {
				this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142));
			}
		}

		@Override
		public boolean canInteractWith(EntityPlayer player) {
			return loader.isUseableByPlayer(player);
		}

		/**
		 * The loader is the item the GUI belongs to, so it must stay in the player's hand:
		 * otherwise its NBT (and anything loaded into it) would be written to a stack that
		 * is no longer held, silently losing the item.
		 */
		@Override
		public ItemStack slotClick(int index, int button, int mode, EntityPlayer player) {
			if(index == 28 + player.inventory.currentItem) return null;
			if(mode == 2 && button == player.inventory.currentItem) return null;

			return super.slotClick(index, button, mode, player);
		}

		@Override
		public ItemStack transferStackInSlot(EntityPlayer player, int index) {
			Slot slot = (Slot) this.inventorySlots.get(index);

			if(slot == null || !slot.getHasStack()) return null;

			ItemStack stack = slot.getStack();
			ItemStack original = stack.copy();

			if(index == 0) {
				if(!this.mergeItemStack(stack, 1, this.inventorySlots.size(), true)) return null;
			} else if(!this.mergeItemStack(stack, 0, 1, false)) {
				return null;
			}

			if(stack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}

			slot.onPickupFromSlot(player, stack);
			return original;
		}
	}

	@SideOnly(Side.CLIENT)
	public static class GuiBulkLoader extends GuiContainer {

		private static final int PANEL = 0xFFC6C6C6;
		private static final int OUTLINE = 0xFF555555;
		private static final int HIGHLIGHT = 0xFFFFFFFF;
		private static final int SLOT_DARK = 0xFF373737;
		private static final int SLOT_LIGHT = 0xFF8B8B8B;
		private static final int TEXT = 4210752;

		private final InventoryBulkLoader inventory;

		public GuiBulkLoader(InventoryPlayer invPlayer, InventoryBulkLoader inventory) {
			super(new ContainerBulkLoader(invPlayer, inventory));
			this.inventory = inventory;
			this.xSize = 176;
			this.ySize = 166;
		}

		@Override
		protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
			GL11.glColor4f(1F, 1F, 1F, 1F);

			drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, PANEL);
			drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, HIGHLIGHT);
			drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, HIGHLIGHT);
			drawRect(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, OUTLINE);
			drawRect(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, OUTLINE);

			// item slot
			drawRect(guiLeft + 79, guiTop + 34, guiLeft + 97, guiTop + 52, SLOT_DARK);
			drawRect(guiLeft + 80, guiTop + 35, guiLeft + 96, guiTop + 51, SLOT_LIGHT);
		}

		@Override
		protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
			String title = StatCollector.translateToLocal(this.inventory.getInventoryName());
			this.fontRendererObj.drawString(title, this.xSize / 2 - this.fontRendererObj.getStringWidth(title) / 2, 6, TEXT);

			String hint = StatCollector.translateToLocal("gui.bulk_loader.hint");
			this.fontRendererObj.drawString(hint, this.xSize / 2 - this.fontRendererObj.getStringWidth(hint) / 2, 57, TEXT);

			this.fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, TEXT);
		}
	}
}
