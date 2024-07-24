package com.cwjoshuak;

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
import java.util.HashSet;
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

	@Inject
	private Notifier notifier;

	@Getter
	private final Set<TileObject> veins = new HashSet<>();

	@Getter
	private final Set<TileObject> streams = new HashSet<>();

	@Getter
	private final Map<WorldPoint, TileObject> rocks = new HashMap<>();

	private static final Set<Integer> ROCK_OBJECT_IDS = ImmutableSet.of(
		ObjectID.ROCKS_51486,
		ObjectID.ROCKS_51488,
		ObjectID.ROCKS_51490,
		ObjectID.ROCKS_51492
	);

	private static final Set<Integer> VEIN_OBJECT_IDS = ImmutableSet.of(
		ObjectID.CALCIFIED_ROCKS,
		ObjectID.CALCIFIED_ROCKS_51487,
		ObjectID.CALCIFIED_ROCKS_51489,
		ObjectID.CALCIFIED_ROCKS_51491
	);

	private static final int STREAM_OBJECT_ID = 51493;

	private boolean inCamTorumMiningArea;

	private int lastNotificationTick;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
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
		veins.clear();
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
			{
				streams.clear();
				veins.clear();
				rocks.clear();
				inCamTorumMiningArea = client.getLocalPlayer().getWorldLocation().getRegionID() == CAM_TORUM_REGION;
				lastNotificationTick = -100; // negative value so instant logging in on water will still notify
				break;
			}
		}
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		onTileObject(null, event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		onTileObject(event.getDecorativeObject(), null);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		onTileObject(null, event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		onTileObject(event.getGameObject(), null);
	}

	private void onTileObject(TileObject oldObject, TileObject newObject)
	{
		if (oldObject != null)
		{
			streams.remove(oldObject);
			veins.remove(oldObject);
			rocks.remove(oldObject.getWorldLocation());
		}

		if (newObject == null || !isInCamTorumMiningArea())
		{
			return;
		}

		int objectId = newObject.getId();
		if (VEIN_OBJECT_IDS.contains(objectId))
		{
			// Add the object to the vein, this will also include streams
			veins.add(newObject);
		}

		if (objectId == STREAM_OBJECT_ID)
		{
			streams.add(newObject);
		}
		else if (ROCK_OBJECT_IDS.contains(objectId))
		{
			rocks.put(newObject.getWorldLocation(), newObject);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!inCamTorumMiningArea || streams.isEmpty() || !config.notifyWater())
		{
			return;
		}

		if (isPlayerMiningNotifiedRock())
		{
			// Already notifier for current set
			return;
		}

		boolean alreadyMiningStream = false;
		Player player = client.getLocalPlayer();
		WorldPoint playerLocation = player.getWorldLocation();

		for (TileObject stream : streams)
		{
			WorldPoint location = stream.getWorldLocation();
			if (playerLocation.distanceTo(location) >= config.maxDistance())
			{
				// Stream is outside the requested distance, do not render.
				continue;
			}

			int distance = Math.abs(playerLocation.getX() - location.getX()) + Math.abs(playerLocation.getY() - location.getY());
			if (distance != 1)
			{
				// Manhattan distance of 1 is adjacent and not diagonal to a stream-the tile to be able to mine it
				continue;
			}

			if (player.getAnimation() >= 0)
			{
				// Assuming they are performing a mining animation if it isn't -1
				alreadyMiningStream = true;
				break;
			}
		}

		if (!alreadyMiningStream)
		{
			notifier.notify("Watery rocks spawned!");
		}
	}

	private boolean isPlayerMiningNotifiedRock()
	{
		int ticksSinceNotified = client.getTickCount() - lastNotificationTick;
		if (ticksSinceNotified < 52)
		{
			// Streams last for about 45 or 50 game ticks
			return true;
		}

		lastNotificationTick = client.getTickCount();
		return false;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (config.dynamicMenuEntrySwap())
		{
			swapRockMenuEntries(event);
		}
	}

	private void swapRockMenuEntries(MenuEntryAdded event)
	{
		if (!isInCamTorumMiningArea())
		{
			return;
		}

		String target = event.getTarget();
        if (!target.contains("Rocks"))
		{
            return;
        }

        MenuEntry entry = event.getMenuEntry();
        WorldPoint entryTargetPoint = WorldPoint.fromScene(client, entry.getParam0(), entry.getParam1(), client.getPlane());

        if (rocks.get(entryTargetPoint) != null)
        {
            entry.setDeprioritized(true);
        }
    }

	@Provides
	CamTorumMiningConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CamTorumMiningConfig.class);
	}
}
