/**
 *
 *     Brewery Minecraft-Plugin for an alternate Brewing Process
 *     Copyright (C) 2021 Milan Albrecht
 *
 *     This file is part of Brewery.
 *
 *     Brewery is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Brewery is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Brewery.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.dre.brewery;

import com.dre.brewery.api.addons.AddonManager;
import com.dre.brewery.commands.CommandManager;
import com.dre.brewery.commands.subcommands.ReloadCommand;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.configurer.TranslationManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.integration.Hook;
import com.dre.brewery.integration.PlaceholderAPIHook;
import com.dre.brewery.integration.barrel.BlocklockerBarrel;
import com.dre.brewery.integration.bstats.Stats;
import com.dre.brewery.integration.listeners.ChestShopListener;
import com.dre.brewery.integration.listeners.IntegrationListener;
import com.dre.brewery.integration.listeners.ShopKeepersListener;
import com.dre.brewery.integration.listeners.SlimefunListener;
import com.dre.brewery.listeners.BlockListener;
import com.dre.brewery.listeners.CauldronListener;
import com.dre.brewery.listeners.EntityListener;
import com.dre.brewery.listeners.InventoryListener;
import com.dre.brewery.listeners.PlayerListener;
import com.dre.brewery.recipe.CustomItem;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.recipe.ItemLoader;
import com.dre.brewery.recipe.PluginItem;
import com.dre.brewery.recipe.SimpleItem;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.storage.StorageInitException;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.LegacyUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import com.dre.brewery.utility.UpdateChecker;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class BreweryPlugin extends JavaPlugin {

	// TODO: File backups
	// TODO: Change the addon API FileManager to use Okaeri

	private static final int RESOURCE_ID = 114777;

	private @Getter static AddonManager addonManager;
	private @Getter static TaskScheduler scheduler;
	private @Getter static BreweryPlugin instance;
	private @Getter static MinecraftVersion MCVersion;
	private @Getter @Setter static DataManager dataManager;
	private @Getter static boolean isFolia = false;
	private @Getter static boolean useNBT = false;


	private final Map<String, Function<ItemLoader, Ingredient>> ingredientLoaders = new HashMap<>(); // Registrations
	private Stats stats; // Metrics


	@Override
	public void onLoad() {
		instance = this;
		MCVersion = MinecraftVersion.getIt();
		scheduler = UniversalScheduler.getScheduler(this);

		// Needs to be here otherwise BreweryX can't get the right language before Okaeri starts loading.
		TranslationManager.newInstance(this.getDataFolder());
	}

	@Override
	public void onEnable() {
		migrateBreweryDataFolder();
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			isFolia = true;
		} catch (ClassNotFoundException ignored) {
		}


		// MC 1.13 uses a different NBT API than the newer versions.
		// We decide here which to use, the new or the old or none at all
		if (LegacyUtil.initNbt()) {
			useNBT = true;
		}

		if (getMCVersion().isOrLater(MinecraftVersion.V1_14)) {
			// Campfires are weird
			// Initialize once now so it doesn't lag later when we check for campfires under Cauldrons
			getServer().createBlockData(Material.CAMPFIRE);
		}


		// Register Item Loaders
		CustomItem.registerItemLoader(this);
		SimpleItem.registerItemLoader(this);
		PluginItem.registerItemLoader(this);

		// Load config and lang
		Config config = ConfigManager.getConfig(Config.class);
		ConfigManager.newInstance(Lang.class, false);

		if (config.isFirstCreation()) {
			config.onFirstCreation();
		}

		BSealer.registerRecipe(); // Sealing table recipe
		ConfigManager.registerDefaultPluginItems(); // Register plugin items
		ConfigManager.loadCauldronIngredients();
		ConfigManager.loadRecipes();
		ConfigManager.loadDistortWords();
		this.stats = new Stats(); // Load metrics

        // Load Addons
		addonManager = new AddonManager(this);
		addonManager.loadAddons();



		Logging.log("Minecraft version&7:&a " + MCVersion.getVersion());
		if (MCVersion == MinecraftVersion.UNKNOWN) {
			Logging.warningLog("This version of Minecraft is not known to Brewery! Please be wary of bugs or other issues that may occur in this version.");
		}


		// Load DataManager
        try {
            dataManager = DataManager.createDataManager(config.getStorage());
        } catch (StorageInitException e) {
			Logging.errorLog("Failed to initialize DataManager!", e);
			Bukkit.getPluginManager().disablePlugin(this);
        }

		DataManager.loadMiscData(dataManager.getBreweryMiscData());
		Barrel.getBarrels().addAll(dataManager.getAllBarrels().stream().filter(Objects::nonNull).toList());
		BCauldron.getBcauldrons().putAll(dataManager.getAllCauldrons().stream().filter(Objects::nonNull).collect(Collectors.toMap(BCauldron::getBlock, Function.identity())));
		BPlayer.getPlayers().putAll(dataManager.getAllPlayers().stream().filter(Objects::nonNull).collect(Collectors.toMap(BPlayer::getUuid, Function.identity())));
		Wakeup.getWakeups().addAll(dataManager.getAllWakeups().stream().filter(Objects::nonNull).toList());


		// Setup Metrics
		this.stats.setupBStats();

		// Register command and aliases
		PluginCommand defaultCommand = getCommand("breweryx");
		defaultCommand.setExecutor(new CommandManager());
		try {
			// This has to be done reflectively because Spigot doesn't expose the CommandMap through the API
			Field bukkitCommandMap = getServer().getClass().getDeclaredField("commandMap");
			bukkitCommandMap.setAccessible(true);

			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(getServer());

			for (String alias : config.getCommandAliases()) {
				commandMap.register(alias, "breweryx", defaultCommand);
			}
		} catch (Exception e) {
			Logging.errorLog("Failed to register command aliases!", e);
		}

		// Register Listeners
		getServer().getPluginManager().registerEvents(new BlockListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		getServer().getPluginManager().registerEvents(new EntityListener(), this);
		getServer().getPluginManager().registerEvents(new InventoryListener(), this);
		getServer().getPluginManager().registerEvents(new IntegrationListener(), this);
		if (getMCVersion().isOrLater(MinecraftVersion.V1_9)) getServer().getPluginManager().registerEvents(new CauldronListener(), this);
		if (Hook.CHESTSHOP.isEnabled() && getMCVersion().isOrLater(MinecraftVersion.V1_13)) getServer().getPluginManager().registerEvents(new ChestShopListener(), this);
		if (Hook.SHOPKEEPERS.isEnabled()) getServer().getPluginManager().registerEvents(new ShopKeepersListener(), this);
		if (Hook.SLIMEFUN.isEnabled() && getMCVersion().isOrLater(MinecraftVersion.V1_14)) getServer().getPluginManager().registerEvents(new SlimefunListener(), this);


		// Heartbeat
		BreweryPlugin.getScheduler().runTaskTimer(new BreweryRunnable(), 650, 1200);
		BreweryPlugin.getScheduler().runTaskTimer(new DrunkRunnable(), 120, 120);
		if (getMCVersion().isOrLater(MinecraftVersion.V1_9)) BreweryPlugin.getScheduler().runTaskTimer(new CauldronParticles(), 1, 1);



		// Register PlaceholderAPI Placeholders
		PlaceholderAPIHook placeholderAPIHook = PlaceholderAPIHook.PLACEHOLDERAPI;
		if (placeholderAPIHook.isEnabled()) {
			placeholderAPIHook.getInstance().register();
		}

		if (config.isUpdateCheck()) {
			UpdateChecker.run(RESOURCE_ID);
		}

		Logging.log("Using scheduler&7: &a" + scheduler.getClass().getSimpleName());
		Logging.log("BreweryX enabled!");
	}

	@Override
	public void onDisable() {
		if (addonManager != null) addonManager.unloadAddons();

		// Disable listeners
		HandlerList.unregisterAll(this);

		// Stop schedulers
		BreweryPlugin.getScheduler().cancelTasks(this);

		if (instance == null) {
			return;
		}

		// save Data to Disk
		if (dataManager != null) dataManager.exit(true, false);

		BSealer.unregisterRecipe();

		PlaceholderAPIHook placeholderAPIHook = PlaceholderAPIHook.PLACEHOLDERAPI;
		if (placeholderAPIHook.isEnabled()) {
			placeholderAPIHook.getInstance().unregister();
		}

		Logging.log("BreweryX disabled!");
	}

	private void migrateBreweryDataFolder() {
		String pluginsFolder = getDataFolder().getParentFile().getPath();

		File breweryFolder = new File(pluginsFolder + File.separator + "Brewery");
		File breweryXFolder = new File(pluginsFolder + File.separator + "BreweryX");

		if (!breweryFolder.exists() || breweryXFolder.exists()) {
			return;
		}

		if (!breweryXFolder.exists()) {
			breweryXFolder.mkdirs();
		}

		File[] files = breweryFolder.listFiles();
		if (files != null) {
			for (File file : files) {
				try {
					Files.copy(file.toPath(), new File(breweryXFolder, file.getName()).toPath());
				} catch (IOException e) {
					Logging.errorLog("Failed to move file: " + file.getName(), e);
				}
			}
			Logging.log("&5Moved files from Brewery to BreweryX's data folder");
		}
	}


	/**
	 * For loading ingredients from ItemMeta.
	 * <p>Register a Static function that takes an ItemLoader, containing a DataInputStream.
	 * <p>Using the Stream it constructs a corresponding Ingredient for the chosen SaveID
	 *
	 * @param saveID The SaveID should be a small identifier like "AB"
	 * @param loadFct The Static Function that loads the Item, i.e.
	 *                public static AItem loadFrom(ItemLoader loader)
	 */
	public void registerForItemLoader(String saveID, Function<ItemLoader, Ingredient> loadFct) {
		ingredientLoaders.put(saveID, loadFct);
	}

	/**
	 * Unregister the ItemLoader
	 *
	 * @param saveID the chosen SaveID
	 */
	public void unRegisterItemLoader(String saveID) {
		ingredientLoaders.remove(saveID);
	}



	// Runnables

	public static class DrunkRunnable implements Runnable {
		@Override
		public void run() {
			if (!BPlayer.isEmpty()) {
				BPlayer.drunkenness();
			}
		}
	}

	public class BreweryRunnable implements Runnable {
		@Override
		public void run() {
			long start = System.currentTimeMillis();

            // runs every min to update cooking time

			for (BCauldron bCauldron : BCauldron.bcauldrons.values()) {
				BreweryPlugin.getScheduler().runTask(bCauldron.getBlock().getLocation(), () -> {
					if (!bCauldron.onUpdate()) {
						BCauldron.bcauldrons.remove(bCauldron.getBlock());
					}
				});
			}


			Barrel.onUpdate();// runs every min to check and update ageing time

			if (getMCVersion().isOrLater(MinecraftVersion.V1_14)) MCBarrel.onUpdate();
			if (Hook.BLOCKLOCKER.isEnabled()) BlocklockerBarrel.clearBarrelSign();

			BPlayer.onUpdate();// updates players drunkenness


			//DataSave.autoSave();
			dataManager.tryAutoSave();

			Logging.debugLog("BreweryRunnable: " + (System.currentTimeMillis() - start) + "ms");
		}

	}

	public static class CauldronParticles implements Runnable {


		@Override
		public void run() {
			Config config = ConfigManager.getConfig(Config.class);

			if (!config.isEnableCauldronParticles()) return;
			if (config.isMinimalParticles() && BCauldron.particleRandom.nextFloat() > 0.5f) {
				return;
			}
			BCauldron.processCookEffects();
		}
	}

}
