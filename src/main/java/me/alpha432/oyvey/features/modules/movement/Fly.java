package me.alpha432.oyvey.features.modules.movement;

//Made with <3 by casxx.deb/ShadyShift
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Fly extends Module {
    private final Setting<Float> horizontalSpeed = register(new Setting<>("HorizontalSpeed", 0.1f, 0.1f, 2.0f));
    private final Setting<Float> verticalSpeed = register(new Setting<>("VerticalSpeed", 0.1f, 0.1f, 2.0f));
    private final Setting<Boolean> antiKick = register(new Setting<>("AntiKick", true));

    public Fly() {
        super("Fly", "Allows you to fly like in creative mode", Category.MOVEMENT, true, false, false);
    }

    @Override
    public void onEnable() {
        if (Util.mc.player == null) return;
        Util.mc.player.getAbilities().flying = false;
        Util.mc.player.setVelocity(0, 0, 0);
    }

    @Override
    public void onDisable() {
        if (Util.mc.player == null) return;
        Util.mc.player.getAbilities().flying = false;
        Util.mc.player.setVelocity(0, 0, 0);
    }

    @Override
    public void onUpdate() {
        if (Util.mc.player == null || Util.mc.world == null) return;

        ClientPlayerEntity player = Util.mc.player;

        player.getAbilities().flying = false;

        float forward = 0.0f;
        float strafe = 0.0f;
        if (Util.mc.options.forwardKey.isPressed()) forward += 1.0f;
        if (Util.mc.options.backKey.isPressed()) forward -= 1.0f;
        if (Util.mc.options.leftKey.isPressed()) strafe += 1.0f;
        if (Util.mc.options.rightKey.isPressed()) strafe -= 1.0f;
        float vertical = Util.mc.options.jumpKey.isPressed() ? 1.0f : Util.mc.options.sneakKey.isPressed() ? -1.0f : 0.0f;

        Vec3d horizontalDirection = new Vec3d(strafe, 0, forward)
                .rotateY((float) Math.toRadians(-player.getYaw()))
                .normalize()
                .multiply(horizontalSpeed.getValue());

        double verticalMotion = vertical * verticalSpeed.getValue();

        Vec3d direction = new Vec3d(horizontalDirection.x, verticalMotion, horizontalDirection.z);

        player.setVelocity(direction);

        if (antiKick.getValue()) {
            player.setVelocity(player.getVelocity().add(0, -0.01, 0));
        }
    }
}