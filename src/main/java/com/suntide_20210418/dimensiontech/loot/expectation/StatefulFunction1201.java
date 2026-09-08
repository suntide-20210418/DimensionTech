package com.suntide_20210418.dimensiontech.loot.expectation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.registries.ForgeRegistries;

/** Ordered LootItemFunction execution over one stack and one concrete Xoroshiro state. */
public final class StatefulFunction1201 {
    private StatefulFunction1201() {}

    public static Result applyAll(
            ItemStack input,
            JsonElement functions,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            String functionsPointer) {
        return applyAll(input, functions, context, initialState, null, null, functionsPointer);
    }

    public static Result applyAll(
            ItemStack input,
            JsonElement functions,
            LootAnalysisContext context,
            XoroshiroState1201 initialState,
            StatefulCondition1201.ReferenceResolver conditionReferences,
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
            XoroshiroState1201 initialState,
            StatefulCondition1201.ReferenceResolver conditionReferences,
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
            XoroshiroState1201 initialState,
            StatefulCondition1201.ReferenceResolver conditionReferences,
            FunctionReferenceResolver functionReferences,
            String functionsPointer) {
        if (functions == null) return Result.exact(input.copy(), initialState);
        if (!functions.isJsonArray()) {
            return Result.unsupported(
                    input, initialState, functionsPointer, "Function list is not an array");
        }
        ItemStack stack = input.copy();
        XoroshiroState1201 state = initialState;
        JsonArray array = functions.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            String pointer = functionsPointer + "/" + index;
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                return Result.unsupported(stack, state, pointer, "Function is not an object");
            }
            JsonObject function = element.getAsJsonObject();
            StatefulCondition1201.Result conditions;
            try {
                conditions =
                        StatefulCondition1201.testAll(
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
            XoroshiroState1201 state,
            String pointer) {
        if (type.equals("minecraft:set_count")) {
            StatefulNumberProvider1201.IntResult count =
                    StatefulNumberProvider1201.getInt(function.get("count"), context, state);
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
            StatefulNumberProvider1201.FloatResult damage =
                    StatefulNumberProvider1201.getFloat(function.get("damage"), context, state);
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
                                : Component.Serializer.fromJson(name);
            } catch (RuntimeException exception) {
                return Step.unsupported(input, state, pointer + "/name", "Invalid name component");
            }
            if (component == null) {
                return Step.unsupported(input, state, pointer + "/name", "Null name component");
            }
            ItemStack output = input.copy();
            output.setHoverName(component);
            return Step.exact(output, state);
        }
        if (type.equals("minecraft:set_potion")) {
            ResourceLocation id = resourceLocationField(function, "id");
            Potion potion = id == null ? null : ForgeRegistries.POTIONS.getValue(id);
            if (potion == null) {
                return Step.unsupported(input, state, pointer + "/id", "Missing potion reference");
            }
            ItemStack output = input.copy();
            PotionUtils.setPotion(output, potion);
            return Step.exact(output, state);
        }
        if (type.equals("minecraft:enchant_randomly")) {
            List<Enchantment> candidates = enchantmentCandidates(function, input);
            if (candidates == null) {
                return Step.unsupported(
                        input, state, pointer + "/enchantments", "Invalid enchantment reference");
            }
            if (candidates.isEmpty()) return Step.exact(input.copy(), state);
            StatefulRandomSource1201 random = new StatefulRandomSource1201(state);
            Enchantment enchantment = candidates.get(random.nextInt(candidates.size()));
            int level = Mth.nextInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
            ItemStack output;
            if (input.is(Items.BOOK)) {
                output = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantedBookItem.addEnchantment(
                        output, new EnchantmentInstance(enchantment, level));
            } else {
                output = input.copy();
                output.enchant(enchantment, level);
            }
            return Step.exact(output, random.state());
        }
        if (type.equals("minecraft:enchant_with_levels")) {
            StatefulNumberProvider1201.IntResult levels =
                    StatefulNumberProvider1201.getInt(function.get("levels"), context, state);
            if (levels == null) {
                return Step.unsupported(
                        input, state, pointer + "/levels", "Unsupported levels provider");
            }
            StatefulRandomSource1201 random = new StatefulRandomSource1201(levels.randomState());
            Boolean treasureValue = booleanField(function, "treasure", false);
            if (treasureValue == null) {
                return Step.unsupported(
                        input, state, pointer + "/treasure", "Invalid treasure boolean");
            }
            boolean treasure = treasureValue;
            ItemStack output =
                    EnchantmentHelper.enchantItem(random, input.copy(), levels.value(), treasure);
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
            MobEffect effect =
                    effectId == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(effectId);
            if (effect == null) {
                return Step.unsupported(
                        input,
                        selected.state(),
                        pointer + "/effects/" + selected.value() + "/type",
                        "Missing stew effect reference");
            }
            StatefulNumberProvider1201.IntResult duration =
                    StatefulNumberProvider1201.getInt(
                            effectObject.get("duration"), context, selected.state());
            if (duration == null) {
                return Step.unsupported(
                        input,
                        selected.state(),
                        pointer + "/effects/" + selected.value() + "/duration",
                        "Unsupported stew duration provider");
            }
            ItemStack output = input.copy();
            int ticks = effect.isInstantenous() ? duration.value() : duration.value() * 20;
            SuspiciousStewItem.saveMobEffect(output, effect, ticks);
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
            var selected = state.nextInt(set.get().size());
            Holder<Instrument> holder = set.get().get(selected.value());
            java.util.Optional<net.minecraft.resources.ResourceKey<Instrument>> key =
                    holder.unwrapKey();
            if (key.isEmpty()) {
                return Step.unsupported(
                        input, selected.state(), pointer + "/options", "Unkeyed instrument holder");
            }
            ItemStack output = input.copy();
            output.getOrCreateTag().putString("instrument", key.get().location().toString());
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
            MapDecoration.Type decoration;
            try {
                decoration =
                        function.has("decoration")
                                ? MapDecoration.Type.valueOf(
                                        function.get("decoration")
                                                .getAsString()
                                                .toUpperCase(Locale.ROOT))
                                : MapDecoration.Type.MANSION;
            } catch (IllegalArgumentException exception) {
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
                        SavedDataTransaction1201.run(
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

    private static List<Enchantment> enchantmentCandidates(JsonObject function, ItemStack stack) {
        if (function.has("enchantments")) {
            JsonElement values = function.get("enchantments");
            if (values == null || !values.isJsonArray()) return null;
            // The 1.20.1 runtime treats an empty collection as the sentinel used by
            // randomApplicableEnchantment(), then discovers all currently registered, discoverable
            // enchantments applicable to this stack.  It is not a deterministic no-op.
            if (values.getAsJsonArray().isEmpty()) {
                return dynamicEnchantmentCandidates(stack);
            }
            ArrayList<Enchantment> result = new ArrayList<>();
            for (JsonElement value : values.getAsJsonArray()) {
                ResourceLocation id =
                        value.isJsonPrimitive()
                                ? ResourceLocation.tryParse(value.getAsString())
                                : null;
                Enchantment enchantment =
                        id == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(id);
                if (enchantment == null) return null;
                result.add(enchantment);
            }
            return List.copyOf(result);
        }
        return dynamicEnchantmentCandidates(stack);
    }

    private static List<Enchantment> dynamicEnchantmentCandidates(ItemStack stack) {
        boolean book = stack.is(Items.BOOK);
        return BuiltInRegistries.ENCHANTMENT.stream()
                .filter(Enchantment::isDiscoverable)
                .filter(enchantment -> book || enchantment.canEnchant(stack))
                .toList();
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
        Result resolve(ResourceLocation id, ItemStack input, XoroshiroState1201 randomState);

        default Result resolve(
                ResourceLocation id,
                ItemStack input,
                XoroshiroState1201 randomState,
                String pointer) {
            return resolve(id, input, randomState);
        }
    }

    public record Result(
            boolean supported,
            ItemStack stack,
            XoroshiroState1201 randomState,
            String pointer,
            String message) {
        private static Result exact(ItemStack stack, XoroshiroState1201 state) {
            return new Result(true, stack, state, "", "");
        }

        private static Result unsupported(
                ItemStack stack, XoroshiroState1201 state, String pointer, String message) {
            return new Result(false, stack.copy(), state, pointer, message);
        }
    }

    private record Step(
            boolean supported,
            ItemStack stack,
            XoroshiroState1201 randomState,
            String pointer,
            String message) {
        private static Step exact(ItemStack stack, XoroshiroState1201 state) {
            return new Step(true, stack, state, "", "");
        }

        private static Step unsupported(
                ItemStack stack, XoroshiroState1201 state, String pointer, String message) {
            return new Step(false, stack.copy(), state, pointer, message);
        }
    }
}
