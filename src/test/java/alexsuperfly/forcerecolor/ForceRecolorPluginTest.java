package alexsuperfly.forcerecolor;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ForceRecolorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ForceRecolorPlugin.class);
		RuneLite.main(args);
	}
}