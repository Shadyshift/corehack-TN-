package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AutoTotem extends Module {
    private final Setting<Item> fallback = register(new Setting<>("Fallback", Items.AIR));
    private final Setting<Integer> delay = register(new Setting<>("Delay", 0, 0, 100));
    private final Setting<Integer> maxTotems = register(new Setting<>("MaxTotems", 0, 0, 20));
    private final Setting<Float> healthThreshold = register(new Setting<>("HealthThreshold", 10.0f, 1.0f, 20.0f));
    private final Setting<Boolean> fallDamageSwitch = register(new Setting<>("FallDamageSwitch", false));
    private final Setting<Integer> revertDelay = register(new Setting<>("RevertDelay", 20, 0, 100));
    private int ticks;
    private int totemCount;
    private int revertTicks;
    private Item originalOffHandItem;

    public AutoTotem() {
        super("AutoTotem", "Automatically equips totems in off-hand", Category.PLAYER, true, false, false);
    }

    @Override
    public void onEnable() {
        ticks = 0;
        totemCount = 0;
        revertTicks = 0;
        if (Util.mc.player != null) {
            originalOffHandItem = Util.mc.player.getOffHandStack().getItem();
        } else {
            originalOffHandItem = Items.AIR;
        }
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    public void onDisable() {
        ticks = 0;
        totemCount = 0;
        revertTicks = 0;
        originalOffHandItem = Items.AIR;
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.player.isSpectator()) return;

        if (ticks < delay.getValue()) {
            ticks++;
            return;
        }

        PlayerInventory inventory = client.player.getInventory();
        Item offHandItem = inventory.getStack(PlayerInventory.OFF_HAND_SLOT).getItem();
        float health = client.player.getHealth();
        boolean shouldSwitchForFall = fallDamageSwitch.getValue() && client.player.fallDistance > 3.0f && client.player.getVelocity().y < 0;

        if ((health <= healthThreshold.getValue() || shouldSwitchForFall) && offHandItem != Items.TOTEM_OF_UNDYING && (maxTotems.getValue() == 0 || totemCount < maxTotems.getValue())) {
            int totemSlot = -1;
            for (int i = 0; i < 36; i++) {
                if (inventory.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    totemSlot = i;
                    break;
                }
            }

            if (totemSlot != -1) {
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, totemSlot < 9 ? totemSlot + 36 : totemSlot, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, client.player);
                totemCount++;
                ticks = 0;
                revertTicks = 0;
            }
        } else if (health > healthThreshold.getValue() && !shouldSwitchForFall && offHandItem == Items.TOTEM_OF_UNDYING && client.player.isOnGround()) {
            if (revertTicks < revertDelay.getValue()) {
                revertTicks++;
                return;
            }

            int originalSlot = -1;
            for (int i = 0; i < 36; i++) {
                if (inventory.getStack(i).getItem() == originalOffHandItem) {
                    originalSlot = i;
                    break;
                }
            }

            if (originalSlot != -1) {
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, originalSlot < 9 ? originalSlot + 36 : originalSlot, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, client.player);
                ticks = 0;
                revertTicks = 0;
            } else if (fallback.getValue() != Items.AIR && offHandItem != fallback.getValue()) {
                int fallbackSlot = -1;
                for (int i = 0; i < 36; i++) {
                    if (inventory.getStack(i).getItem() == fallback.getValue()) {
                        fallbackSlot = i;
                        break;
                    }
                }
                if (fallbackSlot != -1) {
                    client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, fallbackSlot < 9 ? fallbackSlot + 36 : fallbackSlot, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, client.player);
                    ticks = 0;
                    revertTicks = 0;
                }
            }
        }
    }
}