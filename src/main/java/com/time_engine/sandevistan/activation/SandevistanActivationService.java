package com.time_engine.sandevistan.activation;

import com.time_engine.api.TemporalActivationResult;
import com.time_engine.api.TemporalEngineApi;
import com.time_engine.sandevistan.item.SandevistanItem;
import com.time_engine.sandevistan.item.SandevistanTier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class SandevistanActivationService {
    private SandevistanActivationService() {}

    public static TemporalActivationResult toggle(ServerPlayer player, SandevistanTier tier) {
        if (TemporalEngineApi.sessionState(player).isPresent()) {
            return TemporalEngineApi.stop(player);
        }

        return TemporalEngineApi.activate(player, tier.options());
    }

    public static Optional<TemporalActivationResult> toggleBestAvailable(ServerPlayer player) {
        if (TemporalEngineApi.sessionState(player).isPresent()) {
            return Optional.of(TemporalEngineApi.stop(player));
        }

        Optional<SandevistanTier> tier = findBestAvailableTier(player);
        if (tier.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.time_engine.sandevistan.missing"), true);
            return Optional.empty();
        }

        return Optional.of(toggle(player, tier.get()));
    }

    public static Optional<SandevistanTier> findBestAvailableTier(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        return findBestTier(inventory.items, inventory.offhand, inventory.armor);
    }

    @SafeVarargs
    static Optional<SandevistanTier> findBestTier(Collection<ItemStack>... stackGroups) {
        return chooseBestTierFromGroups(extractTierGroups(stackGroups));
    }

    static Optional<SandevistanTier> chooseBestTierFromGroups(
            Collection<Collection<SandevistanTier>> tierGroups) {
        Optional<SandevistanTier> bestTier = Optional.empty();
        for (Collection<SandevistanTier> tierGroup : tierGroups) {
            Optional<SandevistanTier> groupTier = chooseBestTier(tierGroup);
            bestTier = chooseBetterTier(bestTier, groupTier);
        }
        return bestTier;
    }

    static Optional<SandevistanTier> chooseBestTier(Collection<SandevistanTier> tiers) {
        return tiers.stream().max(Comparator.comparingInt(SandevistanTier::priority));
    }

    private static Collection<SandevistanTier> extractTiers(Collection<ItemStack> stacks) {
        return stacks.stream()
                .map(SandevistanActivationService::tierFromStack)
                .flatMap(Optional::stream)
                .toList();
    }

    @SafeVarargs
    private static List<Collection<SandevistanTier>> extractTierGroups(
            Collection<ItemStack>... stackGroups) {
        List<Collection<SandevistanTier>> tierGroups = new ArrayList<>(stackGroups.length);
        for (Collection<ItemStack> stackGroup : stackGroups) {
            tierGroups.add(extractTiers(stackGroup));
        }
        return tierGroups;
    }

    private static Optional<SandevistanTier> tierFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        if (!(stack.getItem() instanceof SandevistanItem sandevistanItem)) {
            return Optional.empty();
        }

        return Optional.of(sandevistanItem.tier());
    }

    private static Optional<SandevistanTier> chooseBetterTier(
            Optional<SandevistanTier> current, Optional<SandevistanTier> candidate) {
        if (current.isEmpty()) {
            return candidate;
        }
        if (candidate.isEmpty()) {
            return current;
        }
        if (candidate.get().priority() > current.get().priority()) {
            return candidate;
        }
        return current;
    }
}
