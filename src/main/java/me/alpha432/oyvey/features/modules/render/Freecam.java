package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
    private final Setting<Float> speed = register(new Setting<>("Speed", 1.0f, 0.1f, 5.0f));

    private Vec3d oldPos;
    private float oldYaw;
    private float oldPitch;
    private boolean forwardPressed;
    private boolean backPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean sneakPressed;
    private boolean attackPressed;
    private boolean usePressed;
    private Vec3d cameraPos;

    public Freecam() {
        super("Freecam", "Move your camera freely like spectator mode", Category.RENDER, true, false, false);
    }

    @Override
    public void onEnable() {
        if (Util.mc.player == null || Util.mc.world == null) return;

        ClientPlayerEntity player = Util.mc.player;

        oldPos = player.getPos();
        oldYaw = player.getYaw();
        oldPitch = player.getPitch();
        cameraPos = player.getPos();

        player.setVelocity(0, 0, 0);
        player.getAbilities().flying = false;
        player.setNoGravity(true);

        forwardPressed = Util.mc.options.forwardKey.isPressed();
        backPressed = Util.mc.options.backKey.isPressed();
        leftPressed = Util.mc.options.leftKey.isPressed();
        rightPressed = Util.mc.options.rightKey.isPressed();
        jumpPressed = Util.mc.options.jumpKey.isPressed();
        sneakPressed = Util.mc.options.sneakKey.isPressed();
        attackPressed = Util.mc.options.attackKey.isPressed();
        usePressed = Util.mc.options.useKey.isPressed();

        Util.mc.options.forwardKey.setPressed(false);
        Util.mc.options.backKey.setPressed(false);
        Util.mc.options.leftKey.setPressed(false);
        Util.mc.options.rightKey.setPressed(false);
        Util.mc.options.jumpKey.setPressed(false);
        Util.mc.options.sneakKey.setPressed(false);
        Util.mc.options.attackKey.setPressed(true);
        Util.mc.options.useKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        if (Util.mc.player == null || Util.mc.world == null) return;

        ClientPlayerEntity player = Util.mc.player;

        player.setPos(oldPos.x, oldPos.y, oldPos.z);
        player.setYaw(oldYaw);
        player.setPitch(oldPitch);
        player.setNoGravity(false);
        player.setVelocity(0, 0, 0);

        Util.mc.options.forwardKey.setPressed(forwardPressed);
        Util.mc.options.backKey.setPressed(backPressed);
        Util.mc.options.leftKey.setPressed(leftPressed);
        Util.mc.options.rightKey.setPressed(rightPressed);
        Util.mc.options.jumpKey.setPressed(jumpPressed);
        Util.mc.options.sneakKey.setPressed(sneakPressed);
        Util.mc.options.attackKey.setPressed(attackPressed);
        Util.mc.options.useKey.setPressed(usePressed);
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
                .multiply(speed.getValue());

        double verticalMotion = vertical * speed.getValue();

        Vec3d direction = new Vec3d(horizontalDirection.x, verticalMotion, horizontalDirection.z);

        cameraPos = cameraPos.add(direction);
        Util.mc.player.setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        Util.mc.player.noClip = true;
    }
}