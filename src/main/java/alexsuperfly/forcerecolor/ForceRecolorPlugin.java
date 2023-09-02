package alexsuperfly.forcerecolor;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
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
	private ChatMessageManager chatMessageManager;

	@Inject
	private ConfigManager configManager;

	@Provides
	ForceRecolorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ForceRecolorConfig.class);
	}

	private static final Pattern TAG_REGEXP_SANS_LT_GT = Pattern.compile("<(?!lt>|gt>)[^>]*>");

	private int transparencyVarbit = -1;
	private final Map<Integer, String> textGroupStrings = new HashMap<>();
	private final Map<Integer, Pattern> matchGroupPatterns = new HashMap<>();
	private final Map<Integer, String> colorGroupStrings = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		updateMatchingText();
		updateRecolorColor();
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
		}

		if (event.getGroup().equals("textrecolor"))
		{
			updateRecolorColor();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int setting = client.getVar(Varbits.TRANSPARENT_CHATBOX);

		if (transparencyVarbit != setting)
		{
			transparencyVarbit = setting;
			updateRecolorColor();
		}
	}

	// Run after ChatMessageManager and the Chat Filter plugin
	// in case they change something/remove the message
	@Subscribe(priority = -3)
	public void onChatMessage(ChatMessage chatMessage)
	{
		ChatMessageType chatType = chatMessage.getType();
		if (!config.allMessageTypes() && chatType != ChatMessageType.GAMEMESSAGE && chatType != ChatMessageType.SPAM && chatType != ChatMessageType.ENGINE)
		{
			return;
		}

		MessageNode messageNode = chatMessage.getMessageNode();
		String nodeValue = removeMostTags(messageNode.getValue());

		for (int group : matchGroupPatterns.keySet())
		{
			Matcher matcher = matchGroupPatterns.get(group).matcher(nodeValue);

			if (matcher.find())
			{
				// When the Notifier makes a new message onChatMessage gets fired
				// before it refreshes its content and so changes would be overridden
				// unless I set them after it finally does
				clientThread.invokeLater(() ->
				{
					messageNode.setValue(colorGroupStrings.get(group) + nodeValue);
					messageNode.setRuneLiteFormatMessage(messageNode.getValue());
					client.refreshChat();
				});
				break;
			}
		}
	}

	public static String removeMostTags(String str)
	{
		return TAG_REGEXP_SANS_LT_GT.matcher(str).replaceAll("").replace('\u00A0', ' ');
	}
}
