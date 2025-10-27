package me.alpha432.oyvey.features.modules.combat;

import com.google.common.eventbus.Subscribe;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.traits.Util;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class AutoCrystal extends Module implements ClientTickEvents.EndTick {
    private final Setting<TargetMode> targetMode = register(new Setting<>("TargetMode", TargetMode.Players));
    private final Setting<Float> placeRange = register(new Setting<>("PlaceRange", 4.0f, 1.0f, 6.0f));
    private final Setting<Float> breakRange = register(new Setting<>("BreakRange", 4.0f, 1.0f, 6.0f));
    private final Setting<Float> minDamage = register(new Setting<>("MinDamage", 6.0f, 0.0f, 20.0f));
    private final Setting<Float> maxSelfDamage = register(new Setting<>("MaxSelfDamage", 4.0f, 0.0f, 20.0f));
    private final Setting<Integer> placeDelay = register(new Setting<>("PlaceDelay", 0, 0, 20));
    private final Setting<Integer> breakDelay = register(new Setting<>("BreakDelay", 0, 0, 20));
    private final Setting<Boolean> offHandCrystal = register(new Setting<>("OffHandCrystal", false));
    private final Setting<Boolean> render = register(new Setting<>("Render", true));
    private final Setting<Color> color = register(new Setting<>("Color", new Color(255, 0, 0, 255)));
    private final Setting<Float> lineWidth = register(new Setting<>("LineWidth", 1.0f, 0.1f, 5.0f));
    private final Setting<Integer> maxLoopIterations = register(new Setting<>("MaxLoopIterations", 100, 50, 500));
    private final Setting<AggressionLevel> aggression = register(new Setting<>("Aggression", AggressionLevel.Medium));
    private int placeTicks;
    private int breakTicks;
    private BlockPos lastPlacePos;
    private List<LivingEntity> cachedEntities;

    public AutoCrystal() {
        super("AutoCrystal", "Automatically places and breaks end crystals", Category.COMBAT, true, false, false);
    }

    private enum TargetMode {
        Players,
        Animals,
        Creatures,
        All
    }

    private enum AggressionLevel {
        Low,
        Medium,
        High
    }

    @Override
    public void onEnable() {
        placeTicks = 0;
        breakTicks = 0;
        lastPlacePos = null;
        cachedEntities = new ArrayList<>();
        ClientTickEvents.END_CLIENT_TICK.register(this);
        System.out.println("[AutoCrystal] Module enabled");
    }

    @Override
    public void onDisable() {
        placeTicks = 0;
        breakTicks = 0;
        lastPlacePos = null;
        cachedEntities = null;
        System.out.println("[AutoCrystal] Module disabled");
    }

    @Override
    public void onEndTick(MinecraftClient client) {
        if (!isEnabled()) return; // Early check to ensure module is enabled
        if (client.player == null || client.world == null) return;

        // Cache entities to reduce performance impact
        if (client.world.getTime() % 5 == 0) {
            cachedEntities = client.world.getEntitiesByClass(LivingEntity.class, new Box(client.player.getBlockPos()).expand(breakRange.getValue()), e -> isValidTarget(e));
        }

        if (placeTicks < (placeDelay.getValue() != null ? placeDelay.getValue() : 0)) {
            placeTicks++;
        } else {
            List<BlockPos> bestPlacePositions = findBestCrystalPositions(client);
            if (!bestPlacePositions.isEmpty()) {
                Hand hand = offHandCrystal.getValue() ? Hand.OFF_HAND : Hand.MAIN_HAND;
                boolean hasCrystal = false;

                // Check if the desired hand already has an End Crystal
                if (offHandCrystal.getValue()) {
                    hasCrystal = client.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
                    if (!hasCrystal) {
                        swapToOffHand(client, Items.END_CRYSTAL);
                        hasCrystal = client.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
                    }
                } else {
                    hasCrystal = client.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
                    if (!hasCrystal) {
                        swapToItem(client, Items.END_CRYSTAL);
                        hasCrystal = client.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
                    }
                }

                if (!hasCrystal) {
                    System.out.println("[AutoCrystal] No End Crystal in selected hand after swap attempt");
                    return;
                }

                int maxPlacements = aggression.getValue() == AggressionLevel.High ? 3 : aggression.getValue() == AggressionLevel.Medium ? 2 : 1;
                for (int i = 0; i < Math.min(bestPlacePositions.size(), maxPlacements); i++) {
                    BlockPos pos = bestPlacePositions.get(i);
                    BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos.down()), Direction.UP, pos.down(), false);
                    client.interactionManager.interactBlock(client.player, hand, hitResult);
                    System.out.println("[AutoCrystal] Attempted to place End Crystal at " + pos + " using " + (offHandCrystal.getValue() ? "off-hand" : "main hand"));
                    lastPlacePos = pos;
                }
                placeTicks = 0;
            } else {
                System.out.println("[AutoCrystal] No valid positions found for End Crystal placement");
            }
        }

        if (breakTicks < (breakDelay.getValue() != null ? breakDelay.getValue() : 0)) {
            breakTicks++;
        } else {
            EndCrystalEntity crystal = findCrystalToBreak(client);
            if (crystal != null) {
                client.interactionManager.attackEntity(client.player, crystal);
                System.out.println("[AutoCrystal] Breaking End Crystal at " + crystal.getBlockPos());
                breakTicks = 0;
            }
        }
    }

    private List<BlockPos> findBestCrystalPositions(MinecraftClient client) {
        if (client.player == null || client.world == null) return new ArrayList<>();

        BlockPos playerPos = client.player.getBlockPos();
        List<BlockPos> validPositions = new ArrayList<>();
        float rangeValue = placeRange.getValue() != null ? placeRange.getValue() : 4.0f;
        int range = (int) rangeValue;
        int maxIterations = maxLoopIterations.getValue();
        int iterations = 0;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    iterations++;
                    if (iterations > maxIterations) break;
                    BlockPos pos = playerPos.add(x, y, z);
                    if (isValidPlacePos(pos) && client.world.getBlockState(pos).isAir() && client.player.getPos().distanceTo(Vec3d.ofCenter(pos)) <= rangeValue) {
                        float damage = calculateDamage(pos, client);
                        if (damage >= minDamage.getValue()) {
                            float selfDamage = calculateSelfDamage(pos, client);
                            if (selfDamage <= maxSelfDamage.getValue()) {
                                validPositions.add(pos);
                            }
                        }
                    }
                }
            }
        }

        validPositions.sort((a, b) -> Float.compare(calculateDamage(b, client), calculateDamage(a, client)));
        return validPositions;
    }

    private boolean isValidPlacePos(BlockPos pos) {
        if (Util.mc.world == null) return false;
        BlockPos below = pos.down();
        return Util.mc.world.getBlockState(below).isOf(Blocks.OBSIDIAN) || Util.mc.world.getBlockState(below).isOf(Blocks.BEDROCK);
    }

    private float calculateDamage(BlockPos pos, MinecraftClient client) {
        if (client.world == null || client.player == null || cachedEntities == null) return 0.0f;
        float totalDamage = 0.0f;
        Vec3d crystalPos = Vec3d.ofCenter(pos).add(0, 1, 0);

        for (LivingEntity entity : cachedEntities) {
            float distance = (float) entity.getPos().distanceTo(crystalPos);
            if (distance <= breakRange.getValue()) {
                float damage = 6.0f * (1.0f - distance / 12.0f);
                totalDamage += damage;
            }
        }
        return totalDamage;
    }

    private float calculateSelfDamage(BlockPos pos, MinecraftClient client) {
        if (client.player == null || client.world == null) return 0.0f;
        Vec3d crystalPos = Vec3d.ofCenter(pos).add(0, 1, 0);
        float distance = (float) client.player.getPos().distanceTo(crystalPos);
        return 6.0f * (1.0f - distance / 12.0f);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == Util.mc.player) return false;
        switch (targetMode.getValue()) {
            case Players:
                return entity instanceof PlayerEntity;
            case Animals:
                return entity instanceof AnimalEntity;
            case Creatures:
                return entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && !(entity instanceof AnimalEntity);
            case All:
                return entity instanceof LivingEntity;
            default:
                return false;
        }
    }

    private EndCrystalEntity findCrystalToBreak(MinecraftClient client) {
        if (client.world == null || client.player == null) return null;
        List<EndCrystalEntity> crystals = client.world.getEntitiesByClass(EndCrystalEntity.class, new Box(client.player.getBlockPos()).expand(breakRange.getValue()), e -> true);
        return crystals.stream()
                .filter(c -> client.player.getPos().distanceTo(c.getPos()) <= breakRange.getValue())
                .max(Comparator.comparingDouble(c -> calculateDamage(c.getBlockPos().down(), client)))
                .orElse(null);
    }

    private void swapToItem(MinecraftClient client, Item item) {
        if (client == null || client.player == null || client.player.getInventory() == null || client.player.networkHandler == null || client.interactionManager == null) {
            System.out.println("[AutoCrystal] Null check failed in swapToItem");
            return;
        }
        PlayerInventory inventory = client.player.getInventory();

        if (client.player.getMainHandStack().getItem() == item) {
            System.out.println("[AutoCrystal] End Crystal already in main hand");
            return;
        }

        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).getItem() == item) {
                client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                System.out.println("[AutoCrystal] Swapped to hotbar slot " + slot + " for End Crystal");
                return;
            }
        }

        int targetHotbarSlot = 0;
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getStack(slot).getItem() == item) {
                System.out.println("[AutoCrystal] Found End Crystal in inventory slot " + slot);
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 36, 0, SlotActionType.PICKUP, client.player);
                if (!inventory.getStack(targetHotbarSlot).isEmpty()) {
                    client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
                }
                client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetHotbarSlot));
                System.out.println("[AutoCrystal] Swapped End Crystal to hotbar slot 0");
                return;
            }
        }
        System.out.println("[AutoCrystal] No End Crystal found in inventory for main hand");
    }

    private void swapToOffHand(MinecraftClient client, Item item) {
        if (client == null || client.player == null || client.player.getInventory() == null || client.player.networkHandler == null || client.interactionManager == null) {
            System.out.println("[AutoCrystal] Null check failed in swapToOffHand");
            return;
        }
        PlayerInventory inventory = client.player.getInventory();

        if (client.player.getOffHandStack().getItem() == item) {
            System.out.println("[AutoCrystal] End Crystal already in off-hand");
            return;
        }

        int itemSlot = -1;
        for (int slot = 0; slot < 36; slot++) {
            if (inventory.getStack(slot).getItem() == item) {
                itemSlot = slot;
                break;
            }
        }

        if (itemSlot != -1) {
            int offHandSlot = 45;
            System.out.println("[AutoCrystal] Found End Crystal in slot " + itemSlot);
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, itemSlot < 9 ? itemSlot + 36 : itemSlot, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, offHandSlot, 0, SlotActionType.PICKUP, client.player);
            if (!inventory.getStack(PlayerInventory.OFF_HAND_SLOT).isEmpty()) {
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, itemSlot < 9 ? itemSlot + 36 : itemSlot, 0, SlotActionType.PICKUP, client.player);
            }
            System.out.println("[AutoCrystal] Swapped End Crystal to off-hand");
        } else {
            System.out.println("[AutoCrystal] No End Crystal found for off-hand");
        }
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (Util.mc.player == null || Util.mc.world == null || lastPlacePos == null || !render.getValue()) return;
        Box box = new Box(lastPlacePos);
        RenderUtil.drawBox(event.getMatrix(), box, color.getValue(), lineWidth.getValue());
    }
}