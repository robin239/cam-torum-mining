package com.cwjoshuak;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Cam Torum Mining",
	description = "Highlights water streams in Cam Torum mine.",
	tags = {"cam", "cam torum", "mine", "mining", "calcified", "calficied rock"}
)
@Getter
public class CamTorumMiningPlugin extends Plugin
{
	private static final int CAM_TORUM_REGION = 6037;
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;

	@Inject
	private CamTorumMiningConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CamTorumMiningOverlay overlay;

	@Getter
	private final Map<TileObject, Tile> streams = new HashMap<>();

	@Getter
	private final Map<WorldPoint, TileObject> rocks = new HashMap<>();

	private static final Set<Integer> ROCK_OBJECT_IDS = ImmutableSet.of(
		ObjectID.ROCKS_51486,
		ObjectID.ROCKS_51488,
		ObjectID.ROCKS_51490,
		ObjectID.ROCKS_51492
	);
	private boolean inCamTorumMiningArea;


	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		if (client.getGameState() == GameState.LOGGED_IN) {
			clientThread.invokeLater(() ->
			{
				inCamTorumMiningArea = client.getLocalPlayer().getWorldLocation().getRegionID() == CAM_TORUM_REGION;
			});
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		streams.clear();
		rocks.clear();
		inCamTorumMiningArea = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
			case LOGIN_SCREEN:
			case HOPPING:
				streams.clear();
				rocks.clear();
				inCamTorumMiningArea = client.getLocalPlayer().getWorldLocation().getRegionID() == CAM_TORUM_REGION;
		}
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		onTileObject(event.getTile(), null, event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		onTileObject(event.getTile(), event.getDecorativeObject(), null);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		onTileObject(event.getTile(), null, event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		onTileObject(event.getTile(), event.getGameObject(), null);
	}


	private void onTileObject(Tile tile, TileObject oldObject, TileObject newObject)
	{
		streams.remove(oldObject);
		rocks.remove(oldObject);

		if (newObject == null || !isInCamTorumMiningArea())
		{
			return;
		}
		if (newObject.getId() == 51493)
		{
			streams.put(newObject, tile);
			return;
		}
		if (ROCK_OBJECT_IDS.contains(newObject.getId()))
		{
			rocks.put(newObject.getWorldLocation(), newObject);
			return;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (!isInCamTorumMiningArea()) {
			return;
		}

		if (config.dynamicMenuEntrySwap()) {
			swapRockMenuEntries(event);
		}
	}

	private void swapRockMenuEntries(MenuEntryAdded event) {
		String target = event.getTarget();
		if (target.contains("Rocks")) {
			MenuEntry entry = event.getMenuEntry();
			WorldPoint entryTargetPoint = WorldPoint.fromScene(client, entry.getParam0(), entry.getParam1(), client.getPlane());
			if (rocks.get(entryTargetPoint) != null) {
				entry.setDeprioritized(true);
			}
		}
	}

	@Provides
	CamTorumMiningConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CamTorumMiningConfig.class);
	}
}
