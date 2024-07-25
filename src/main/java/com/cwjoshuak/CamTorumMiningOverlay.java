package com.cwjoshuak;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;

public class CamTorumMiningOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 2350;

	private final CamTorumMiningConfig config;
	private final CamTorumMiningPlugin plugin;
	private final Client client;

	private final BufferedImage miningIcon;

	@Inject
	private CamTorumMiningOverlay(Client client, CamTorumMiningConfig config, CamTorumMiningPlugin plugin, SkillIconManager iconManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);

		this.config = config;
		this.plugin = plugin;
		this.client = client;

		miningIcon = iconManager.getSkillImage(Skill.MINING);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInCamTorumMiningArea())
		{
			return null;
		}

		Player player = client.getLocalPlayer();

		renderStreams(graphics, player);
		renderVeins(graphics, player);
		return null;
	}

	private void renderStreams(Graphics2D graphics, Player player)
	{
		if (plugin.getStreams().isEmpty())
		{
			return;
		}

		WorldPoint playerLocation = player.getWorldLocation();
		for (TileObject stream : plugin.getStreams())
		{
			WorldPoint location = stream.getWorldLocation();
			if (playerLocation.distanceTo(location) >= config.maxDistance())
			{
				// Stream is outside the requested distance, do not render
				continue;
			}

			Shape objectClickbox = stream.getClickbox();
			if (objectClickbox == null)
			{
				continue;
			}

			// Draw border
			Color outsideColor = config.getWaterOutlineColor();
			graphics.setColor(outsideColor);
			graphics.draw(objectClickbox);

			// Draw inside
			Color waterFillColor = config.getWaterFillColor();
			Color insideColor = ColorUtil.colorWithAlpha(waterFillColor, waterFillColor.getAlpha() / 12);
			graphics.setColor(insideColor);
			graphics.fill(objectClickbox);
		}
	}

	private void renderVeins(Graphics2D graphics, Player player)
	{
		if (!config.showVeins() || plugin.getVeins().isEmpty())
		{
			return;
		}

		LocalPoint playerLocation = player.getLocalLocation();
		for (TileObject vein : plugin.getVeins())
		{
			Point canvasLocation = Perspective.getCanvasImageLocation(client, vein.getLocalLocation(), miningIcon, 150);
			if (canvasLocation == null)
			{
				continue;
			}

			LocalPoint location = vein.getLocalLocation();
			if (playerLocation.distanceTo(location) >= MAX_DISTANCE)
			{
				// Vein is too far away for the player to see, do not render an icon
				continue;
			}

			graphics.drawImage(miningIcon, canvasLocation.getX(), canvasLocation.getY(), null);
		}
	}
}
