package alexsuperfly.forcerecolor;

import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;

public enum OverheadTextTarget
{
	NONE,
	PLAYERS,
	NPCS,
	BOTH;

	public boolean matches(Actor actor)
	{
		switch (this)
		{
			case PLAYERS:
				return actor instanceof Player;
			case NPCS:
				return actor instanceof NPC;
			case BOTH:
				return true;
			default:
				return false;
		}
	}
}