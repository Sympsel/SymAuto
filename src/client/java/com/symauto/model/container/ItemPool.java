package com.symauto.model.container;

import com.symauto.function.base.SymContainer;
import com.symauto.function.container.depend.ItemKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.world.item.ItemStack;

import java.util.*;

@Data
public class ItemPool {
    /**
     * 物品身份 -> 分布上下文
     */
    @Getter
    private final Map<ItemKey, ItemContext> pool;

    private ItemPool() {
        this.pool = new LinkedHashMap<>();
    }

    public static ItemPool create() {
        return new ItemPool();
    }

    public static ItemPool of(Collection<? extends SymContainer> containers) {
        return create().rebuild(containers);
    }

    public ItemPool rebuild(Collection<? extends SymContainer> containers) {
        pool.clear();
        for (SymContainer container : containers) {
            addContainer(container);
        }
        return this;
    }

    public ItemPool addContainer(SymContainer container) {
        if (container == null) {
            return this;
        }
        List<ItemStack> entries = container.entries(); // entries() 返回副本
        for (int index = 0; index < entries.size(); index++) {
            ItemStack entry = entries.get(index);
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            ItemKey key = ItemKey.of(entry);
            pool.computeIfAbsent(key, ItemContext::new)
                    .addSource(new Source(container, entry, index));
        }
        return this;
    }

    public ItemContext getContext(ItemKey key) {
        return pool.get(key);
    }

    public ItemContext getContext(ItemStack probe) {
        return pool.get(ItemKey.of(probe));
    }

    public int remove(ItemStack probe, int count) {
        if (probe == null || probe.isEmpty() || count <= 0) {
            return 0;
        }
        ItemKey key = ItemKey.of(probe);
        ItemContext ctx = pool.get(key);
        if (ctx == null) {
            return 0;
        }
        int removed = ctx.drain(count);
        if (ctx.isEmpty()) {
            pool.remove(key);
        }
        return removed;
    }

    public ItemPool removeContext(ItemKey key) {
        pool.remove(key);
        return this;
    }

    public int totalCount() {
        int n = 0;
        for (ItemContext ctx : pool.values()) {
            n += ctx.getCount();
        }
        return n;
    }

    public boolean isEmpty() {
        return pool.isEmpty();
    }

    public Collection<ItemContext> contexts() {
        return pool.values();
    }

    /**
     * 同种物品在容器池中的上下文
     */
    @AllArgsConstructor
    @Data
    public static class ItemContext {
        @Getter
        private final ItemKey key;
        public Map<SymContainer, ItemStack> stacks;
        @Getter
        private final List<Source> sources;

        private ItemContext(ItemKey key) {
            this.key = key;
            this.sources = new ArrayList<>();
        }

        void addSource(Source source) {
            sources.add(source);
        }

        public String getId() {
            return key.id();
        }

        public int getCount() {
            int n = 0;
            for (Source s : sources) {
                n += s.count();
            }
            return n;
        }

        public boolean isEmpty() {
            return getCount() == 0;
        }

        /**
         * 从中取走至多 {@code count} 个，顺带清理被清空的来源，返回实际取走数量
         */
        int drain(int count) {
            int remaining = count;
            Iterator<Source> it = sources.iterator();
            while (it.hasNext() && remaining > 0) {
                Source s = it.next();
                int have = s.count();
                if (have <= remaining) {
                    remaining -= have;
                    it.remove();
                } else {
                    s.shrink(remaining);
                    remaining = 0;
                }
            }
            return count - remaining;
        }

        /**
         * 该物品出现过的容器
         */
        public List<SymContainer> containers() {
            List<SymContainer> result = new ArrayList<>();
            for (Source s : sources) {
                if (!result.contains(s.container())) {
                    result.add(s.container());
                }
            }
            return result;
        }
    }

    /**
     * 某物品在某个容器某条目上的一份副本来源
     */
    public record Source(SymContainer container, ItemStack stack, int index) {
        public int count() {
            return stack.getCount();
        }

        public void shrink(int n) {
            stack.shrink(n);
        }
    }
}
