package me.alpha432.oyvey.features.modules.combat;

//Made with <3 by casxx.deb/ShadyShift
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AimAssist extends Module {
    private final Setting<Double> range = register(new Setting<>("Range", 6.0, 1.0, 10.0));
    private final Setting<TargetMode> targetMode = register(new Setting<>("Target", TargetMode.PLAYERS));
    private final Setting<Boolean> legitMode = register(new Setting<>("Legit", false));
    private final Setting<Float> speed = register(new Setting<>("Speed", 5.0f, 1.0f, 10.0f));
    private final Setting<Float> smoothness = register(new Setting<>("Smoothness", 0.5f, 0.1f, 1.0f)); // Hogere waarde = soepeler

    private final Random random = new Random(); // Voor human-like jitter

    public enum TargetMode {
        PLAYERS, CREATURES, ANIMALS, ALL
    }

    public AimAssist() {
        super("AimAssist", "Automatically aims at nearby entities", Category.COMBAT, true, false, false);
    }

    @Override
    public void onUpdate() {
        if (Util.mc.player == null || Util.mc.world == null) return;

        ClientPlayerEntity player = Util.mc.player;


        double maxRange = legitMode.getValue() ? 3.0 : range.getValue();


        List<LivingEntity> targets = Util.mc.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(maxRange), this::isValidTarget)
                .stream()
                .filter(entity -> player.distanceTo(entity) <= maxRange)
                .sorted(Comparator.comparingDouble(entity -> player.squaredDistanceTo(entity)))
                .collect(Collectors.toList());

        if (targets.isEmpty()) return;

        LivingEntity target = targets.get(0);


        float[] rotations = getRotationsToEntity(target);
        smoothRotate(player, rotations[0], rotations[1]);
    }

    private boolean isValidTarget(LivingEntity entity) {
        switch (targetMode.getValue()) {
            case PLAYERS:
                return entity instanceof PlayerEntity && entity != Util.mc.player;
            case CREATURES:
                return entity instanceof HostileEntity;
            case ANIMALS:
                return entity instanceof PassiveEntity;
            case ALL:
                return entity != Util.mc.player && entity instanceof LivingEntity;
            default:
                return false;
        }
    }

    private float[] getRotationsToEntity(Entity entity) {
        double diffX = entity.getX() - Util.mc.player.getX();
        double diffY = (entity.getEyeY()) - (Util.mc.player.getEyeY());
        double diffZ = entity.getZ() - Util.mc.player.getZ();
        double dist = MathHelper.sqrt((float)(diffX * diffX + diffZ * diffZ));
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(diffY, dist) * 180.0 / Math.PI);

        yaw += (random.nextFloat() - 0.5f) * 2.0f;
        pitch += (random.nextFloat() - 0.5f) * 2.0f;

        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }

    private void smoothRotate(ClientPlayerEntity player, float targetYaw, float targetPitch) {
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = MathHelper.wrapDegrees(targetPitch - currentPitch);


        float smoothFactor = 1.0f - smoothness.getValue();
        if (smoothFactor == 0.0f) smoothFactor = 0.01f;

        float maxSpeed = speed.getValue();
        float yawStep = yawDiff * smoothFactor;
        float pitchStep = pitchDiff * smoothFactor;

        yawStep = MathHelper.clamp(yawStep, -maxSpeed, maxSpeed);
        pitchStep = MathHelper.clamp(pitchStep, -maxSpeed, maxSpeed);

        player.setYaw(currentYaw + yawStep);
        player.setPitch(currentPitch + pitchStep);
    }
}