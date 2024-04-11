package com.cwjoshuak;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.util.ColorUtil;

import java.awt.*;

@ConfigGroup("camtorummining")
public interface CamTorumMiningConfig extends Config
{
	@ConfigItem(
		keyName = "maxDistance",
		name = "Max Distance",
		description = "The maximum distance in which you wish to see highlighted water streams.",
		position = 1
	)
	default int maxDistance()
	{
		return 10;
	}

	@Alpha
	@ConfigItem(
		keyName = "waterHighlightColor",
		name = "Water Fill Color",
		description = "Color of inner water fill",
		position = 2
	)
	default Color getWaterFillColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		position = 3,
		keyName = "waterOutlineColor",
		name = "Water Clickbox Color",
		description = "Color of outer water clickbox"
	)
	default Color getWaterOutlineColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 4,
		keyName = "dynamicMenuEntrySwap",
		name = "Dynamically swap depleted rock menu entries",
		description = "Swap menu entries to only make calcified rocks clickable."
	)
	default boolean dynamicMenuEntrySwap() {
		return true;
	}
}
