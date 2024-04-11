package com.cwjoshuak;

import java.awt.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;

public class CamTorumMiningOverlay extends Overlay {

	private final CamTorumMiningConfig config;
	private final CamTorumMiningPlugin plugin;
	private final Client client;

	@Inject
	private CamTorumMiningOverlay(Client client, CamTorumMiningConfig config, CamTorumMiningPlugin plugin)
	{
		this.config = config;
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getStreams().isEmpty())
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		plugin.getStreams().forEach((object, tile) ->
		{
			if (tile.getWorldLocation().distanceTo(player.getWorldLocation()) < config.maxDistance()) {

				Shape objectClickbox = object.getClickbox();
				if (objectClickbox != null) {
					graphics.setColor(config.getWaterOutlineColor());
					graphics.draw(objectClickbox);
					Color waterFillColor = config.getWaterFillColor();
					graphics.setColor(ColorUtil.colorWithAlpha(waterFillColor, waterFillColor.getAlpha() / 12));
					graphics.fill(objectClickbox);
				}
			}
		});
		return null;
	}
}
