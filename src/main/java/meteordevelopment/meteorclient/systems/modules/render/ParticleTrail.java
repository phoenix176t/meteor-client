/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

public class ParticleTrail extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    // General settings
    private final Setting<ParticleMode> particleMode = sgGeneral.add(new EnumSetting.Builder<ParticleMode>()
        .name("particle-mode")
        .description("The type of particle to use for the trail.")
        .defaultValue(ParticleMode.Portal)
        .build()
    );

    private final Setting<TrailShape> shape = sgGeneral.add(new EnumSetting.Builder<TrailShape>()
        .name("shape")
        .description("The shape of the particle trail.")
        .defaultValue(TrailShape.Line)
        .build()
    );

    private final Setting<Integer> density = sgGeneral.add(new IntSetting.Builder()
        .name("density")
        .description("Number of particles to spawn per tick.")
        .defaultValue(3)
        .range(1, 20)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> spread = sgGeneral.add(new DoubleSetting.Builder()
        .name("spread")
        .description("How much the particles spread out.")
        .defaultValue(0.3)
        .range(0.0, 2.0)
        .sliderMax(2.0)
        .build()
    );

    private final Setting<Boolean> speedBased = sgGeneral.add(new BoolSetting.Builder()
        .name("speed-based")
        .description("Spawn more particles when moving faster.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only spawn particles when on the ground.")
        .defaultValue(false)
        .build()
    );

    // Visual settings
    private final Setting<Boolean> elytraBoost = sgVisual.add(new BoolSetting.Builder()
        .name("elytra-boost")
        .description("Special rocket-like effect when flying with elytra.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> height = sgVisual.add(new DoubleSetting.Builder()
        .name("height")
        .description("Height offset for the particle spawn position.")
        .defaultValue(0.0)
        .range(-2.0, 2.0)
        .sliderMax(2.0)
        .build()
    );

    private int tickCounter = 0;

    public ParticleTrail() {
        super(Categories.Render, "particle-trail", "Leaves a beautiful particle trail behind you as you move.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // Check if only on ground
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;

        // Calculate player velocity
        double velocityX = mc.player.getX() - mc.player.prevX;
        double velocityY = mc.player.getY() - mc.player.prevY;
        double velocityZ = mc.player.getZ() - mc.player.prevZ;
        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);

        // Calculate particle count based on settings
        int particleCount = density.get();
        if (speedBased.get()) {
            particleCount = (int) Math.max(1, particleCount * (1 + speed * 2));
            particleCount = Math.min(particleCount, 20); // Cap at 20
        }

        // Check if flying with elytra
        boolean isFlying = mc.player.isFallFlying() && elytraBoost.get();
        if (isFlying) {
            particleCount *= 2; // Double particles for elytra
        }

        // Spawn particles
        for (int i = 0; i < particleCount; i++) {
            spawnParticle(isFlying);
        }

        tickCounter++;
    }

    private void spawnParticle(boolean isFlying) {
        // Get base position
        double x = mc.player.getX();
        double y = mc.player.getY() + height.get();
        double z = mc.player.getZ();

        // Calculate position based on shape
        double offsetX = 0;
        double offsetY = 0;
        double offsetZ = 0;

        switch (shape.get()) {
            case Spiral -> {
                double angle = tickCounter * 0.3;
                double radius = 0.5;
                offsetX = Math.cos(angle) * radius;
                offsetZ = Math.sin(angle) * radius;
                offsetY = (Math.random() - 0.5) * spread.get();
            }
            case Wave -> {
                double wave = Math.sin(tickCounter * 0.2) * 0.5;
                offsetX = (Math.random() - 0.5) * spread.get();
                offsetY = wave;
                offsetZ = (Math.random() - 0.5) * spread.get();
            }
            case Random -> {
                offsetX = (Math.random() - 0.5) * spread.get() * 2;
                offsetY = (Math.random() - 0.5) * spread.get() * 2;
                offsetZ = (Math.random() - 0.5) * spread.get() * 2;
            }
            case Circle -> {
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * spread.get();
                offsetX = Math.cos(angle) * radius;
                offsetZ = Math.sin(angle) * radius;
                offsetY = (Math.random() - 0.5) * spread.get() * 0.5;
            }
            case Line -> {
                offsetX = (Math.random() - 0.5) * spread.get();
                offsetY = (Math.random() - 0.5) * spread.get();
                offsetZ = (Math.random() - 0.5) * spread.get();
            }
        }

        // Apply offsets
        x += offsetX;
        y += offsetY;
        z += offsetZ;

        // Get particle type
        ParticleType<?> particleType = getParticleType(isFlying);

        // Spawn particle with slight velocity for effect
        double velX = (Math.random() - 0.5) * 0.02;
        double velY = (Math.random() - 0.5) * 0.02;
        double velZ = (Math.random() - 0.5) * 0.02;

        mc.world.addParticle((ParticleEffect) particleType, x, y, z, velX, velY, velZ);
    }

    private ParticleType<?> getParticleType(boolean isFlying) {
        // Use flame for elytra boost
        if (isFlying) {
            return ParticleTypes.FLAME;
        }

        // Return based on selected mode
        return switch (particleMode.get()) {
            case Flame -> ParticleTypes.FLAME;
            case Heart -> ParticleTypes.HEART;
            case Portal -> ParticleTypes.PORTAL;
            case Enchant -> ParticleTypes.ENCHANT;
            case Dragonbreath -> ParticleTypes.DRAGON_BREATH;
            case Totem -> ParticleTypes.TOTEM_OF_UNDYING;
            case EndRod -> ParticleTypes.END_ROD;
            case Firework -> ParticleTypes.FIREWORK;
            case Soul -> ParticleTypes.SOUL_FIRE_FLAME;
            case Glow -> ParticleTypes.GLOW;
            case Cherry -> ParticleTypes.CHERRY_LEAVES;
            case Electric -> ParticleTypes.ELECTRIC_SPARK;
        };
    }

    public enum ParticleMode {
        Flame,
        Heart,
        Portal,
        Enchant,
        Dragonbreath,
        Totem,
        EndRod,
        Firework,
        Soul,
        Glow,
        Cherry,
        Electric
    }

    public enum TrailShape {
        Line,
        Spiral,
        Wave,
        Random,
        Circle
    }
}
