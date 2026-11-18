package com.oneof90.blocks.generic;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class Reinforcement extends Block {

	// Constructor for the invisible block
	public Reinforcement(Material material) {
		super(material);
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1F, 1F, 1F);
		this.setResistance(2000.0F);
	}

	@Override
	public boolean renderAsNormalBlock() {
		return true;
	}

	// cannot be placed manually
	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return true;
	}
}
