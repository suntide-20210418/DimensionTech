package com.suntide_20210418.dimensiontech.integration.kubejs;

import com.suntide_20210418.dimensiontech.integration.MinerIntegrationHooks;
import com.suntide_20210418.dimensiontech.utils.MinerScriptConfigService;
import com.suntide_20210418.dimensiontech.utils.StructureScriptConfigService;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.AttachedData;
import dev.latvian.mods.kubejs.util.ClassFilter;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

public final class DimensionTechKubeJSPlugin extends KubeJSPlugin {
    private static final EventGroup EVENTS = EventGroup.of("DimensionTechEvents");

    @Override
    public void registerEvents() {
        EVENTS.server("minerWork", () -> MinerEventsJS.class);
        EVENTS.server("minerCycle", () -> MinerEventsJS.class);
        EVENTS.server("minerOutput", () -> MinerEventsJS.class);
        EVENTS.register();
        MinerIntegrationHooks.install(
                new MinerIntegrationHooks.Hooks() {
                    @Override
                    public boolean postWork(
                            com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity
                                    miner) {
                        if (!EVENTS.getHandlers().get("minerWork").hasListeners()) return false;
                        var event =
                                new MinerEventsJS(
                                        new MinerBlockEntityJS(miner),
                                        (net.minecraft.server.level.ServerLevel) miner.getLevel(),
                                        -1,
                                        null,
                                        java.util.List.of());
                        return EVENTS.getHandlers()
                                .get("minerWork")
                                .post(ScriptType.SERVER, event)
                                .interruptFalse();
                    }

                    @Override
                    public boolean postCycle(
                            com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity
                                    miner,
                            net.minecraft.server.level.ServerLevel level,
                            int slot,
                            net.minecraft.resources.ResourceLocation marker,
                            int parallel) {
                        var handler = EVENTS.getHandlers().get("minerCycle");
                        if (!handler.hasListeners()) return false;
                        var event =
                                new MinerEventsJS(
                                        new MinerBlockEntityJS(miner),
                                        level,
                                        slot,
                                        marker,
                                        java.util.List.of());
                        event.parallel = parallel;
                        return handler.post(ScriptType.SERVER, event).interruptFalse();
                    }

                    @Override
                    public MinerIntegrationHooks.OutputResult postOutput(
                            com.suntide_20210418.dimensiontech.block.entity.BaseMinerBlockEntity
                                    miner,
                            net.minecraft.server.level.ServerLevel level,
                            java.util.List<ItemStack> outputs) {
                        var handler = EVENTS.getHandlers().get("minerOutput");
                        if (!handler.hasListeners())
                            return new MinerIntegrationHooks.OutputResult(false, outputs);
                        var event =
                                new MinerEventsJS(
                                        new MinerBlockEntityJS(miner),
                                        level,
                                        -1,
                                        null,
                                        new ArrayList<>(outputs));
                        boolean cancelled = handler.post(ScriptType.SERVER, event).interruptFalse();
                        var sanitized =
                                event.outputs.stream()
                                        .filter(stack -> stack != null && !stack.isEmpty())
                                        .map(ItemStack::copy)
                                        .toList();
                        return new MinerIntegrationHooks.OutputResult(cancelled, sanitized);
                    }
                });
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("DimensionTech", DimensionTechJS.class);
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        filter.allow(DimensionTechJS.class.getName());
        filter.allow(MinerBlockEntityJS.class.getName());
        filter.allow(MinerEventsJS.class.getName());
    }

    @Override
    public void onServerReload() {
        MinerScriptConfigService.clear();
        StructureScriptConfigService.clear();
    }

    @Override
    public void attachServerData(AttachedData<MinecraftServer> event) {
        event.add("dimensionTechMinerConfigs", MinerScriptConfigService.snapshot());
    }
}
