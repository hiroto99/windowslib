package com.hiroto99.windowslib.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import javax.annotation.Nullable;

public abstract class EnergyBlockEntity extends BlockEntity {
    SimpleEnergyHandler ENERGY_INSTANCE;

    public EnergyBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
        assignEnergyStorage();
    }

    protected void CreateEnergy(int capacity, int insertExtract) {
        ENERGY_INSTANCE = new SimpleEnergyHandler(capacity, insertExtract, insertExtract);
    }

    protected void CreateEnergy(int capacity, int insertExtract, int energy) {
        ENERGY_INSTANCE = new SimpleEnergyHandler(capacity, insertExtract, insertExtract, energy);
    }

    protected void CreateEnergy(int capacity, int maxInsert, int maxExtract, int energy) {
        ENERGY_INSTANCE = new SimpleEnergyHandler(capacity, maxInsert, maxExtract, energy);
    }

    public abstract void assignEnergyStorage();

    public SimpleEnergyHandler getEnergyInstance() {
        return this.ENERGY_INSTANCE;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ENERGY_INSTANCE.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ENERGY_INSTANCE.deserialize(input);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
}
