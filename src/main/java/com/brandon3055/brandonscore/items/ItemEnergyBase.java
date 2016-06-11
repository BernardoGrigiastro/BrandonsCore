package com.brandon3055.brandonscore.items;

import cofh.api.energy.IEnergyContainerItem;
import com.brandon3055.brandonscore.utils.ItemNBTHelper;
import net.minecraft.item.ItemStack;

/**
 * Created by brandon3055 on 31/05/2016.
 * The base RF item for Brandon's Core
 */
public class ItemEnergyBase extends ItemBCore implements IEnergyContainerItem {

    private int capacity;
    private int receive;
    private int extract;

    public ItemEnergyBase(){}

    //region Energy

    /**
     * Set the Capacity, Receive and Extract stats for the item
     * */
    public void setEnergyStats(int capacity, int receive, int extract) {
        this.capacity = capacity;
        this.receive = receive;
        this.extract = extract;
    }

    /**
     * Returns the items RF capacity.
     * Overriding will override the capacity set by setEnergyStats
     * */
    public int getCapacity(ItemStack stack) {
        return capacity;
    }

    /**
     * Returns the items max receive.
     * Overriding will override the receive set by setEnergyStats
     * */
    public int getMaxReceive(ItemStack stack) {
        return receive;
    }

    /**
     * Returns the items max extract.
     * Overriding will override the extract set by setEnergyStats
     * */
    public int getMaxExtract(ItemStack stack) {
        return extract;
    }

    @Override
    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
        int energy = ItemNBTHelper.getInteger(container, "Energy", 0);
        int energyReceived = Math.min(getCapacity(container) - energy, Math.min(getMaxReceive(container), maxReceive));

        if (!simulate) {
            energy += energyReceived;
            ItemNBTHelper.setInteger(container, "Energy", energy);
        }

        return energyReceived;
    }

    @Override
    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
        int energy = ItemNBTHelper.getInteger(container, "Energy", 0);
        int energyExtracted = Math.min(energy, Math.min(getMaxExtract(container), maxExtract));

        if (!simulate) {
            energy -= energyExtracted;
            ItemNBTHelper.setInteger(container, "Energy", energy);
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored(ItemStack container) {
        return ItemNBTHelper.getInteger(container, "Energy", 0);
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return getCapacity(container);
    }

    //endregion

    //region Display

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return !(getEnergyStored(stack) == getMaxEnergyStored(stack));
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1D - ((double)getEnergyStored(stack) / (double)getMaxEnergyStored(stack));
    }

    //endregion
}