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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {
    private final Setting<Double> range = register(new Setting<>("Range", 6.0, 1.0, 10.0));
    private final Setting<TargetMode> targetMode = register(new Setting<>("Target", TargetMode.PLAYERS));
    private final Setting<Boolean> pvp19Mode = register(new Setting<>("1.9PvP", false));
    private final Setting<Boolean> legitMode = register(new Setting<>("Legit", false));

    public enum TargetMode {
        PLAYERS, CREATURES, ANIMALS, ALL
    }

    public TriggerBot() {
        super("TriggerBot", "Automatically attacks entity under crosshair", Category.COMBAT, true, false, false);
    }

    @Override
    public void onUpdate() {
        if (Util.mc.player == null || Util.mc.world == null) return;

        ClientPlayerEntity player = Util.mc.player;


        if (pvp19Mode.getValue() && Util.mc.player.getAttackCooldownProgress(0.0f) < 1.0f) {
            return;
        }


        HitResult hitResult = Util.mc.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            return;
        }

        EntityHitResult entityHitResult = (EntityHitResult) hitResult;
        Entity entity = entityHitResult.getEntity();

        if (!(entity instanceof LivingEntity)) {
            return;
        }

        LivingEntity target = (LivingEntity) entity;


        if (!isValidTarget(target)) {
            return;
        }


        double maxRange = legitMode.getValue() ? 3.0 : range.getValue();
        if (player.distanceTo(target) > maxRange) {
            return;
        }


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
}