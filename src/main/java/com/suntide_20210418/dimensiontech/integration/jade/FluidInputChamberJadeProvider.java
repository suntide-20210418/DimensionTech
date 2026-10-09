package com.suntide_20210418.dimensiontech.integration.jade;

import com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity;
import com.suntide_20210418.dimensiontech.block.entity.FluidInputChamberBlockEntity;
import com.suntide_20210418.dimensiontech.utils.ResourceLocationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/**
 * Hover readout for the fluid input chamber: what it holds, what the machine needs, and whether its
 * auto-pull is on.
 *
 * <p>Auto-pull lives on the chamber rather than the controller now, so this is the only place its
 * state is visible without shift-right-clicking the block and reading the chat line.
 */
public enum FluidInputChamberJadeProvider
        implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocationHelper.modLoc("fluid_input_chamber");
    private static final String BOUND = "Bound";
    private static final String AUTO_PULL = "AutoPull";
    private static final String FLUID_ID = "FluidId";
    private static final String FLUID_AMOUNT = "FluidAmount";
    private static final String FLUID_CAPACITY = "FluidCapacity";
    private static final String REQUIRED_FLUID_ID = "RequiredFluidId";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof FluidInputChamberBlockEntity chamber)) {
            return;
        }
        BaseMinerBlockEntity miner = chamber.miner();
        data.putBoolean(BOUND, miner != null);
        data.putBoolean(AUTO_PULL, chamber.isAutoPullEnabled());
        data.putString(FLUID_ID, fluidId(chamber.tank().getFluid().getFluid()));
        data.putInt(FLUID_AMOUNT, chamber.tank().getFluidAmount());
        data.putInt(FLUID_CAPACITY, chamber.tank().getCapacity());
        // The required fluid is the bound miner's, so it is absent rather than wrong when unbound.
        if (miner != null) {
            data.putString(REQUIRED_FLUID_ID, fluidId(miner.getRequiredFluid()));
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(BOUND, Tag.TAG_BYTE)) {
            return;
        }
        IThemeHelper theme = IThemeHelper.get();
        tooltip.add(JadeText.line("fluid", JadeText.fluidName(fluid(data.getString(FLUID_ID)))));
        tooltip.add(
                JadeText.line(
                        "fluid_amount",
                        Component.literal(
                                data.getInt(FLUID_AMOUNT)
                                        + " / "
                                        + data.getInt(FLUID_CAPACITY)
                                        + " mB")));
        if (data.getBoolean(BOUND)) {
            tooltip.add(
                    JadeText.line(
                            "required_fluid",
                            JadeText.fluidName(fluid(data.getString(REQUIRED_FLUID_ID)))));
        } else {
            tooltip.add(Component.translatable("jade.dimension_tech.unbound"));
        }
        boolean autoPull = data.getBoolean(AUTO_PULL);
        tooltip.add(
                JadeText.line(
                        "auto_pull",
                        autoPull
                                ? theme.success(Component.translatable("jade.dimension_tech.on"))
                                : theme.info(Component.translatable("jade.dimension_tech.off"))));
    }

    /** Fluids cross the wire as their registry id, since {@code Fluid} itself is not a tag type. */
    private static String fluidId(Fluid fluid) {
        return fluid == null ? "" : BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    private static Fluid fluid(String id) {
        if (id.isEmpty()) return Fluids.EMPTY;
        return BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(id)).orElse(Fluids.EMPTY);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
