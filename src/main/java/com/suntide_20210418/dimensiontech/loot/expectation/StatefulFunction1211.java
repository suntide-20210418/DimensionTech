package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Ordered LootItemFunction execution over one stack and one concrete Xoroshiro state. */
public final class StatefulFunction1211 {
    private StatefulFunction1211() {}

    public static Result applyAll(
            ItemStack input,
            JsonElement functions,
            LootAnalysisContext context,
            XoroshiroState1211 initialState,
            String functionsPointer) {
        return applyAll(input, functions, context, initialState, null, null, functionsPointer);
    }

    public static Result applyAll(
            ItemStack input,
            JsonElement functions,
            LootAnalysisContext context,
            XoroshiroState1211 initialState,
            StatefulCondition1211.ReferenceResolver conditionReferences,
            String functionsPointer) {
        return applyAll(
                input,
                functions,
                context,
                initialState,
                conditionReferences,
                null,
                functionsPointer);
    }

    public static Result applyAll(
            ItemStack input,
            JsonElement functions,
            LootAnalysisContext context,
            XoroshiroState1211 initialState,
            StatefulCondition1211.ReferenceResolver conditionReferences,
            FunctionReferenceResolver functionReferences,
            String functionsPointer) {
        try {
            return applyAllUnchecked(
                    input,
                    functions,
                    context,
                    initialState,
                    conditionReferences,
                    functionReferences,
                    functionsPointer);
        } catch (RuntimeException exception) {
            return Result.unsupported(
                    input,
                    initialState,
                    functionsPointer == null ? "" : functionsPointer,
                    malformedJsonMessage(exception));
        }
    }

    private static Result applyAllUnchecked(
            ItemStack input,
            JsonElement functions,
            LootAnalysisContext context,
            XoroshiroState1211 initialState,
            StatefulCondition1211.ReferenceResolver conditionReferences,
            FunctionReferenceResolver functionReferences,
            String functionsPointer) {
        if (functions == null) return Result.exact(input.copy(), initialState);
        if (!functions.isJsonArray()) {
            return Result.unsupported(
                    input, initialState, functionsPointer, "Function list is not an array");
        }
        ItemStack stack = input.copy();
        XoroshiroState1211 state = initialState;
        JsonArray array = functions.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            String pointer = functionsPointer + "/" + index;
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                return Result.unsupported(stack, state, pointer, "Function is not an object");
            }
            JsonObject function = element.getAsJsonObject();
            StatefulCondition1211.Result conditions;
            try {
                conditions =
                        StatefulCondition1211.testAll(
                                function.get("conditions"),
                                context,
                                state,
                                conditionReferences,
                                pointer + "/conditions");
            } catch (RuntimeException exception) {
                return Result.unsupported(
                        stack, state, pointer + "/conditions", malformedJsonMessage(exception));
            }
            if (conditions == null) {
                return Result.unsupported(
                        stack,
                        state,
                        pointer + "/conditions",
                        "Unsupported reachable function condition");
            }
            state = conditions.randomState();
            if (!conditions.value()) continue;

            String type = functionType(function);
            if (type.equals("minecraft:reference")) {
                ResourceLocation id = resourceLocationField(function, "name");
                if (id == null || functionReferences == null) {
                    return Result.unsupported(
                            stack, state, pointer + "/name", "Invalid function reference");
                }
                Result referenced = functionReferences.resolve(id, stack, state, pointer + "/name");
                if (referenced == null) {
                    return Result.unsupported(
                            stack, state, pointer, "Function resolver returned no result");
                }
                if (!referenced.supported()) return referenced;
                stack = referenced.stack();
                state = referenced.randomState();
                continue;
            }
            Step step = applyOne(stack, function, type, context, state, pointer);
            if (!step.supported()) {
                return Result.unsupported(
                        step.stack(), step.randomState(), step.pointer(), step.message());
            }
            stack = step.stack();
            state = step.randomState();
        }
        return Result.exact(stack, state);
    }

    private static Step applyOne(
            ItemStack input,
            JsonObject function,
            String type,
            LootAnalysisContext context,
            XoroshiroState1211 state,
            String pointer) {
        if (type.equals("minecraft:set_count")) {
            StatefulNumberProvider1211.IntResult count =
                    StatefulNumberProvider1211.getInt(function.get("count"), context, state);
            if (count == null) {
                return Step.unsupported(
                        input, state, pointer + "/count", "Unsupported count provider");
            }
            Boolean addValue = booleanField(function, "add", false);
            if (addValue == null) {
                return Step.unsupported(input, state, pointer + "/add", "Invalid add boolean");
            }
            int value = addValue ? input.getCount() + count.value() : count.value();
            // Do not copy a zero-count ItemStack: ItemStack.copy() turns it into EMPTY and would
            // erase the raw item needed by a later set_count in this same function pipeline.
            ItemStack output = input;
            output.setCount(Mth.clamp(value, 0, output.getMaxStackSize()));
            return Step.exact(output, count.randomState());
        }
        if (type.equals("minecraft:set_damage")) {
            if (!input.isDamageableItem()) return Step.exact(input.copy(), state);
            StatefulNumberProvider1211.FloatResult damage =
                    StatefulNumberProvider1211.getFloat(function.get("damage"), context, state);
            if (damage == null) {
                return Step.unsupported(
                        input, state, pointer + "/damage", "Unsupported damage provider");
            }
            Boolean addValue = booleanField(function, "add", false);
            if (addValue == null) {
                return Step.unsupported(input, state, pointer + "/add", "Invalid add boolean");
            }
            boolean add = addValue;
            float remaining =
                    add
                            ? 1.0F
                                    - (float) input.getDamageValue() / (float) input.getMaxDamage()
                                    + damage.value()
                            : damage.value();
            ItemStack output = input.copy();
            output.setDamageValue(
                    Mth.floor(
                            (1.0F - Mth.clamp(remaining, 0.0F, 1.0F))
                                    * (float) input.getMaxDamage()));
            return Step.exact(output, damage.randomState());
        }
        if (type.equals("minecraft:set_name")) {
            if (function.has("entity")) {
                return Step.unsupported(
                        input,
                        state,
                        pointer + "/entity",
                        "Context-resolved set_name components are not implemented");
            }
            Component component;
            try {
                JsonElement name = function.get("name");
                component =
                        name != null && name.isJsonPrimitive()
                                ? Component.literal(name.getAsString())
                                : ComponentSerialization.CODEC
                                        .parse(JsonOps.INSTANCE, name)
                                        .result()
                                        .orElse(null);
            } catch (RuntimeException exception) {
                return Step.unsupported(input, state, pointer + "/name", "Invalid name component");
            }
            if (component == null) {
                return Step.unsupported(input, state, pointer + "/name", "Null name component");
            }
            ItemStack output = input.copy();
            // 1.21 的 SetNameFunction 默认 target 是 custom_name，直接 set 到
            // DataComponents.CUSTOM_NAME（1.20.1 的 ItemStack#setHoverName 已删除）。
            output.set(DataComponents.CUSTOM_NAME, component);
            return Step.exact(output, state);
        }
        if (type.equals("minecraft:set_potion")) {
            ResourceLocation id = resourceLocationField(function, "id");
            Holder<Potion> potion =
                    id == null ? null : BuiltInRegistries.POTION.getHolder(id).orElse(null);
            if (potion == null) {
                return Step.unsupported(input, state, pointer + "/id", "Missing potion reference");
            }
            ItemStack output = input.copy();
            // 1.21 的 SetPotionFunction：PotionUtils 已删除，药水内容由 POTION_CONTENTS 组件承载。
            output.update(
                    DataComponents.POTION_CONTENTS,
                    PotionContents.EMPTY,
                    potion,
                    PotionContents::withPotion);
            return Step.exact(output, state);
        }
        if (type.equals("minecraft:enchant_randomly")) {
            List<Holder<Enchantment>> candidates =
                    ExactEnchantmentSemantics1211.randomCandidates(
                            function, input, context.registries());
            if (candidates == null) {
                return Step.unsupported(
                        input,
                        state,
                        pointer + "/options",
                        "Enchantment options need registry access or are malformed");
            }
            if (candidates.isEmpty()) return Step.exact(input.copy(), state);
            StatefulRandomSource1211 random = new StatefulRandomSource1211(state);
            Holder<Enchantment> enchantment = candidates.get(random.nextInt(candidates.size()));
            int level =
                    Mth.nextInt(
                            random,
                            enchantment.value().getMinLevel(),
                            enchantment.value().getMaxLevel());
            ItemStack output =
                    input.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : input.copy();
            // ItemStack#enchant 会按 EnchantmentHelper#getComponentType 选组件，附魔书写入
            // STORED_ENCHANTMENTS，等价于已删除的 EnchantedBookItem#addEnchantment。
            output.enchant(enchantment, level);
            return Step.exact(output, random.state());
        }
        if (type.equals("minecraft:enchant_with_levels")) {
            StatefulNumberProvider1211.IntResult levels =
                    StatefulNumberProvider1211.getInt(function.get("levels"), context, state);
            if (levels == null) {
                return Step.unsupported(
                        input, state, pointer + "/levels", "Unsupported levels provider");
            }
            // 1.21 的 EnchantWithLevelsFunction 不再有 treasure / 内置注册表，候选集来自
            // options（缺省为整个附魔注册表），选取过程仍是 EnchantmentHelper#enchantItem。
            List<Holder<Enchantment>> candidates =
                    ExactEnchantmentSemantics1211.enchantmentOptions(
                            function.get("options"), context.registries());
            if (candidates == null) {
                return Step.unsupported(
                        input,
                        state,
                        pointer + "/options",
                        "Enchantment options need registry access or are malformed");
            }
            StatefulRandomSource1211 random = new StatefulRandomSource1211(levels.randomState());
            ItemStack output =
                    EnchantmentHelper.enchantItem(
                            random, input.copy(), levels.value(), candidates.stream());
            return Step.exact(output, random.state());
        }
        if (type.equals("minecraft:set_stew_effect")) {
            if (!input.is(Items.SUSPICIOUS_STEW)) return Step.exact(input.copy(), state);
            JsonArray effects =
                    function.has("effects") && function.get("effects").isJsonArray()
                            ? function.getAsJsonArray("effects")
                            : null;
            if (effects == null) {
                return Step.unsupported(
                        input, state, pointer + "/effects", "Invalid stew effect list");
            }
            if (effects.isEmpty()) return Step.exact(input.copy(), state);
            var selected = state.nextInt(effects.size());
            JsonElement selectedElement = effects.get(selected.value());
            if (!selectedElement.isJsonObject()) {
                return Step.unsupported(
                        input,
                        selected.state(),
                        pointer + "/effects/" + selected.value(),
                        "Invalid stew effect entry");
            }
            JsonObject effectObject = selectedElement.getAsJsonObject();
            ResourceLocation effectId = resourceLocationField(effectObject, "type");
            Holder<MobEffect> effect =
                    effectId == null
                            ? null
                            : BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
            if (effect == null) {
                return Step.unsupported(
                        input,
                        selected.state(),
                        pointer + "/effects/" + selected.value() + "/type",
                        "Missing stew effect reference");
            }
            StatefulNumberProvider1211.IntResult duration =
                    StatefulNumberProvider1211.getInt(
                            effectObject.get("duration"), context, selected.state());
            if (duration == null) {
                return Step.unsupported(
                        input,
                        selected.state(),
                        pointer + "/effects/" + selected.value() + "/duration",
                        "Unsupported stew duration provider");
            }
            ItemStack output = input.copy();
            int ticks = effect.value().isInstantenous() ? duration.value() : duration.value() * 20;
            // 1.21 的 SetStewEffectFunction：SuspiciousStewItem 的 saveMobEffect 已删除，
            // 改写入 SUSPICIOUS_STEW_EFFECTS 组件。
            output.update(
                    DataComponents.SUSPICIOUS_STEW_EFFECTS,
                    SuspiciousStewEffects.EMPTY,
                    new SuspiciousStewEffects.Entry(effect, ticks),
                    SuspiciousStewEffects::withEffectAdded);
            return Step.exact(output, duration.randomState());
        }
        if (type.equals("minecraft:set_instrument")) {
            String option = stringField(function, "options");
            if (option == null) {
                return Step.unsupported(
                        input, state, pointer + "/options", "Instrument options is not a string");
            }
            if (!option.startsWith("#")) {
                return Step.unsupported(
                        input, state, pointer + "/options", "Instrument options is not a tag");
            }
            ResourceLocation tagId = ResourceLocation.tryParse(option.substring(1));
            if (tagId == null) {
                return Step.unsupported(
                        input, state, pointer + "/options", "Invalid instrument tag");
            }
            TagKey<Instrument> tag = TagKey.create(Registries.INSTRUMENT, tagId);
            java.util.Optional<? extends HolderSet<Instrument>> set =
                    BuiltInRegistries.INSTRUMENT.getTag(tag);
            if (set.isEmpty() || set.get().size() == 0) return Step.exact(input.copy(), state);
            // 1.21 的 InstrumentItem#setRandom 用 HolderSet#getRandomElement（即
            // Util#getRandomSafe → contents.get(random.nextInt(size))），与这里的抽取一致。
            var selected = state.nextInt(set.get().size());
            Holder<Instrument> holder = set.get().get(selected.value());
            ItemStack output = input.copy();
            // 1.21 用 DataComponents.INSTRUMENT（Holder<Instrument>）取代根 tag "instrument" 字符串。
            output.set(DataComponents.INSTRUMENT, holder);
            return Step.exact(output, selected.state());
        }
        if (type.equals("minecraft:exploration_map")) {
            if (!input.is(Items.MAP)) return Step.exact(input.copy(), state);
            if (!(context.level() instanceof ServerLevel level) || context.origin() == null) {
                return Step.unsupported(
                        input,
                        state,
                        pointer,
                        "Exploration map requires ServerLevel and origin context");
            }
            String destinationName =
                    function.has("destination")
                            ? stringField(function, "destination")
                            : "minecraft:on_treasure_maps";
            if (destinationName == null) {
                return Step.unsupported(
                        input, state, pointer + "/destination", "Invalid structure tag");
            }
            if (destinationName.startsWith("#")) destinationName = destinationName.substring(1);
            ResourceLocation destinationId = ResourceLocation.tryParse(destinationName);
            if (destinationId == null) {
                return Step.unsupported(
                        input, state, pointer + "/destination", "Invalid structure tag");
            }
            Holder<MapDecorationType> decoration =
                    function.has("decoration")
                            ? mapDecoration(function.get("decoration").getAsString())
                            : MapDecorationTypes.WOODLAND_MANSION;
            if (decoration == null) {
                return Step.unsupported(
                        input, state, pointer + "/decoration", "Invalid map decoration");
            }
            Integer zoomValue = integerField(function, "zoom", 2);
            Integer radiusValue = integerField(function, "search_radius", 50);
            Boolean skipKnownValue = booleanField(function, "skip_existing_chunks", true);
            if (zoomValue == null) {
                return Step.unsupported(input, state, pointer + "/zoom", "Invalid map zoom");
            }
            if (radiusValue == null) {
                return Step.unsupported(
                        input, state, pointer + "/search_radius", "Invalid search radius");
            }
            if (skipKnownValue == null) {
                return Step.unsupported(
                        input,
                        state,
                        pointer + "/skip_existing_chunks",
                        "Invalid skip_existing_chunks boolean");
            }
            int zoom = zoomValue;
            int radius = radiusValue;
            boolean skipKnown = skipKnownValue;
            TagKey<Structure> destination = TagKey.create(Registries.STRUCTURE, destinationId);
            BlockPos target =
                    level.findNearestMapStructure(
                            destination, BlockPos.containing(context.origin()), radius, skipKnown);
            if (target == null) return Step.exact(input.copy(), state);
            try {
                ItemStack output =
                        SavedDataTransaction1211.run(
                                level,
                                () -> {
                                    ItemStack map =
                                            MapItem.create(
                                                    level,
                                                    target.getX(),
                                                    target.getZ(),
                                                    (byte) zoom,
                                                    true,
                                                    true);
                                    MapItem.renderBiomePreviewMap(level, map);
                                    MapItemSavedData.addTargetDecoration(
                                            map, target, "+", decoration);
                                    return map.copy();
                                });
                return Step.exact(output, state);
            } catch (RuntimeException exception) {
                return Step.unsupported(
                        input,
                        state,
                        pointer,
                        "Exploration map SavedData transaction failed: "
                                + exception.getClass().getSimpleName());
            }
        }
        return Step.unsupported(input, state, pointer, "Unsupported reachable function " + type);
    }

    /**
     * 1.20.1 的 {@code "decoration"} 字段是 {@code MapDecoration.Type} 枚举名（无命名空间的字面量）； 1.21 改成注册表
     * {@code MapDecorationType} 的 id，默认值仍是 {@code minecraft:mansion} （见 {@code
     * ExplorationMapFunction#DEFAULT_DECORATION} 与 {@code MapDecorationTypes}）。
     */
    private static Holder<MapDecorationType> mapDecoration(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id == null ? null : BuiltInRegistries.MAP_DECORATION_TYPE.getHolder(id).orElse(null);
    }

    private static String functionType(JsonObject function) {
        if (!function.has("function")) return "";
        JsonElement value = function.get("function");
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : "";
    }

    private static String stringField(JsonObject object, String name) {
        if (!object.has(name)) return null;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            return null;
        try {
            return value.getAsString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static ResourceLocation resourceLocationField(JsonObject object, String name) {
        if (!object.has(name)) return null;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            return null;
        try {
            String text = value.getAsString();
            return text.isEmpty() ? null : ResourceLocation.tryParse(text);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Boolean booleanField(JsonObject object, String name, boolean defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean()
                : null;
    }

    private static Integer integerField(JsonObject object, String name, int defaultValue) {
        if (!object.has(name)) return defaultValue;
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            return null;
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String malformedJsonMessage(RuntimeException exception) {
        String detail = exception.getMessage();
        return "Malformed reachable function AST ("
                + exception.getClass().getSimpleName()
                + (detail == null || detail.isBlank() ? ")" : "): " + detail);
    }

    @FunctionalInterface
    public interface FunctionReferenceResolver {
        Result resolve(ResourceLocation id, ItemStack input, XoroshiroState1211 randomState);

        default Result resolve(
                ResourceLocation id,
                ItemStack input,
                XoroshiroState1211 randomState,
                String pointer) {
            return resolve(id, input, randomState);
        }
    }

    public record Result(
            boolean supported,
            ItemStack stack,
            XoroshiroState1211 randomState,
            String pointer,
            String message) {
        private static Result exact(ItemStack stack, XoroshiroState1211 state) {
            return new Result(true, stack, state, "", "");
        }

        private static Result unsupported(
                ItemStack stack, XoroshiroState1211 state, String pointer, String message) {
            return new Result(false, stack.copy(), state, pointer, message);
        }
    }

    private record Step(
            boolean supported,
            ItemStack stack,
            XoroshiroState1211 randomState,
            String pointer,
            String message) {
        private static Step exact(ItemStack stack, XoroshiroState1211 state) {
            return new Step(true, stack, state, "", "");
        }

        private static Step unsupported(
                ItemStack stack, XoroshiroState1211 state, String pointer, String message) {
            return new Step(false, stack.copy(), state, pointer, message);
        }
    }
}
