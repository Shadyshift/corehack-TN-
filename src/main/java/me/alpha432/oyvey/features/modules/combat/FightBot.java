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
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FightBot extends Module {
    private final Setting<Double> range = register(new Setting<>("Range", 6.0, 1.0, 10.0)); // Standaard 6.0, max 10.0
    private final Setting<TargetMode> targetMode = register(new Setting<>("Target", TargetMode.PLAYERS));
    private final Setting<Boolean> pvp19Mode = register(new Setting<>("1.9PvP", false));

    public enum TargetMode {
        PLAYERS, CREATURES, ANIMALS, ALL
    }

    public FightBot() {
        super("FightBot", "Automatically attacks nearby entities", Category.COMBAT, true, false, false);
    }

    @Override
    public void onUpdate() {
        if (Util.mc.player == null || Util.mc.world == null) return;

        ClientPlayerEntity player = Util.mc.player;

        if (pvp19Mode.getValue() && Util.mc.player.getAttackCooldownProgress(0.0f) < 1.0f) {
            return;
        }

        List<LivingEntity> targets = Util.mc.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range.getValue()), this::isValidTarget)
                .stream()
                .filter(entity -> player.distanceTo(entity) <= range.getValue())
                .sorted(Comparator.comparingDouble(entity -> player.squaredDistanceTo(entity)))
                .collect(Collectors.toList());

        if (targets.isEmpty()) return;

        LivingEntity target = targets.get(0);

        float[] rotations = getRotationsToEntity(target);
        setPlayerRotations(rotations[0], rotations[1]);

        Util.mc.interactionManager.attackEntity(player, target);
        Util.mc.player.swingHand(Hand.MAIN_HAND);
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
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }

    private void setPlayerRotations(float yaw, float pitch) {
        Util.mc.player.setYaw(yaw);
        Util.mc.player.setPitch(pitch);
    }
}