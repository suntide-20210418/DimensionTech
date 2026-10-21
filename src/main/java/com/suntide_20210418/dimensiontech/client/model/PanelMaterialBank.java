package com.suntide_20210418.dimensiontech.client.model;

import com.suntide_20210418.dimensiontech.DimensionTechMod;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed, immutable material bank rebuilt with each resource reload. No global
 * Minecraft/model-manager access from the baking worker thread.
 */
final class PanelMaterialBank {
    private static final String[] TYPES = {"energy", "efficiency", "luck", "parallel", "aggregate"};
    private final Map<String, List<BakedQuad>[]> faces = new HashMap<>();

    static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                DimensionTechMod.MOD_ID, "block/redwedge/" + name);
    }

    static List<String> names() {
        List<String> names = new ArrayList<>();
        for (String kind : List.of("casing", "glass", "structure")) {
            for (int mask : ConnectionMask.validMasks()) names.add(kind + "_" + mask);
        }
        for (String cap : List.of("cap_top", "cap_bottom"))
            for (int i = 0; i < 5; i++) names.add(cap + "_" + i);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) names.add("deck_" + x + "_" + z);
        for (int i = 0; i < 3; i++) names.add("spine_" + i);
        for (String face : List.of("front", "side", "top")) names.add("controller_" + face);
        for (String type : TYPES)
            for (int tier = 1; tier <= 6; tier++) names.add("module_" + type + "_" + tier);
        return List.copyOf(names);
    }

    @SuppressWarnings("unchecked")
    PanelMaterialBank(Map<ResourceLocation, BakedModel> registry) {
        RandomSource random = RandomSource.create(0);
        for (String name : names()) {
            BakedModel model = registry.get(id(name));
            if (model == null)
                throw new IllegalStateException("Missing RED WEDGE material model: " + name);
            List<BakedQuad>[] sides = new List[6];
            for (Direction face : Direction.values()) {
                sides[face.ordinal()] = List.copyOf(model.getQuads(null, face, random));
                if (sides[face.ordinal()].size() != 1)
                    throw new IllegalStateException(
                            "Expected one culled face: " + name + "/" + face);
            }
            faces.put(name, sides);
        }
    }

    List<BakedQuad> face(String name, Direction side) {
        List<BakedQuad>[] material = faces.get(name);
        if (material == null) throw new IllegalArgumentException("Unknown material: " + name);
        return material[side.ordinal()];
    }
}
