package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class Speed extends Module implements ClientTickEvents.EndTick {
    private final Setting<Mode> mode = register(new Setting<>("Mode", Mode.Strafe));
    private final Setting<Float> speed = register(new Setting<>("Speed", 1.0f, 0.1f, 2.0f));
    private final Setting<Float> jumpHeight = register(new Setting<>("JumpHeight", 0.4f, 0.2f, 1.0f));

    public Speed() {
        super("Speed", "Increases movement speed", Category.MOVEMENT, true, false, false);
    }

    private enum Mode {
        Strafe,
        BunnyHop,
        Legit
    }

    @Override
    public void onEnable() {
        if (Util.mc.player == null) return;
        ClientTickEvents.END_CLIENT_TICK.register(this);
        System.out.println("[Speed] Module enabled with mode: " + mode.getValue());
    }

    @Override
    public void onDisable() {
        if (Util.mc.player == null) return;
        Util.mc.player.setVelocity(0, Util.mc.player.getVelocity().y, 0);
        System.out.println("[Speed] Module disabled");
    }

    @Override
    public void onEndTick(MinecraftClient client) {
        if (!isEnabled()) return; // Early check to ensure module is enabled
        if (Util.mc.player == null || !Util.mc.player.isOnGround()) return;

        ClientPlayerEntity p = Util.mc.player;
        float yaw = p.getYaw();
        float forward = 0.0f;
        float strafe = 0.0f;

        if (client.options.forwardKey.isPressed()) forward += 1.0f;
        if (client.options.backKey.isPressed()) forward -= 1.0f;
        if (client.options.leftKey.isPressed()) strafe += 1.0f;
        if (client.options.rightKey.isPressed()) strafe -= 1.0f;

        Vec3d motion = new Vec3d(strafe, 0, forward).normalize().rotateY((float) Math.toRadians(-yaw));

        if (mode.getValue() == Mode.Strafe) {
            if (forward != 0 || strafe != 0) {
                p.setVelocity(motion.multiply(0.26f * speed.getValue()).add(0, p.getVelocity().y, 0));
                System.out.println("[Speed] Applied Strafe speed: " + speed.getValue());
            }
        } else if (mode.getValue() == Mode.BunnyHop) {
            if (forward != 0 || strafe != 0) {
                p.setVelocity(motion.multiply(0.26f * speed.getValue()).add(0, p.getVelocity().y, 0));
                p.jump();
                p.setVelocity(p.getVelocity().x, jumpHeight.getValue(), p.getVelocity().z);
                System.out.println("[Speed] Applied BunnyHop speed: " + speed.getValue() + ", jump height: " + jumpHeight.getValue());
            }
        } else if (mode.getValue() == Mode.Legit) {
            if (forward != 0 || strafe != 0) {
                p.setVelocity(motion.multiply(0.26f * (speed.getValue() * 0.5f + 0.5f)).add(0, p.getVelocity().y, 0));
                if (p.isSprinting()) {
                    p.jump();
                    p.setVelocity(p.getVelocity().x, jumpHeight.getValue() * 0.75f, p.getVelocity().z);
                    System.out.println("[Speed] Applied Legit speed: " + (speed.getValue() * 0.5f + 0.5f) + ", jump height: " + (jumpHeight.getValue() * 0.75f));
                }
            }
        }
    }
}