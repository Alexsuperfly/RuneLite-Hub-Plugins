package alexsuperfly.forcerecolor;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Varbits;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Force Recolor"
)
public class ForceRecolorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ForceRecolorConfig config;

	@Inject
	private ConfigManager configManager;

	@Provides
	ForceRecolorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ForceRecolorConfig.class);
	}

	private static final Pattern TAG_REGEXP_SANS_LT_GT_IMG = Pattern.compile("<(?!lt>|gt>|img=[0-9]+>)[^>]*>");
	private static final Pattern COLOR_TAGS = Pattern.compile("<col=[^>]*>|</col>");
	private static final Pattern COMBAT_TASK_PREFIX = Pattern.compile("^(?:<[^>]*>|\\s)*CA_ID:[0-9]+\\|");

	private int transparencyVarbit = -1;
	private final Map<Integer, String> textGroupStrings = new HashMap<>();
	private final Map<Integer, Pattern> matchGroupPatterns = new TreeMap<>();
	private final Map<Integer, String> colorGroupStrings = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invokeLater(() ->
		{
			transparencyVarbit = client.getVarbitValue(Varbits.TRANSPARENT_CHATBOX);
			updateMatchingText();
			updateRecolorColor();
			client.refreshChat();
		});
	}

	@Override
	protected void shutDown()
	{
		clientThread.invokeLater(client::refreshChat);
	}

	private void updateMatchingText()
	{
		textGroupStrings.clear();
		matchGroupPatterns.clear();

		if (!config.matchedTextString().trim().equals(""))
		{
			List<String> items = Text.fromCSV(config.matchedTextString());
			for (String val : items)
			{
				String[] segments = val.split("::");
				int group = 0;
				if (segments.length == 2)
				{
					group = StringUtils.isNumeric(segments[1]) ? Integer.parseInt(segments[1]) : 0;
					if (group > 9 || group < 0)
					{
						group = 0;
					}
				}
				String matchText = Pattern.quote(Text.escapeJagex(segments[0]));
				String currentText = textGroupStrings.get(group);
				String combinedMatchText = currentText != null ? currentText + "|" + matchText : matchText;
				textGroupStrings.put(group, combinedMatchText);
			}

			textGroupStrings.forEach((group, text)
					-> matchGroupPatterns.put(group, Pattern.compile("(?:\\b|(?<=\\s)|\\A)(?:" + text + ")(?:\\b|(?=\\s)|\\z)", Pattern.CASE_INSENSITIVE)));
		}
	}

	private void updateRecolorColor()
	{
		log.debug("updateRecolorColor");
		boolean transparent = client.isResized() && transparencyVarbit != 0;
		String defaultColorString = "";

		colorGroupStrings.clear();

		switch (config.recolorStyle())
		{
			case CHAT_COLOR_CONFIG:
			{
				Color chatColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentGameMessage" : "opaqueGameMessage", Color.class);
				if (chatColorConfigColor != null)
				{
					defaultColorString = "<col=" + ColorUtil.toHexColor(chatColorConfigColor).substring(1) + ">";
				}
				break;
			}
			case THIS_CONFIG:
			{
				Color thisConfigColor = transparent ? config.transparentRecolor() : config.opaqueRecolor();
				if (thisConfigColor != null)
				{
					defaultColorString = "<col=" + ColorUtil.toHexColor(thisConfigColor).substring(1) + ">";
				}
				break;
			}
		}
		colorGroupStrings.put(0, defaultColorString);

		for (int i = 1; i < 10; i++)
		{
			Color color = configManager.getConfiguration("forcerecolor", transparent ? "transparentRecolorGroup" + String.valueOf(i) : "opaqueRecolorGroup" + String.valueOf(i), Color.class);
			colorGroupStrings.put(i, color != null ? "<col=" + ColorUtil.toHexColor(color).substring(1) + ">" : "");
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("forcerecolor"))
		{
			updateMatchingText();
			updateRecolorColor();
			clientThread.invokeLater(client::refreshChat);
		}

		if (event.getGroup().equals("textrecolor"))
		{
			updateRecolorColor();
			clientThread.invokeLater(client::refreshChat);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int setting = client.getVarbitValue(Varbits.TRANSPARENT_CHATBOX);

		if (transparencyVarbit != setting)
		{
			transparencyVarbit = setting;
			updateRecolorColor();
			clientThread.invokeLater(client::refreshChat);
		}
	}

	// Run after ChatMessageManager so its colors can also be overridden.
	@Subscribe(priority = -3)
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatMessageBuilding".equals(event.getEventName()) || matchGroupPatterns.isEmpty())
		{
			return;
		}

		int uid = client.getIntStack()[client.getIntStackSize() - 1];
		MessageNode messageNode = client.getMessages().get(uid);
		if (messageNode == null)
		{
			return;
		}

		ChatMessageType chatType = messageNode.getType();
		if (!config.allMessageTypes() && chatType != ChatMessageType.GAMEMESSAGE && chatType != ChatMessageType.SPAM && chatType != ChatMessageType.ENGINE)
		{
			return;
		}

		Object[] objectStack = client.getObjectStack();
		int messageIndex = client.getObjectStackSize() - 2;
		String message = client.macroExpand((String) objectStack[messageIndex]);
		String matchingText = removeMostTags(message.replace("<br>", " "));

		for (int group : matchGroupPatterns.keySet())
		{
			if (matchGroupPatterns.get(group).matcher(matchingText).find())
			{
				// Change only the text being drawn. The stored message and RuneLite
				// format retain the data used by quest links, task menus and plugins.
				objectStack[messageIndex] = recolorMessage(message, colorGroupStrings.get(group));
				break;
			}
		}
	}

	static String recolorMessage(String message, String colorTag)
	{
		String text = COLOR_TAGS.matcher(message).replaceAll("");
		int prefixEnd = 0;
		Matcher combatTask = COMBAT_TASK_PREFIX.matcher(text);
		if (combatTask.find())
		{
			// Keep the control prefix before the color, including icons and the
			// delimiter. The game splits this into metadata and display text.
			prefixEnd = combatTask.end();
		}
		else if (text.startsWith("|"))
		{
			// Group Ironman chat uses a leading delimiter too.
			prefixEnd = 1;
		}
		String color = colorTag == null ? "" : colorTag;
		return text.substring(0, prefixEnd) + color
			+ text.substring(prefixEnd).replace("<br>", "<br>" + color);
	}

	public static String removeMostTags(String str)
	{
		return TAG_REGEXP_SANS_LT_GT_IMG.matcher(str).replaceAll("").replace('\u00A0', ' ');
	}
}
