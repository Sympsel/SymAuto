package com.symauto.function.abstracts;

import com.symauto.function.utils.EntityGlowRegistry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.TeamColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public abstract class HighLightFunction extends SymAbstractFunction {
    @Getter
    @Setter
    private int scanRadius;
    @Getter
    @Setter
    private TeamColor color;

    protected HighLightFunction(String id, String name, boolean isConfigurable, int scanRadius, String tooltip) {
        super(id, name, isConfigurable, tooltip);
        this.scanRadius = scanRadius;
    }

    /**
     * 子类重写收集到的实体uuid，由此类统一高亮
     */
    @Nullable
    protected abstract Set<UUID> collectGlowIds(Minecraft client);

    @Override
    protected final void onTrigger(Minecraft client) {
        if (client.player == null || client.level == null) {
            EntityGlowRegistry.clear(getId());
            return;
        }
        Set<UUID> ids = collectGlowIds(client);
        EntityGlowRegistry.replace(getId(), glowColor(), ids == null ? Set.of() : ids);
    }

    protected abstract TeamColor glowColor();

    protected <T extends Entity> Set<UUID> scan(Minecraft client, Class<T> type, Predicate<T> match) {
        if (client.player == null || client.level == null) {
            return new HashSet<>();
        }
        AABB box = client.player.getBoundingBox().inflate(scanRadius);
       Set<UUID> ids = new HashSet<>();
        for (T entity : client.level.getEntitiesOfClass(type, box)) {
            if (entity.isAlive() && match.test(entity)) {
                ids.add(entity.getUUID());
            }
        }
        return ids;
    }


    @Override
    protected void onDisable() {
        EntityGlowRegistry.clear(getId());
    }
}
