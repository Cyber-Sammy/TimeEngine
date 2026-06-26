package com.time_engine.client;

import com.time_engine.common.network.TemporalConfigPayload;
import com.time_engine.common.network.TemporalConfigUpdateRequestPayload;
import com.time_engine.config.SnapshotHistoryGuidance;
import com.time_engine.config.TemporalConfigSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TemporalConfigScreen extends Screen {
    private static final int FIELD_WIDTH = 90;
    private static final int ROW_HEIGHT = 22;
    private static final int SUCCESS_COLOR = 0xFF55FF55;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int WARNING_COLOR = 0xFFFFAA00;
    private static final int INFO_COLOR = 0xFFAAAAAA;
    private static final double TIME_SCALE_STEP = 0.01D;

    private final List<FieldRow> fieldRows = new ArrayList<>();
    private TemporalConfigSnapshot current;
    private TemporalConfigSnapshot defaults;
    private String status;
    private int statusColor;

    private EditBox durationTicks;
    private EditBox cooldownTicks;
    private TimeScaleSlider timeScale;
    private EditBox radius;
    private EditBox snapshotHistoryTicks;
    private EditBox maxTrackedEntities;
    private EditBox ghostFrameIntervalTicks;
    private EditBox afterimageIntervalTicks;
    private EditBox afterimageLifetimeTicks;
    private EditBox afterimageObserverRadius;
    private EditBox maxTemporalBlocksPerSession;
    private EditBox maxInterceptCorrectionDistance;
    private EditBox phantomAttackReach;
    private EditBox phantomDamageMultiplier;
    private EditBox phantomAttackCooldownTicks;
    private EditBox phantomAllowedHitTickDrift;
    private Button snapshotPlayersAlways;
    private Button temporalInterceptEnabled;
    private Button diagnosticLogging;
    private Button useRecommendedHistory;
    private boolean snapshotPlayersAlwaysValue;
    private boolean temporalInterceptEnabledValue;
    private boolean diagnosticLoggingValue;
    private int footerY;

    private TemporalConfigScreen(TemporalConfigPayload payload) {
        super(Component.translatable("screen.time_engine.config.title"));
        current = payload.current();
        defaults = payload.defaults();
        status = payload.message();
        statusColor = payload.success() ? SUCCESS_COLOR : ERROR_COLOR;
    }

    public static void receive(TemporalConfigPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TemporalConfigScreen screen) {
            screen.updateFromServer(payload);
        } else {
            minecraft.setScreen(new TemporalConfigScreen(payload));
        }
    }

    @Override
    protected void init() {
        fieldRows.clear();
        int leftX = width / 2 - 280;
        int rightX = width / 2 + 10;
        int startY = 30;

        durationTicks = field("duration", current.durationTicks(), leftX, startY);
        cooldownTicks = field("cooldown", current.cooldownTicks(), leftX, startY + ROW_HEIGHT);
        timeScale = timeScaleSlider(current.timeScale(), leftX, startY + ROW_HEIGHT * 2);
        radius = field("radius", current.radius(), leftX, startY + ROW_HEIGHT * 3);
        snapshotHistoryTicks =
                field("history", current.snapshotHistoryTicks(), leftX, startY + ROW_HEIGHT * 4);
        useRecommendedHistory =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable(
                                                "screen.time_engine.config.use_recommended_history"),
                                        button -> useRecommendedHistory())
                                .bounds(leftX + 270, startY + ROW_HEIGHT * 4, 105, 20)
                                .build());
        maxTrackedEntities =
                field("max_entities", current.maxTrackedEntities(), leftX, startY + ROW_HEIGHT * 5);
        snapshotPlayersAlwaysValue = current.snapshotPlayersAlways();
        snapshotPlayersAlways =
                booleanButton(
                        "snapshot_players",
                        snapshotPlayersAlwaysValue,
                        leftX,
                        startY + ROW_HEIGHT * 6,
                        button -> {
                            snapshotPlayersAlwaysValue = !snapshotPlayersAlwaysValue;
                            updateBooleanButton(button, snapshotPlayersAlwaysValue);
                        });
        ghostFrameIntervalTicks =
                field(
                        "frame_interval",
                        current.ghostFrameIntervalTicks(),
                        leftX,
                        startY + ROW_HEIGHT * 7);
        afterimageIntervalTicks =
                field(
                        "afterimage_interval",
                        current.afterimageIntervalTicks(),
                        leftX,
                        startY + ROW_HEIGHT * 8);
        afterimageLifetimeTicks =
                field(
                        "afterimage_lifetime",
                        current.afterimageLifetimeTicks(),
                        leftX,
                        startY + ROW_HEIGHT * 9);

        afterimageObserverRadius =
                field("afterimage_radius", current.afterimageObserverRadius(), rightX, startY);
        temporalInterceptEnabledValue = current.temporalInterceptEnabled();
        temporalInterceptEnabled =
                booleanButton(
                        "intercept_enabled",
                        temporalInterceptEnabledValue,
                        rightX,
                        startY + ROW_HEIGHT,
                        button -> {
                            temporalInterceptEnabledValue = !temporalInterceptEnabledValue;
                            updateBooleanButton(button, temporalInterceptEnabledValue);
                        });
        maxTemporalBlocksPerSession =
                field(
                        "intercept_max_blocks",
                        current.maxTemporalBlocksPerSession(),
                        rightX,
                        startY + ROW_HEIGHT * 2);
        maxInterceptCorrectionDistance =
                field(
                        "intercept_max_distance",
                        current.maxInterceptCorrectionDistance(),
                        rightX,
                        startY + ROW_HEIGHT * 3);
        phantomAttackReach =
                field(
                        "attack_reach",
                        current.phantomAttackReach(),
                        rightX,
                        startY + ROW_HEIGHT * 4);
        phantomDamageMultiplier =
                field(
                        "damage_multiplier",
                        current.phantomDamageMultiplier(),
                        rightX,
                        startY + ROW_HEIGHT * 5);
        phantomAttackCooldownTicks =
                field(
                        "attack_cooldown",
                        current.phantomAttackCooldownTicks(),
                        rightX,
                        startY + ROW_HEIGHT * 6);
        phantomAllowedHitTickDrift =
                field(
                        "tick_drift",
                        current.phantomAllowedHitTickDrift(),
                        rightX,
                        startY + ROW_HEIGHT * 7);
        diagnosticLoggingValue = current.diagnosticLogging();
        diagnosticLogging =
                booleanButton(
                        "diagnostic_logging",
                        diagnosticLoggingValue,
                        rightX,
                        startY + ROW_HEIGHT * 8,
                        button -> {
                            diagnosticLoggingValue = !diagnosticLoggingValue;
                            updateBooleanButton(button, diagnosticLoggingValue);
                        });

        int buttonsY = startY + ROW_HEIGHT * 10;
        footerY = buttonsY + 28;
        addRenderableWidget(
                Button.builder(
                                Component.translatable("screen.time_engine.config.apply"),
                                button -> apply())
                        .bounds(width / 2 - 155, buttonsY, 100, 20)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("screen.time_engine.config.reset"),
                                button -> load(defaults))
                        .bounds(width / 2 - 50, buttonsY, 100, 20)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("screen.time_engine.config.cancel"),
                                button -> onClose())
                        .bounds(width / 2 + 55, buttonsY, 100, 20)
                        .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        fieldRows.forEach(
                row ->
                        graphics.drawString(
                                font,
                                Component.translatable("screen.time_engine.config." + row.key),
                                row.labelX,
                                row.y + 6,
                                0xFFFFFFFF,
                                false));
        graphics.drawCenteredString(font, status, width / 2, footerY, statusColor);
        HistoryGuidance guidance = historyGuidance();
        if (guidance.available()) {
            graphics.drawCenteredString(
                    font, guidance.message(), width / 2, footerY + 18, guidance.color());
        }
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.time_engine.config.notice"),
                width / 2,
                footerY + 36,
                INFO_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private EditBox field(String key, Number value, int x, int y) {
        EditBox box =
                new EditBox(
                        font,
                        x + 175,
                        y,
                        FIELD_WIDTH,
                        20,
                        Component.translatable("screen.time_engine.config." + key));
        box.setMaxLength(24);
        box.setValue(value.toString());
        fieldRows.add(new FieldRow(key, x, y));
        return addRenderableWidget(box);
    }

    private Button booleanButton(
            String key, boolean enabled, int x, int y, Button.OnPress onPress) {
        fieldRows.add(new FieldRow(key, x, y));
        Button button =
                Button.builder(booleanLabel(enabled), onPress)
                        .bounds(x + 175, y, FIELD_WIDTH, 20)
                        .build();
        button.setMessage(booleanLabel(enabled));
        return addRenderableWidget(button);
    }

    private void apply() {
        try {
            TemporalConfigSnapshot requested = readSnapshot();
            var validationError = requested.validate();
            if (validationError.isPresent()) {
                setStatus(validationError.orElseThrow(), false);
                return;
            }

            status = Component.translatable("screen.time_engine.config.saving").getString();
            statusColor = INFO_COLOR;
            PacketDistributor.sendToServer(new TemporalConfigUpdateRequestPayload(requested));
        } catch (NumberFormatException exception) {
            setStatus("Invalid number: " + exception.getMessage(), false);
        }
    }

    private void useRecommendedHistory() {
        try {
            snapshotHistoryTicks.setValue(Integer.toString(recommendedHistoryTicks()));
            setStatus(
                    Component.translatable("screen.time_engine.config.recommended_history_applied")
                            .getString(),
                    true);
        } catch (NumberFormatException exception) {
            setStatus("Invalid session values: " + exception.getMessage(), false);
        }
    }

    private TemporalConfigSnapshot readSnapshot() {
        return new TemporalConfigSnapshot(
                diagnosticLoggingValue,
                Integer.parseInt(durationTicks.getValue()),
                Integer.parseInt(cooldownTicks.getValue()),
                timeScale.scale(),
                Double.parseDouble(radius.getValue()),
                Integer.parseInt(snapshotHistoryTicks.getValue()),
                Integer.parseInt(maxTrackedEntities.getValue()),
                snapshotPlayersAlwaysValue,
                Integer.parseInt(ghostFrameIntervalTicks.getValue()),
                Integer.parseInt(afterimageIntervalTicks.getValue()),
                Integer.parseInt(afterimageLifetimeTicks.getValue()),
                Double.parseDouble(afterimageObserverRadius.getValue()),
                temporalInterceptEnabledValue,
                Integer.parseInt(maxTemporalBlocksPerSession.getValue()),
                Double.parseDouble(maxInterceptCorrectionDistance.getValue()),
                Double.parseDouble(phantomAttackReach.getValue()),
                Double.parseDouble(phantomDamageMultiplier.getValue()),
                Integer.parseInt(phantomAttackCooldownTicks.getValue()),
                Double.parseDouble(phantomAllowedHitTickDrift.getValue()));
    }

    private void load(TemporalConfigSnapshot snapshot) {
        durationTicks.setValue(Integer.toString(snapshot.durationTicks()));
        cooldownTicks.setValue(Integer.toString(snapshot.cooldownTicks()));
        timeScale.setScale(snapshot.timeScale());
        radius.setValue(Double.toString(snapshot.radius()));
        snapshotHistoryTicks.setValue(Integer.toString(snapshot.snapshotHistoryTicks()));
        maxTrackedEntities.setValue(Integer.toString(snapshot.maxTrackedEntities()));
        ghostFrameIntervalTicks.setValue(Integer.toString(snapshot.ghostFrameIntervalTicks()));
        afterimageIntervalTicks.setValue(Integer.toString(snapshot.afterimageIntervalTicks()));
        afterimageLifetimeTicks.setValue(Integer.toString(snapshot.afterimageLifetimeTicks()));
        afterimageObserverRadius.setValue(Double.toString(snapshot.afterimageObserverRadius()));
        maxTemporalBlocksPerSession.setValue(
                Integer.toString(snapshot.maxTemporalBlocksPerSession()));
        maxInterceptCorrectionDistance.setValue(
                Double.toString(snapshot.maxInterceptCorrectionDistance()));
        phantomAttackReach.setValue(Double.toString(snapshot.phantomAttackReach()));
        phantomDamageMultiplier.setValue(Double.toString(snapshot.phantomDamageMultiplier()));
        phantomAttackCooldownTicks.setValue(
                Integer.toString(snapshot.phantomAttackCooldownTicks()));
        phantomAllowedHitTickDrift.setValue(Double.toString(snapshot.phantomAllowedHitTickDrift()));
        snapshotPlayersAlwaysValue = snapshot.snapshotPlayersAlways();
        temporalInterceptEnabledValue = snapshot.temporalInterceptEnabled();
        diagnosticLoggingValue = snapshot.diagnosticLogging();
        updateBooleanButton(snapshotPlayersAlways, snapshotPlayersAlwaysValue);
        updateBooleanButton(temporalInterceptEnabled, temporalInterceptEnabledValue);
        updateBooleanButton(diagnosticLogging, diagnosticLoggingValue);
        status = Component.translatable("screen.time_engine.config.defaults_loaded").getString();
        statusColor = INFO_COLOR;
    }

    private void updateFromServer(TemporalConfigPayload payload) {
        current = payload.current();
        defaults = payload.defaults();
        load(current);
        setStatus(payload.message(), payload.success());
    }

    private void updateBooleanButton(Button button, boolean enabled) {
        button.setMessage(booleanLabel(enabled));
    }

    private HistoryGuidance historyGuidance() {
        try {
            int historyTicks = Integer.parseInt(snapshotHistoryTicks.getValue());
            int recommended = recommendedHistoryTicks();
            if (historyTicks < recommended) {
                return HistoryGuidance.warning(recommended);
            }
            return HistoryGuidance.ok(recommended);
        } catch (NumberFormatException exception) {
            return HistoryGuidance.unavailable();
        }
    }

    private int recommendedHistoryTicks() {
        return SnapshotHistoryGuidance.recommendedHistoryTicks(
                Integer.parseInt(durationTicks.getValue()), timeScale.scale());
    }

    private TimeScaleSlider timeScaleSlider(double value, int x, int y) {
        fieldRows.add(new FieldRow("time_scale", x, y));
        return addRenderableWidget(new TimeScaleSlider(x + 175, y, value));
    }

    private static Component booleanLabel(boolean enabled) {
        return Component.translatable(
                enabled
                        ? "screen.time_engine.config.enabled"
                        : "screen.time_engine.config.disabled");
    }

    private void setStatus(String message, boolean success) {
        status = message;
        statusColor = success ? SUCCESS_COLOR : ERROR_COLOR;
    }

    private record FieldRow(String key, int labelX, int y) {}

    private record HistoryGuidance(boolean available, Component message, int color) {
        private static HistoryGuidance warning(int recommended) {
            return new HistoryGuidance(
                    true,
                    Component.translatable(
                            "screen.time_engine.config.history_warning", recommended),
                    WARNING_COLOR);
        }

        private static HistoryGuidance ok(int recommended) {
            return new HistoryGuidance(
                    true,
                    Component.translatable("screen.time_engine.config.history_ok", recommended),
                    INFO_COLOR);
        }

        private static HistoryGuidance unavailable() {
            return new HistoryGuidance(false, Component.empty(), INFO_COLOR);
        }
    }

    private static final class TimeScaleSlider extends AbstractSliderButton {
        private double scale;

        private TimeScaleSlider(int x, int y, double scale) {
            super(x, y, FIELD_WIDTH, 20, Component.empty(), normalize(scale));
            setScale(scale);
        }

        private double scale() {
            return scale;
        }

        private void setScale(double requestedScale) {
            scale =
                    quantize(
                            Mth.clamp(
                                    requestedScale,
                                    TemporalConfigSnapshot.MIN_TIME_SCALE,
                                    TemporalConfigSnapshot.MAX_TIME_SCALE));
            value = normalize(scale);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format(Locale.ROOT, "%.2f", scale)));
        }

        @Override
        protected void applyValue() {
            scale = denormalize(value);
            updateMessage();
        }

        private static double normalize(double scale) {
            double clampedScale =
                    Mth.clamp(
                            scale,
                            TemporalConfigSnapshot.MIN_TIME_SCALE,
                            TemporalConfigSnapshot.MAX_TIME_SCALE);
            return (clampedScale - TemporalConfigSnapshot.MIN_TIME_SCALE)
                    / (TemporalConfigSnapshot.MAX_TIME_SCALE
                            - TemporalConfigSnapshot.MIN_TIME_SCALE);
        }

        private static double denormalize(double normalizedValue) {
            double rawScale =
                    TemporalConfigSnapshot.MIN_TIME_SCALE
                            + normalizedValue
                                    * (TemporalConfigSnapshot.MAX_TIME_SCALE
                                            - TemporalConfigSnapshot.MIN_TIME_SCALE);
            return quantize(rawScale);
        }

        private static double quantize(double rawScale) {
            double offset = rawScale - TemporalConfigSnapshot.MIN_TIME_SCALE;
            double steps = Math.round(offset / TIME_SCALE_STEP);
            double quantized = TemporalConfigSnapshot.MIN_TIME_SCALE + steps * TIME_SCALE_STEP;
            return Mth.clamp(
                    quantized,
                    TemporalConfigSnapshot.MIN_TIME_SCALE,
                    TemporalConfigSnapshot.MAX_TIME_SCALE);
        }
    }
}
