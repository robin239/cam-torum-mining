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
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.HashMap;
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

	@Inject
	private Notifier notifier;

	private static final Set<Integer> ROCK_OBJECT_IDS = ImmutableSet.of(
		ObjectID.ROCKS_51486,
		ObjectID.ROCKS_51488,
		ObjectID.ROCKS_51490,
		ObjectID.ROCKS_51492
	);
	private boolean inCamTorumMiningArea;

	private int lastNotificationTick;

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
				lastNotificationTick = -100; // negative value so instant logging in on water will still notify
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
		if (oldObject != null) {
			streams.remove(oldObject);
			rocks.remove(oldObject.getWorldLocation());
		}

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
	public void onGameTick(GameTick event)
	{
		if (!inCamTorumMiningArea || streams.isEmpty() || !config.notifyWater())
		{
			return;
		}

		int ticksSinceNotif = client.getTickCount() - lastNotificationTick;
		if (ticksSinceNotif < 52)
		{ // streams last for about 45 or 50 game ticks
			return; // already notifier for current set
		}

		lastNotificationTick = client.getTickCount();

		boolean alreadyMiningStream = false;
		WorldPoint wp = client.getLocalPlayer().getWorldLocation();
		for (Map.Entry<TileObject, Tile> entry : streams.entrySet())
		{
			Tile tile = entry.getValue();
			if (tile.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) >= config.maxDistance())
			{
				continue;
			}

			int dist = Math.abs(wp.getX() - tile.getWorldLocation().getX()) + Math.abs(wp.getY() - tile.getWorldLocation().getY());
			if (dist != 1)
			{ // manhattan distance of 1 is adjacent and not diagonal to a stream-the tile to be able to mine it
				continue;
			}

			if (client.getLocalPlayer().getAnimation() >= 0)
			{ // Assuming they are performing a mining animation if it isn't -1
				alreadyMiningStream = true;
				break;
			}
		}

		if (!alreadyMiningStream)
		{
			notifier.notify("Watery rocks spawned");
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
