package alexsuperfly.forcerecolor;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Varbits;
import net.runelite.api.events.OverheadTextChanged;
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

	private static final Pattern COL_TAG_REGEXP = Pattern.compile("</?col(?:=[^>]*)?>");

	private int transparencyVarbit = -1;
	private final Map<Integer, String> textGroupStrings = new HashMap<>();
	private final Map<Integer, Pattern> matchGroupPatterns = new HashMap<>();
	private final Map<Integer, String> colorGroupStrings = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		updateMatchingText();
		updateRecolorColor();
		clientThread.invokeLater(client::refreshChat);
	}

	@Override
	protected void shutDown() throws Exception
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
		}
	}

	// Run after ChatMessageManager and the Chat Filter plugin
	// in case they change something/remove the message
	@Subscribe(priority = -3)
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatMessageBuilding".equals(event.getEventName()))
		{
			return;
		}

		final int[] intStack = client.getIntStack();
		final Object[] objectStack = client.getObjectStack();
		final int isize = client.getIntStackSize();
		final int osize = client.getObjectStackSize();
		final int uid = intStack[isize - 1];

		final MessageNode messageNode = client.getMessages().get(uid);
		if (messageNode == null)
		{
			return;
		}

		final ChatMessageType chatType = messageNode.getType();
		if (!config.allMessageTypes() && chatType != ChatMessageType.GAMEMESSAGE && chatType != ChatMessageType.SPAM && chatType != ChatMessageType.ENGINE)
		{
			return;
		}

		final String message = removeColorTags(client.macroExpand((String) objectStack[osize - 2]));
		final String matchText = Text.removeFormattingTags(message).replace('\u00A0', ' ');

		for (int group : matchGroupPatterns.keySet())
		{
			Matcher matcher = matchGroupPatterns.get(group).matcher(matchText);

			if (matcher.find())
			{
				// Used [proc,rebuildchatbox] and [proc,script632] as reference
				int idx = message.indexOf("CA_ID:");
				if (idx != -1)
				{
					idx = message.indexOf('|', idx) + 1;
				}
				else if (message.startsWith("|"))
				{
					idx = 1;
				}
				else if (message.startsWith("p|"))
				{
					idx = 2;
				}
				else
				{
					idx = 0;
				}

				objectStack[osize - 2] = message.substring(0, idx) + colorGroupStrings.get(group) + message.substring(idx);
				break;
			}
		}
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!config.overheadText().matches(event.getActor()))
		{
			return;
		}

		final String text = event.getOverheadText();

		for (int group : matchGroupPatterns.keySet())
		{
			if (matchGroupPatterns.get(group).matcher(text).find())
			{
				event.getActor().setOverheadText(colorGroupStrings.get(group) + text);
				break;
			}
		}
	}

	public static String removeColorTags(String str)
	{
		return COL_TAG_REGEXP.matcher(str).replaceAll("");
	}
}
