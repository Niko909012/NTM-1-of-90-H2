package com.oneof90.tabs;

import com.oneof90.main.ModItems1of90;
import com.oneof90.main.ModBlocks1of90;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class taboneof90 extends CreativeTabs {

    public taboneof90(int p_i1853_1_, String p_i1853_2_) {
        super(p_i1853_1_, p_i1853_2_);
    }

    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(ModBlocks1of90.ntm_steel_beam);
    }
}
