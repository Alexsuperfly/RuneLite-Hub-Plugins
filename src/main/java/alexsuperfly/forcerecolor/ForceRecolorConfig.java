package alexsuperfly.forcerecolor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("forcerecolor")
public interface ForceRecolorConfig extends Config
{
	@ConfigItem(
		position = 1,
		keyName = "matchedTextString",
		name = "Matched text",
		description = "Comma separated list of text to find, force recoloring containing game messages."
	)
	default String matchedTextString()
	{
		return "";
	}

	@ConfigItem(
		position = 2,
		keyName = "recolorStyle",
		name = "Recolor Style",
		description = "What should be used to recolor the matched message."
	)
	default RecolorStyle recolorStyle()
	{
		return RecolorStyle.CHAT_COLOR_CONFIG;
	}

	@ConfigItem(
		position = 3,
		keyName = "opaqueRecolor",
		name = "Opaque Recolor",
		description = "The recolor color for the opaque chatbox."
	)
	Color opaqueRecolor();

	@ConfigItem(
		position = 4,
		keyName = "transparentRecolor",
		name = "Transparent Recolor",
		description = "The recolor color for the transparent chatbox."
	)
	Color transparentRecolor();
}
