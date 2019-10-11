package com.dre.brewery.integration;

import com.dre.brewery.Barrel;
import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;

public class IntegrationListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBarrelAccessLowest(BarrelAccessEvent event) {
		if (P.p.useWG) {
			Plugin plugin = P.p.getServer().getPluginManager().getPlugin("WorldGuard");
			if (plugin != null) {
				try {
					if (!P.p.wg.checkAccess(event.getPlayer(), event.getSpigot(), plugin)) {
						event.setCancelled(true);
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					P.p.errorLog("Failed to Check WorldGuard for Barrel Open Permissions!");
					P.p.errorLog("Brewery was tested with version 5.8 to 6.1 of WorldGuard!");
					P.p.errorLog("Disable the WorldGuard support in the config and do /brew reload");
					e.printStackTrace();
					P.p.msg(event.getPlayer(), "&cError opening Barrel, please report to an Admin!");
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBarrelAccess(BarrelAccessEvent event) {
		if (P.p.useGP) {
			if (P.p.getServer().getPluginManager().isPluginEnabled("GriefPrevention")) {
				try {
					if (!GriefPreventionBarrel.checkAccess(event)) {
						event.setCancelled(true);
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					P.p.errorLog("Failed to Check GriefPrevention for Barrel Open Permissions!");
					P.p.errorLog("Brewery was tested with GriefPrevention 14.5.4");
					P.p.errorLog("Disable the GriefPrevention support in the config and do /brew reload");
					e.printStackTrace();
					P.p.msg(event.getPlayer(), "&cError opening Barrel, please report to an Admin!");
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onBarrelDestroy(BarrelDestroyEvent event) {
		if (!P.p.useLWC) return;

		if (event.hasPlayer()) {
			try {
				if (LWCBarrel.denyDestroy(event.getPlayerOptional(), event.getBarrel())) {
					event.setCancelled(true);
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				P.p.errorLog("Failed to Check LWC for Barrel Break Permissions!");
				P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				P.p.errorLog("Disable the LWC support in the config and do /brew reload");
				e.printStackTrace();
				P.p.msg(event.getPlayerOptional(), "&cError breaking Barrel, please report to an Admin!");
			}
		} else {
			try {
				if (event.getReason() == BarrelDestroyEvent.Reason.EXPLODED) {
					if (LWCBarrel.denyExplosion(event.getBarrel())) {
						event.setCancelled(true);
					}
				} else {
					if (LWCBarrel.denyDestroyOther(event.getBarrel())) {
						event.setCancelled(true);
					}
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				P.p.errorLog("Failed to Check LWC on Barrel Destruction!");
				P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				P.p.errorLog("Disable the LWC support in the config and do /brew reload");
				e.printStackTrace();
			}
		}
	}

	@EventHandler
	public void onBarrelRemove(BarrelRemoveEvent event) {
		if (!P.p.useLWC) return;

		try {
			LWCBarrel.remove(event.getBarrel());
		} catch (Throwable e) {
			P.p.errorLog("Failed to Remove LWC Lock from Barrel!");
			P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (P.p.useLB) {
			if (event.getInventory().getHolder() instanceof Barrel) {
				try {
					LogBlockBarrel.closeBarrel(event.getPlayer(), event.getInventory());
				} catch (Exception e) {
					P.p.errorLog("Failed to Log Barrel to LogBlock!");
					P.p.errorLog("Brewery was tested with version 1.94 of LogBlock!");
					e.printStackTrace();
				}
			}
		}
	}
}
