package com.mysticalrange;

import com.blakebr0.cucumber.container.BaseContainerMenu;
import com.blakebr0.mysticalagriculture.item.MachineUpgradeItem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Remembers which machines the player asked to see the work area of, and draws those areas.
 *
 * <p>How big the area is. Both machines use the same rule (copied from
 * {@code HarvesterTileEntity.findNextPosition} / {@code FertilizerTileEntity.findNextPosition}):
 * a flat square, one block tall, at the machine's own height, directly in front of the side the
 * machine faces. Its "range" is the distance from the square's centre to its edge: 1 without an
 * upgrade, or 1 plus the upgrade tier's added range. So the square is (2 x range + 1) blocks wide,
 * and its centre sits (range + 1) blocks out so the near edge touches the machine.
 *
 * <p>Where the range comes from. The upgrade sits in the machine's first slot (slot 0 in both
 * menus). While the screen is open the client has a copy of that slot, so we read it there. The
 * machine's block entity on the client is not a reliable source: the server does not re-send it
 * when the upgrade changes. So the range is captured while the screen is open and remembered.
 *
 * <p>How it is drawn. Minecraft 26.1 has "gizmos": simple debug shapes (boxes, lines) that any
 * code can emit while a collector is listening. During every client tick the game listens, and
 * whatever was emitted is drawn each frame until the next tick. So we re-emit every tick.
 */
final class WorkAreas {
    // The machines that work on an area. Matched by block id rather than by class, so Mystical
    // Automation's classes are never touched and this mod still loads in packs without it.
    private static final Set<Identifier> RANGED_MACHINES = Set.of(
            Identifier.fromNamespaceAndPath("mysticalagriculture", "harvester"),
            Identifier.fromNamespaceAndPath("mysticalautomation", "fertilizer"));

    // Placed just left of the upgrade slot, which both machine screens draw at (152, 9).
    private static final int BUTTON_X = 134;
    private static final int BUTTON_Y = 10;
    private static final int BUTTON_SIZE = 14;

    // Colours are ARGB: alpha (opacity) first, then red, green, blue. Solid green edges with a
    // faint green tint on the faces, so the square reads clearly without hiding the crops.
    private static final GizmoStyle AREA_STYLE = GizmoStyle.strokeAndFill(0xFF4CE04C, 2.5F, 0x284CE04C);

    // Machine position -> its range. Only machines whose area is switched on are in here.
    private static final Map<BlockPos, Integer> shownRanges = new HashMap<>();

    private WorkAreas() {
    }

    /** Adds the toggle button when a Harvester or Fertilizer screen opens (or is resized). */
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !(screen.getMenu() instanceof BaseContainerMenu menu)) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        BlockPos pos = menu.getBlockPos();
        if (level == null || !isRangedMachine(level.getBlockState(pos))) {
            return;
        }
        // Copy the position: the button outlives this call and must not share a mutable instance.
        BlockPos machinePos = pos.immutable();
        Button button = Button.builder(label(machinePos), pressed -> {
                    toggle(machinePos, rangeFromUpgradeSlot(menu));
                    pressed.setMessage(label(machinePos));
                    pressed.setTooltip(tooltip(machinePos));
                })
                .bounds(screen.getLeftPos() + BUTTON_X, screen.getTopPos() + BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(tooltip(machinePos))
                .build();
        event.addListener(button);
    }

    /** Refreshes the open machine's range and re-emits the outline of every switched-on area. */
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        refreshOpenMachineRange(minecraft.screen);

        Iterator<Map.Entry<BlockPos, Integer>> entries = shownRanges.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = entries.next();
            BlockPos pos = entry.getKey();
            // Out of render distance, or in another dimension: keep it switched on, just don't draw.
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            // The machine was broken or replaced; its outline would now point at nothing.
            if (!isRangedMachine(state)) {
                entries.remove();
                continue;
            }
            // Facing is read every tick so rotating the machine moves the outline with it.
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            Gizmos.cuboid(workArea(pos, facing, entry.getValue()), AREA_STYLE);
        }
    }

    /** Forgets everything on disconnect: positions from one world mean nothing in the next. */
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        shownRanges.clear();
    }

    private static boolean isRangedMachine(BlockState state) {
        return RANGED_MACHINES.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static void toggle(BlockPos pos, int range) {
        if (shownRanges.remove(pos) == null) {
            shownRanges.put(pos, range);
        }
    }

    /** Keeps the remembered range in step if the player swaps the upgrade with the screen open. */
    private static void refreshOpenMachineRange(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen
                && containerScreen.getMenu() instanceof BaseContainerMenu menu
                && shownRanges.containsKey(menu.getBlockPos())) {
            shownRanges.put(menu.getBlockPos(), rangeFromUpgradeSlot(menu));
        }
    }

    private static int rangeFromUpgradeSlot(BaseContainerMenu menu) {
        return menu.getSlot(0).getItem().getItem() instanceof MachineUpgradeItem upgrade
                ? 1 + upgrade.getTier().getAddedRange()
                : 1;
    }

    /** The one-block-tall square in front of the machine, as a box in world coordinates. */
    private static AABB workArea(BlockPos machinePos, Direction facing, int range) {
        BlockPos centre = machinePos.relative(facing, range + 1);
        // AABB corners are block corners, so the far corner is one past the last block. The tiny
        // outward growth keeps the faint fill from flickering against the ground and crop faces.
        return new AABB(
                        centre.getX() - range, centre.getY(), centre.getZ() - range,
                        centre.getX() + range + 1, centre.getY() + 1, centre.getZ() + range + 1)
                .inflate(0.01);
    }

    private static Component label(BlockPos pos) {
        // Filled square when the outline is on, hollow when off.
        return Component.literal(shownRanges.containsKey(pos) ? "■" : "□");
    }

    private static Tooltip tooltip(BlockPos pos) {
        return Tooltip.create(Component.literal(shownRanges.containsKey(pos) ? "Hide working area" : "Show working area"));
    }
}
