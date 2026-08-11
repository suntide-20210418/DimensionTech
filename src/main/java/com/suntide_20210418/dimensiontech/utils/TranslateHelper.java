package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 翻译助手工具类 提供统一的翻译、格式化和本地化功能
 *
 * @author suntide_20210418
 * @version 1.0.0
 */
public final class TranslateHelper {

    private TranslateHelper() {
    }

    public static MutableComponent translate(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static String item(String name) {
        return namespaced("item", name);
    }

    public static String block(String name) {
        return namespaced("block", name);
    }

    public static String itemGroup(String name) {
        return namespaced("itemGroup", name);
    }

    public static String key(String name) {
        return namespaced("key", name);
    }

    public static String keyCategory(String name) {
        return namespaced("key.categories", name);
    }

    public static String container(String name) {
        return namespaced("container", name);
    }

    public static String entity(String name) {
        return namespaced("entity", name);
    }

    public static String message(String name) {
        return namespaced("message", name);
    }

    public static String tooltip(String name) {
        return namespaced("tooltip", name);
    }

    public static String namespaced(String namespace, String name) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(name, "name");
        return namespace + "." + modKey(name);
    }

    public static String modKey(String name) {
        Objects.requireNonNull(name, "name");
        return DimensionTechMod.MOD_ID + "." + normalize(name);
    }

    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Translation key name cannot be blank");
        }
        return normalized;
    }
}
