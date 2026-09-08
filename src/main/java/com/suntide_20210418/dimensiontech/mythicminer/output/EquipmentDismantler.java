package com.suntide_20210418.dimensiontech.mythicminer.output;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;

/** Converts generated equipment into the materials used to make it. */
public final class EquipmentDismantler {
    private EquipmentDismantler() {}

    public static List<ItemStack> dismantle(ServerLevel level, ItemStack equipment) {
        Optional<ItemStack> primaryMaterial = primaryMaterialResult(equipment);
        if (primaryMaterial.isPresent()) {
            return List.of(primaryMaterial.get());
        }
        if (!isEquipmentOrWeapon(equipment)) {
            return List.of(equipment);
        }
        return craftingIngredients(level, equipment).orElseGet(() -> List.of(equipment));
    }

    public static Map<ResourceLocation, Double> dismantleExpectations(
            ServerLevel level, Map<ResourceLocation, Double> expectations) {
        Map<ResourceLocation, Double> dismantled = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Double> entry : expectations.entrySet()) {
            double expectedCount = entry.getValue();
            if (!Double.isFinite(expectedCount) || expectedCount <= 0.0D) {
                continue;
            }
            BuiltInRegistries.ITEM
                    .getOptional(entry.getKey())
                    .ifPresent(
                            item -> {
                                for (ItemStack output : dismantle(level, new ItemStack(item))) {
                                    mergeExpectation(
                                            dismantled,
                                            BuiltInRegistries.ITEM.getKey(output.getItem()),
                                            expectedCount,
                                            output.getCount());
                                }
                            });
        }
        return Map.copyOf(dismantled);
    }

    public static void mergeExpectation(
            Map<ResourceLocation, Double> expectations,
            ResourceLocation itemId,
            double sourceExpectation,
            int outputCount) {
        double outputExpectation = sourceExpectation * outputCount;
        if (itemId != null && Double.isFinite(outputExpectation) && outputExpectation > 0.0D) {
            expectations.merge(itemId, outputExpectation, Double::sum);
        }
    }

    public static Optional<ItemStack> primaryMaterialResult(ItemStack equipment) {
        Optional<ItemStack> specialMaterial = specialMaterialResult(equipment);
        if (specialMaterial.isPresent()) {
            return specialMaterial;
        }
        int materialCount = primaryMaterialCount(equipment);
        Ingredient repairIngredient = repairIngredient(equipment);
        ItemStack[] materials = repairIngredient.getItems();
        if (materialCount <= 0 || materials.length == 0) {
            return Optional.empty();
        }
        ItemStack result = materials[0].copy();
        result.setCount(materialCount * Math.max(1, equipment.getCount()));
        return Optional.of(result);
    }

    private static Optional<ItemStack> specialMaterialResult(ItemStack equipment) {
        int sourceCount = Math.max(1, equipment.getCount());
        if (equipment.getItem() instanceof ElytraItem) {
            return Optional.of(new ItemStack(Items.PHANTOM_MEMBRANE, 6 * sourceCount));
        }
        if (equipment.getItem() instanceof TridentItem) {
            return Optional.of(new ItemStack(Items.PRISMARINE_SHARD, 3 * sourceCount));
        }
        if (equipment.is(Items.DIAMOND_HORSE_ARMOR)) {
            return Optional.of(new ItemStack(Items.DIAMOND, 7 * sourceCount));
        }
        if (equipment.is(Items.GOLDEN_HORSE_ARMOR)) {
            return Optional.of(new ItemStack(Items.GOLD_INGOT, 7 * sourceCount));
        }
        if (equipment.is(Items.IRON_HORSE_ARMOR)) {
            return Optional.of(new ItemStack(Items.IRON_INGOT, 7 * sourceCount));
        }
        return Optional.empty();
    }

    private static int primaryMaterialCount(ItemStack equipment) {
        if (equipment.getItem() instanceof ArmorItem armor) {
            return switch (armor.getType()) {
                case HELMET -> materialCount(EquipmentPattern.HELMET);
                case CHESTPLATE -> materialCount(EquipmentPattern.CHESTPLATE);
                case LEGGINGS -> materialCount(EquipmentPattern.LEGGINGS);
                case BOOTS -> materialCount(EquipmentPattern.BOOTS);
            };
        }
        if (equipment.getItem() instanceof SwordItem) return materialCount(EquipmentPattern.SWORD);
        if (equipment.getItem() instanceof PickaxeItem)
            return materialCount(EquipmentPattern.PICKAXE);
        if (equipment.getItem() instanceof AxeItem) return materialCount(EquipmentPattern.AXE);
        if (equipment.getItem() instanceof ShovelItem)
            return materialCount(EquipmentPattern.SHOVEL);
        if (equipment.getItem() instanceof HoeItem) return materialCount(EquipmentPattern.HOE);
        return 0;
    }

    public static int materialCount(EquipmentPattern pattern) {
        return switch (pattern) {
            case HELMET -> 5;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 7;
            case BOOTS -> 4;
            case SWORD, HOE -> 2;
            case PICKAXE, AXE -> 3;
            case SHOVEL -> 1;
        };
    }

    private static Ingredient repairIngredient(ItemStack equipment) {
        if (equipment.getItem() instanceof ArmorItem armor) {
            return armor.getMaterial().getRepairIngredient();
        }
        if (equipment.getItem() instanceof TieredItem tieredItem) {
            return tieredItem.getTier().getRepairIngredient();
        }
        return Ingredient.EMPTY;
    }

    private static boolean isEquipmentOrWeapon(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof ProjectileWeaponItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof ShieldItem
                || stack.getItem() instanceof ElytraItem
                || stack.getItem() instanceof HorseArmorItem
                || stack.isDamageableItem();
    }

    private static Optional<List<ItemStack>> craftingIngredients(
            ServerLevel level, ItemStack equipment) {
        for (CraftingRecipe recipe :
                level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (!result.is(equipment.getItem())) {
                continue;
            }
            List<ItemStack> materials = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                ItemStack[] choices = ingredient.getItems();
                if (choices.length == 0) {
                    continue;
                }
                merge(materials, choices[0], Math.max(1, equipment.getCount()));
            }
            if (!materials.isEmpty()) {
                return Optional.of(List.copyOf(materials));
            }
        }
        return Optional.empty();
    }

    private static void merge(List<ItemStack> materials, ItemStack material, int count) {
        for (ItemStack existing : materials) {
            if (ItemStack.isSameItemSameTags(existing, material)) {
                existing.grow(count);
                return;
            }
        }
        ItemStack added = material.copy();
        added.setCount(count);
        materials.add(added);
    }

    public enum EquipmentPattern {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS,
        SWORD,
        PICKAXE,
        AXE,
        SHOVEL,
        HOE
    }
}
