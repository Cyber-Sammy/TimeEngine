package com.time_engine.engine.common.causal;

import java.util.Locale;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CausalLinkDiagnostics {
    private CausalLinkDiagnostics() {}

    public static String summary(CausalLinkView view) {
        return String.format(
                Locale.ROOT,
                "owner=%s, target=%s, state=%s, age=%d, sinceUpdate=%d, hardRemaining=%d, progress=%.3f",
                view.ownerId(),
                view.targetId(),
                view.state(),
                view.ageTicks(),
                view.ticksSinceUpdate(),
                view.hardLockTicksRemaining(),
                view.progress());
    }

    public static String detail(
            CausalLink link,
            int serverTick,
            String ownerName,
            String targetName,
            boolean targetExists,
            boolean targetAlive,
            boolean relationAllowed,
            boolean policyAllowed) {
        CausalLinkView view = CausalLinkView.from(link, serverTick);
        return String.format(
                Locale.ROOT,
                "Time Engine causal for %s -> %s: %s, selectedTick=%d, lastUpdatedTick=%d, targetExists=%s, targetAlive=%s, relationAllowed=%s, policyAllowed=%s, targetFrame[tick=%d, anchor=%s, bounds=%s, pose=%s], originTargetFrame[tick=%d, anchor=%s], ownerFrame[tick=%d, anchor=%s, look=%s, movement=%s, action=%s], originOwnerFrame[tick=%d, anchor=%s]",
                ownerName,
                targetName,
                summary(view),
                link.selectedTick(),
                link.lastUpdatedTick(),
                targetExists,
                targetAlive,
                relationAllowed,
                policyAllowed,
                link.latestFrame().serverTick(),
                vector(link.latestFrame().stableAnchor()),
                bounds(link.latestFrame().authoritativeBounds()),
                link.latestFrame().pose().pose(),
                link.originTargetFrame().serverTick(),
                vector(link.originTargetFrame().stableAnchor()),
                link.ownerFrame().serverTick(),
                vector(link.ownerFrame().stableAnchor()),
                vector(link.ownerFrame().pose().lookDirection()),
                vector(link.ownerFrame().pose().movementIntent()),
                action(link.ownerFrame().action()),
                link.originOwnerFrame().serverTick(),
                vector(link.originOwnerFrame().stableAnchor()));
    }

    private static String action(CausalPhantomActionState action) {
        return String.format(
                Locale.ROOT,
                "swinging=%s,attacking=%s,blocking=%s,usingItem=%s",
                action.swinging(),
                action.attacking(),
                action.blocking(),
                action.usingItem());
    }

    private static String vector(Vec3 vector) {
        return String.format(Locale.ROOT, "%.3f/%.3f/%.3f", vector.x, vector.y, vector.z);
    }

    private static String bounds(AABB bounds) {
        return String.format(
                Locale.ROOT,
                "%.3f/%.3f/%.3f -> %.3f/%.3f/%.3f",
                bounds.minX,
                bounds.minY,
                bounds.minZ,
                bounds.maxX,
                bounds.maxY,
                bounds.maxZ);
    }
}
