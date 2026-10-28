package com.symauto.model.container;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于拼音搜索
 */
public final class PinyinIndex {
    private static final int MAX_VARIANTS = 32;

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    private record Index(Set<String> fulls, Set<String> initials) {
    }

    private static final Map<Item, Index> CACHE = new ConcurrentHashMap<>();
    private static volatile String cachedLanguage = "";

    public static boolean matches(Item item, String q) {
        if (q.isEmpty()) {
            return false;
        }
        for (int i = 0; i < q.length(); i++) {
            if (q.charAt(i) >= 0x80) {
                return false;
            }
        }
        String lang = Minecraft.getInstance().options.languageCode;
        if (!lang.equals(cachedLanguage)) {
            cachedLanguage = lang;
            CACHE.clear();
        }
        Index idx = CACHE.computeIfAbsent(item, PinyinIndex::build);
        for (String full : idx.fulls()) {
            if (full.contains(q)) {
                return true;
            }
        }
        for (String initial : idx.initials()) {
            if (initial.contains(q)) {
                return true;
            }
        }
        return false;
    }

    private static Index build(Item item) {
        String name = Component.translatable(item.getDescriptionId()).getString();
        Set<String> fulls = Set.of("");
        Set<String> initials = Set.of("");
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            String[] readings = readPinyin(c);
            if (readings == null || readings.length == 0) {
                if (c == ' ') {
                    continue;
                }
                readings = new String[]{String.valueOf(Character.toLowerCase(c))};
            }
            fulls = cap(expand(fulls, readings, false));
            initials = cap(expand(initials, readings, true));
        }
        return new Index(fulls, initials);
    }

    private static @Nullable String[] readPinyin(char c) {
        try {
            return PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            return null;
        }
    }

    private static Set<String> expand(Set<String> bases, String[] readings, boolean firstCharOnly) {
        Set<String> out = new HashSet<>();
        for (String base : bases) {
            for (String r : readings) {
                out.add(base + (firstCharOnly ? r.charAt(0) : r));
            }
        }
        return out;
    }

    private static Set<String> cap(Set<String> set) {
        if (set.size() <= MAX_VARIANTS) {
            return set;
        }
        return Set.copyOf(set.stream().limit(MAX_VARIANTS).toList());
    }
}