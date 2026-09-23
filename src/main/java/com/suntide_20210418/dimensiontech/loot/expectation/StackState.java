package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Complete final stack state. Count is stored exactly once here.
 *
 * <p>1.20.1 用一个往返 NBT（{@code ItemStack#serializeNBT()} / {@code ItemStack.of(CompoundTag)}）
 * 来承载这个规范状态。1.21 里 {@code ItemStack} 本身就是 {@code (Item, DATA_COMPONENT_TYPE 补丁, count)}
 * 三元组，而组件值可能引用 datapack 注册表（附魔是 {@code Holder<Enchantment>}）， NBT 编解码因此必须带上 {@code
 * HolderLookup.Provider}。既然身份判据本来就等价于 {@code Item + DataComponentPatch + count}（原版自己的 {@code
 * isSameItemSameComponents} 也是这么比的）， 这里直接持有这三个字段：构造与 {@link #stack()} 不再依赖注册表，只有需要落到字符串的 {@link
 * #serializedStackData(HolderLookup.Provider)} 才要求注册表上下文。
 */
public final class StackState {
    private final Item item;
    private final DataComponentPatch components;
    private final int count;
    private final int hashCode;

    public StackState(ItemStack stack) {
        ItemStack source = Objects.requireNonNull(stack, "stack");
        this.item = source.getItem();
        this.components = source.getComponentsPatch();
        this.count = source.getCount();
        this.hashCode = Objects.hash(this.item, this.components, this.count);
    }

    private StackState(Item item, DataComponentPatch components, int count) {
        this.item = item;
        this.components = components;
        this.count = count;
        this.hashCode = Objects.hash(item, components, count);
    }

    public ItemStack stack() {
        return new ItemStack(item.builtInRegistryHolder(), count, components);
    }

    public int count() {
        return count;
    }

    /**
     * Detached serialized payload. Mirrors the 1.20.1 shape: the nested vanilla NBT always carries
     * {@code count = 1} and the exact in-memory count lives in the outer key, so the payload never
     * depends on the stack count.
     */
    public String serializedStackData(HolderLookup.Provider registries) {
        ItemStack normalized = new ItemStack(item.builtInRegistryHolder(), 1, components);
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        // AIR cannot be encoded by ItemStack.CODEC (it uses ITEM_NON_AIR_CODEC); OPTIONAL_CODEC
        // maps
        // the empty stack to an empty tag, which ItemStack.parseOptional reads back as EMPTY.
        var codec = normalized.isEmpty() ? ItemStack.OPTIONAL_CODEC : ItemStack.CODEC;
        return codec.encodeStart(ops, normalized).getOrThrow().toString();
    }

    /** Changes only the explicit count while retaining the prior item's canonical state. */
    public StackState withCount(int count) {
        return new StackState(item, components, count);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof StackState other
                && count == other.count
                && item == other.item
                && components.equals(other.components);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return item + " x" + count + " " + components;
    }
}
