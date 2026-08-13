package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Serializes the server's loaded loot objects, including Forge load-event modifications. */
public final class RuntimeLootAstSource {
    private static final Gson TABLE_SERIALIZER = Deserializers.createLootTableSerializer().create();
    private static final Gson PREDICATE_SERIALIZER = Deserializers.createConditionSerializer().create();
    private static final Gson MODIFIER_SERIALIZER = Deserializers.createFunctionSerializer().create();

    private final LootDataManager lootData;
    private final ResourceManager resources;

    public RuntimeLootAstSource(MinecraftServer server) {
        this.lootData = server.getLootData();
        this.resources = server.getResourceManager();
    }

    public ResourceManager resources() {
        return resources;
    }

    public Optional<RuntimeAst<LootTable>> table(ResourceLocation id) {
        return lootData.getElementOptional(LootDataType.TABLE, id)
                .map(value -> new RuntimeAst<>(value, serializeObject(TABLE_SERIALIZER, value)));
    }

    public Optional<RuntimeAst<LootItemCondition>> predicate(ResourceLocation id) {
        return lootData.getElementOptional(LootDataType.PREDICATE, id)
                .map(value -> new RuntimeAst<>(value, PREDICATE_SERIALIZER.toJsonTree(value)));
    }

    public Optional<RuntimeAst<LootItemFunction>> modifier(ResourceLocation id) {
        return lootData.getElementOptional(LootDataType.MODIFIER, id)
                .map(value -> new RuntimeAst<>(value, MODIFIER_SERIALIZER.toJsonTree(value)));
    }

    private static JsonObject serializeObject(Gson serializer, Object value) {
        JsonElement element = serializer.toJsonTree(value);
        if (!element.isJsonObject()) {
            throw new IllegalStateException("Loot table serializer did not produce an object");
        }
        return element.getAsJsonObject();
    }

    public record RuntimeAst<T>(T identity, JsonElement json) {}
}
