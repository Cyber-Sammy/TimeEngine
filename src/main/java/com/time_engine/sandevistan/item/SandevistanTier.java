package com.time_engine.sandevistan.item;

import com.time_engine.api.TemporalSessionOptions;
import net.minecraft.world.item.Rarity;

public enum SandevistanTier {
    MK1("mk1", 1, 160, 240, 0.50F, 24.0D, Rarity.UNCOMMON),
    MK2("mk2", 2, 220, 300, 0.35F, 32.0D, Rarity.RARE),
    MK3("mk3", 3, 300, 420, 0.20F, 40.0D, Rarity.EPIC);

    private final String id;
    private final int priority;
    private final TemporalSessionOptions options;
    private final Rarity rarity;

    SandevistanTier(
            String id,
            int priority,
            int durationTicks,
            int cooldownTicks,
            float timeScale,
            double radius,
            Rarity rarity) {
        this.id = id;
        this.priority = priority;
        this.options = new TemporalSessionOptions(durationTicks, cooldownTicks, timeScale, radius);
        this.rarity = rarity;
    }

    public String id() {
        return id;
    }

    public TemporalSessionOptions options() {
        return options;
    }

    public Rarity rarity() {
        return rarity;
    }

    public int priority() {
        return priority;
    }

    public String displayKey() {
        return "tier.time_engine.sandevistan." + id;
    }
}
