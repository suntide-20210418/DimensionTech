package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.block.ModBlocks;
import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.integration.jade.MythicMinerJadeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import snownee.jade.api.BlockAccessor;

/** Regression coverage for Jade's server-data seam after miner responsibility migration. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MythicMinerJadeGameTests {
    private MythicMinerJadeGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void jadeProviderSerializesMinerRuntimeData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos position = helper.absolutePos(BlockPos.ZERO);
        level.setBlock(position, ModBlocks.TIER_1_MYTHIC_MINER.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(position) instanceof BaseMinerBlockEntity miner)) {
            helper.fail("Tier-one miner did not create a block entity");
            return;
        }
        miner.getEnergyStorage().receiveEnergy(4_096, false);
        CompoundTag data = new CompoundTag();
        MythicMinerJadeProvider.INSTANCE.appendServerData(
                data, new MinerAccessor(level, position, miner));

        if (!data.contains("Status")
                || data.getInt("SlotCount") != miner.getSlotCountForScript()
                || data.getInt("BaseCapacity") != miner.getBaseEnergyCapacity()
                || data.getInt("EnergyConsumption") != miner.getEffectiveEnergyConsumption()
                || !"item_handler".equals(data.getString("Output"))) {
            helper.fail("Jade provider did not serialize the current miner state: " + data);
            return;
        }
        helper.succeed();
    }

    private record MinerAccessor(ServerLevel level, BlockPos position, BaseMinerBlockEntity miner)
            implements BlockAccessor {
        @Override
        public Block getBlock() {
            return miner.getBlockState().getBlock();
        }

        @Override
        public BlockState getBlockState() {
            return miner.getBlockState();
        }

        @Override
        public BlockEntity getBlockEntity() {
            return miner;
        }

        @Override
        public BlockPos getPosition() {
            return position;
        }

        @Override
        public Direction getSide() {
            return Direction.UP;
        }

        @Override
        public boolean isFakeBlock() {
            return false;
        }

        @Override
        public ItemStack getFakeBlock() {
            return ItemStack.EMPTY;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public Player getPlayer() {
            return null;
        }

        @Override
        public CompoundTag getServerData() {
            return new CompoundTag();
        }

        @Override
        public BlockHitResult getHitResult() {
            return new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false);
        }

        @Override
        public boolean isServerConnected() {
            return true;
        }

        @Override
        public ItemStack getPickedResult() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean showDetails() {
            return false;
        }

        @Override
        public Object getTarget() {
            return miner;
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer) {}

        @Override
        public boolean verifyData(CompoundTag data) {
            return true;
        }
    }
}
