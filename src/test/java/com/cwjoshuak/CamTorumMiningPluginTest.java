package com.cwjoshuak;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CamTorumMiningPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CamTorumMiningPlugin.class);
		RuneLite.main(args);
	}
}
