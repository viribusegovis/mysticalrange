package com.mysticalrange;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// Client-only mod: it adds a button to two machine screens and draws an outline in the world,
// both of which only exist on the client. The server never needs to know this mod is installed.
//
// Two machines work on an area of the world in front of them rather than on their own inventory:
//  - the Harvester (Mystical Agriculture), which breaks grown crops, and
//  - the Fertilizer (Mystical Automation), which applies fertilizer to crops.
// Neither shows where that area is. This mod adds a toggle button to their screens that outlines
// the area, sized to whatever upgrade is currently in the machine. See WorkAreas for the details.
@Mod(value = MysticalRange.MOD_ID, dist = Dist.CLIENT)
public final class MysticalRange {
    public static final String MOD_ID = "mysticalrange";

    public MysticalRange() {
        // NeoForge.EVENT_BUS is the "game" event bus: screen, tick and connection events fire on it.
        NeoForge.EVENT_BUS.addListener(WorkAreas::onScreenInit);
        NeoForge.EVENT_BUS.addListener(WorkAreas::onClientTick);
        NeoForge.EVENT_BUS.addListener(WorkAreas::onLoggingOut);
    }
}
