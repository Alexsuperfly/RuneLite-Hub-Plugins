package alexsuperfly.forcerecolor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup("forcerecolor")
public interface ForceRecolorConfig extends Config
{
	@ConfigSection(
			name = "Opaque group colors",
			description = "Color groups for the opaque chatbox",
			position = 20,
			closedByDefault = true
	)
	String opaqueGroups = "opaqueGroups";

	@ConfigSection(
			name = "Transparent group colors",
			description = "Color groups for the transparent chatbox",
			position = 21,
			closedByDefault = true
	)
	String transparentGroups = "transparentGroups";

	@ConfigItem(
		position = 1,
		keyName = "matchedTextString",
		name = "Matched text",
		description = "Comma separated list of text to find, force recoloring containing game messages.  Append a ::# to the string to denote what color group it should be colored as, blank or invalid numbers will be evaluated to the default color."
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
		description = "The default recolor color for the opaque chatbox."
	)
	Color opaqueRecolor();

	@ConfigItem(
		position = 4,
		keyName = "transparentRecolor",
		name = "Transparent Recolor",
		description = "The default recolor color for the transparent chatbox."
	)
	Color transparentRecolor();

	@ConfigItem(
			keyName = "opaqueRecolorGroup1",
			name = "Opaque Group 1",
			description = "The recolor color of group 1 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup1();

	@ConfigItem(
			keyName = "opaqueRecolorGroup2",
			name = "Opaque Group 2",
			description = "The recolor color of group 2 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup2();

	@ConfigItem(
			keyName = "opaqueRecolorGroup3",
			name = "Opaque Group 3",
			description = "The recolor color of group 3 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup3();

	@ConfigItem(
			keyName = "opaqueRecolorGroup4",
			name = "Opaque Group 4",
			description = "The recolor color of group 4 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup4();

	@ConfigItem(
			keyName = "opaqueRecolorGroup5",
			name = "Opaque Group 5",
			description = "The recolor color of group 5 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup5();

	@ConfigItem(
			keyName = "opaqueRecolorGroup6",
			name = "Opaque Group 6",
			description = "The recolor color of group 6 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup6();

	@ConfigItem(
			keyName = "opaqueRecolorGroup7",
			name = "Opaque Group 7",
			description = "The recolor color of group 7 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup7();

	@ConfigItem(
			keyName = "opaqueRecolorGroup8",
			name = "Opaque Group 8",
			description = "The recolor color of group 8 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup8();

	@ConfigItem(
			keyName = "opaqueRecolorGroup9",
			name = "Opaque Group 9",
			description = "The recolor color of group 9 for the opaque chatbox.",
			section = opaqueGroups
	)
	Color opaqueRecolorGroup9();

	@ConfigItem(
			keyName = "transparentRecolorGroup1",
			name = "Transparent Group 1",
			description = "The recolor color of group 1 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup1();

	@ConfigItem(
			keyName = "transparentRecolorGroup2",
			name = "Transparent Group 2",
			description = "The recolor color of group 2 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup2();

	@ConfigItem(
			keyName = "transparentRecolorGroup3",
			name = "Transparent Group 3",
			description = "The recolor color of group 3 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup3();

	@ConfigItem(
			keyName = "transparentRecolorGroup4",
			name = "Transparent Group 4",
			description = "The recolor color of group 4 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup4();

	@ConfigItem(
			keyName = "transparentRecolorGroup5",
			name = "Transparent Group 5",
			description = "The recolor color of group 5 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup5();

	@ConfigItem(
			keyName = "transparentRecolorGroup6",
			name = "Transparent Group 6",
			description = "The recolor color of group 6 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup6();

	@ConfigItem(
			keyName = "transparentRecolorGroup7",
			name = "Transparent Group 7",
			description = "The recolor color of group 7 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup7();

	@ConfigItem(
			keyName = "transparentRecolorGroup8",
			name = "Transparent Group 8",
			description = "The recolor color of group 8 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup8();

	@ConfigItem(
			keyName = "transparentRecolorGroup9",
			name = "Transparent Group 9",
			description = "The recolor color of group 9 for the transparent chatbox.",
			section = transparentGroups
	)
	Color transparentRecolorGroup9();
}
